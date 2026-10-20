using System.Net;
using System.Text;
using System.Text.Json;
using LicenseHub.Client;

namespace LicenseHub.Client.Tests;

public class LicenseHubClientTests
{
    private static readonly Guid ProductId =
        Guid.Parse("11111111-1111-1111-1111-111111111111");

    [Fact]
    public async Task ActivateAsyncSendsLicenseKeyAndReadsIssuedToken()
    {
        var activationId = Guid.NewGuid();
        var licenseId = Guid.NewGuid();
        var handler = new RecordingHttpMessageHandler(
            () => JsonResponse(
                HttpStatusCode.OK,
                $$"""
                {
                  "id": "{{activationId}}",
                  "licenseId": "{{licenseId}}",
                  "machineName": "Development laptop",
                  "activatedAt": "2026-10-20T06:00:00Z",
                  "lastSeenAt": "2026-10-20T06:05:00Z",
                  "activationToken": "LA_issued-token"
                }
                """));
        var client = CreateClient(handler);

        var result = await client.ActivateAsync(
            "LH_license-key",
            "machine-001",
            "Development laptop");

        Assert.Equal(HttpMethod.Post, handler.Method);
        Assert.Equal("/api/activations", handler.RequestUri?.AbsolutePath);

        using var request = JsonDocument.Parse(handler.Body!);
        Assert.Equal(ProductId, request.RootElement.GetProperty("productId").GetGuid());
        Assert.Equal("LH_license-key", request.RootElement.GetProperty("licenseKey").GetString());
        Assert.Equal("machine-001", request.RootElement.GetProperty("machineFingerprint").GetString());
        Assert.Equal("Development laptop", request.RootElement.GetProperty("machineName").GetString());

        Assert.Equal(activationId, result.Id);
        Assert.Equal(licenseId, result.LicenseId);
        Assert.Equal("LA_issued-token", result.ActivationToken);
    }

    [Fact]
    public async Task ValidateAsyncSendsActivationTokenInsteadOfLicenseKey()
    {
        var handler = new RecordingHttpMessageHandler(
            () => JsonResponse(
                HttpStatusCode.OK,
                $$"""
                {
                  "id": "{{Guid.NewGuid()}}",
                  "licenseId": "{{Guid.NewGuid()}}",
                  "machineName": "Development laptop",
                  "activatedAt": "2026-10-20T06:00:00Z",
                  "lastSeenAt": "2026-10-20T06:05:00Z"
                }
                """));
        var client = CreateClient(handler);

        await client.ValidateAsync("LA_saved-token", "machine-001");

        Assert.Equal("/api/activations/validate", handler.RequestUri?.AbsolutePath);

        using var request = JsonDocument.Parse(handler.Body!);
        Assert.Equal("LA_saved-token", request.RootElement.GetProperty("activationToken").GetString());
        Assert.False(request.RootElement.TryGetProperty("licenseKey", out _));
    }

    [Fact]
    public async Task DeactivateAsyncAcceptsNoContentResponse()
    {
        var handler = new RecordingHttpMessageHandler(
            () => new HttpResponseMessage(HttpStatusCode.NoContent));
        var client = CreateClient(handler);

        await client.DeactivateAsync("LA_saved-token", "machine-001");

        Assert.Equal("/api/activations/deactivate", handler.RequestUri?.AbsolutePath);

        using var request = JsonDocument.Parse(handler.Body!);
        Assert.Equal(ProductId, request.RootElement.GetProperty("productId").GetGuid());
        Assert.Equal("LA_saved-token", request.RootElement.GetProperty("activationToken").GetString());
        Assert.Equal("machine-001", request.RootElement.GetProperty("machineFingerprint").GetString());
    }

    [Fact]
    public async Task ApiProblemBecomesLicenseHubApiException()
    {
        var handler = new RecordingHttpMessageHandler(
            () => JsonResponse(
                HttpStatusCode.Unauthorized,
                """
                {
                  "title": "Invalid activation token",
                  "status": 401,
                  "detail": "Invalid activation token",
                  "errors": {
                    "activationToken": "must not be blank"
                  }
                }
                """,
                "application/problem+json"));
        var client = CreateClient(handler);

        var exception = await Assert.ThrowsAsync<LicenseHubApiException>(
            () => client.ValidateAsync("LA_invalid-token", "machine-001"));

        Assert.Equal(HttpStatusCode.Unauthorized, exception.StatusCode);
        Assert.Equal("Invalid activation token", exception.Title);
        Assert.Equal("Invalid activation token", exception.Detail);
        Assert.Equal("must not be blank", exception.Errors!["activationToken"]);
    }

    private static LicenseHubClient CreateClient(RecordingHttpMessageHandler handler)
    {
        var httpClient = new HttpClient(handler)
        {
            BaseAddress = new Uri("https://licensehub.test")
        };

        return new LicenseHubClient(httpClient, ProductId);
    }

    private static HttpResponseMessage JsonResponse(
        HttpStatusCode statusCode,
        string json,
        string mediaType = "application/json")
    {
        return new HttpResponseMessage(statusCode)
        {
            Content = new StringContent(json, Encoding.UTF8, mediaType)
        };
    }

    private sealed class RecordingHttpMessageHandler(
        Func<HttpResponseMessage> responseFactory) : HttpMessageHandler
    {
        public HttpMethod? Method { get; private set; }

        public Uri? RequestUri { get; private set; }

        public string? Body { get; private set; }

        protected override async Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request,
            CancellationToken cancellationToken)
        {
            Method = request.Method;
            RequestUri = request.RequestUri;
            Body = request.Content is null
                ? null
                : await request.Content.ReadAsStringAsync(cancellationToken);

            return responseFactory();
        }
    }
}
