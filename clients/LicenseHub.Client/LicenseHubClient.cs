using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using LicenseHub.Client.Internal;
using LicenseHub.Client.Models;

namespace LicenseHub.Client
{
    public sealed class LicenseHubClient
    {
        private static readonly JsonSerializerOptions JsonOptions =
            new JsonSerializerOptions(JsonSerializerDefaults.Web);

        private readonly HttpClient httpClient;
        private readonly Guid productId;

        public LicenseHubClient(HttpClient httpClient, Guid productId)
        {
            this.httpClient = httpClient ?? throw new ArgumentNullException(nameof(httpClient));

            if (httpClient.BaseAddress == null)
            {
                throw new ArgumentException(
                    "The HTTP client must have a base address.",
                    nameof(httpClient));
            }

            if (productId == Guid.Empty)
            {
                throw new ArgumentException(
                    "The product ID must not be empty.",
                    nameof(productId));
            }

            this.productId = productId;
        }

        public Task<ActivationResult> ActivateAsync(
            string licenseKey,
            string machineFingerprint,
            string? machineName,
            CancellationToken cancellationToken = default)
        {
            RequireNotBlank(licenseKey, nameof(licenseKey));
            RequireNotBlank(machineFingerprint, nameof(machineFingerprint));

            var request = new ActivationRequest(
                productId,
                licenseKey,
                machineFingerprint,
                machineName);

            return PostForActivationAsync(
                "/api/activations",
                request,
                cancellationToken);
        }

        public Task<ActivationResult> ValidateAsync(
            string activationToken,
            string machineFingerprint,
            CancellationToken cancellationToken = default)
        {
            RequireNotBlank(activationToken, nameof(activationToken));
            RequireNotBlank(machineFingerprint, nameof(machineFingerprint));

            var request = new ActivationTokenRequest(
                productId,
                activationToken,
                machineFingerprint);

            return PostForActivationAsync(
                "/api/activations/validate",
                request,
                cancellationToken);
        }

        public async Task DeactivateAsync(
            string activationToken,
            string machineFingerprint,
            CancellationToken cancellationToken = default)
        {
            RequireNotBlank(activationToken, nameof(activationToken));
            RequireNotBlank(machineFingerprint, nameof(machineFingerprint));

            var request = new ActivationTokenRequest(
                productId,
                activationToken,
                machineFingerprint);

            using (var response =
                   await PostAsync(
                           "/api/activations/deactivate",
                           request,
                           cancellationToken)
                       .ConfigureAwait(false))
            {
                if (!response.IsSuccessStatusCode)
                {
                    throw await CreateApiExceptionAsync(response).ConfigureAwait(false);
                }
            }
        }

        private async Task<ActivationResult> PostForActivationAsync<TRequest>(
            string path,
            TRequest request,
            CancellationToken cancellationToken)
        {
            using (var response =
                   await PostAsync(path, request, cancellationToken).ConfigureAwait(false))
            {
                string responseBody =
                    await response.Content.ReadAsStringAsync().ConfigureAwait(false);

                if (!response.IsSuccessStatusCode)
                {
                    throw CreateApiException(response, responseBody);
                }

                try
                {
                    var result =
                        JsonSerializer.Deserialize<ActivationResult>(responseBody, JsonOptions);

                    if (result == null)
                    {
                        throw new InvalidOperationException(
                            "LicenseHub returned an empty activation response.");
                    }

                    return result;
                }
                catch (JsonException exception)
                {
                    throw new InvalidOperationException(
                        "LicenseHub returned an invalid activation response.",
                        exception);
                }
            }
        }

        private async Task<HttpResponseMessage> PostAsync<TRequest>(
            string path,
            TRequest request,
            CancellationToken cancellationToken)
        {
            string json = JsonSerializer.Serialize(request, JsonOptions);

            using (var content = new StringContent(json, Encoding.UTF8, "application/json"))
            {
                return await httpClient
                    .PostAsync(path, content, cancellationToken)
                    .ConfigureAwait(false);
            }
        }

        private static async Task<LicenseHubApiException> CreateApiExceptionAsync(
            HttpResponseMessage response)
        {
            string responseBody =
                await response.Content.ReadAsStringAsync().ConfigureAwait(false);

            return CreateApiException(response, responseBody);
        }

        private static LicenseHubApiException CreateApiException(
            HttpResponseMessage response,
            string responseBody)
        {
            ApiProblem? problem = null;

            try
            {
                problem = JsonSerializer.Deserialize<ApiProblem>(responseBody, JsonOptions);
            }
            catch (JsonException)
            {
                // The response was not an RFC 9457 problem document.
            }

            return new LicenseHubApiException(
                response.StatusCode,
                problem?.Title,
                problem?.Detail,
                problem?.Errors);
        }

        private static void RequireNotBlank(string value, string parameterName)
        {
            if (string.IsNullOrWhiteSpace(value))
            {
                throw new ArgumentException(
                    "The value must not be blank.",
                    parameterName);
            }
        }
    }
}
