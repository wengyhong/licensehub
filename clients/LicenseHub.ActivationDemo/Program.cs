using System.Net.Http.Json;
using System.Text.Json;

var jsonOptions =
    new JsonSerializerOptions(JsonSerializerDefaults.Web);
var apiBaseUrl =
    Environment.GetEnvironmentVariable("LICENSEHUB_API_URL")
    ?? "http://localhost:8080";

var productIdText =
    Environment.GetEnvironmentVariable("LICENSEHUB_PRODUCT_ID");

if (!Guid.TryParse(productIdText, out var productId))
{
    Console.Error.WriteLine(
        "LICENSEHUB_PRODUCT_ID must contain a valid product ID.");
    return;
}

using var httpClient = new HttpClient
{
    BaseAddress = new Uri(apiBaseUrl)
};

Console.Write("License key: ");
var licenseKey = Console.ReadLine()?.Trim();

Console.Write("Machine fingerprint: ");
var machineFingerprint = Console.ReadLine()?.Trim();

Console.Write("Machine name: ");
var machineName = Console.ReadLine()?.Trim();

if (string.IsNullOrWhiteSpace(licenseKey) ||
    string.IsNullOrWhiteSpace(machineFingerprint))
{
    Console.Error.WriteLine(
        "License key and machine fingerprint are required.");
    return;
}

var request = new ActivationRequest(
    productId,
    licenseKey,
    machineFingerprint,
    machineName);

using var response = await httpClient.PostAsJsonAsync(
    "/api/activations",
    request);

var responseBody = await response.Content.ReadAsStringAsync();

static ApiProblem? TryDeserializeProblem(
    string responseBody,
    JsonSerializerOptions jsonOptions)
{
    try
    {
        return JsonSerializer.Deserialize<ApiProblem>(
            responseBody,
            jsonOptions);
    }
    catch (JsonException)
    {
        return null;
    }
}

if (!response.IsSuccessStatusCode)
{
    var problem = TryDeserializeProblem(responseBody, jsonOptions);

    Console.Error.WriteLine(
        $"Activation failed: {(int)response.StatusCode} {response.StatusCode}");

    if (problem is not null)
    {
        if (!string.IsNullOrWhiteSpace(problem.Title))
        {
            Console.Error.WriteLine(problem.Title);
        }

        if (!string.IsNullOrWhiteSpace(problem.Detail) &&
            !string.Equals(
                problem.Detail,
                problem.Title,
                StringComparison.OrdinalIgnoreCase))
        {
            Console.Error.WriteLine(problem.Detail);
        }

        if (problem.Errors is not null)
        {
            foreach (var error in problem.Errors)
            {
                Console.Error.WriteLine(
                    $"{error.Key}: {error.Value}");
            }
        }
    }
    else
    {
        Console.Error.WriteLine(responseBody);
    }

    return;
}

var activation = JsonSerializer.Deserialize<ActivationResponse>(
    responseBody,
    jsonOptions);

if (activation is null)
{
    Console.Error.WriteLine("The server returned an invalid response.");
    return;
}

Console.WriteLine();
Console.WriteLine("Activation successful");
Console.WriteLine($"Activation ID: {activation.Id}");
Console.WriteLine($"License ID:    {activation.LicenseId}");
Console.WriteLine($"Machine:       {activation.MachineName}");
Console.WriteLine($"Activated at:  {activation.ActivatedAt}");
Console.WriteLine($"Last seen at:  {activation.LastSeenAt}");

internal sealed record ActivationRequest(
    Guid ProductId,
    string LicenseKey,
    string MachineFingerprint,
    string? MachineName);

internal sealed record ActivationResponse(
    Guid Id,
    Guid LicenseId,
    string? MachineName,
    DateTimeOffset ActivatedAt,
    DateTimeOffset LastSeenAt);

internal sealed record ApiProblem(
    string? Type,
    string? Title,
    int? Status,
    string? Detail,
    string? Instance,
    Dictionary<string, string>? Errors);
