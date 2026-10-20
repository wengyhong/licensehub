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
    BaseAddress = new Uri(apiBaseUrl),
    Timeout = TimeSpan.FromSeconds(10)
};

Console.WriteLine("LicenseHub Activation Demo");
Console.WriteLine("1. Activate this machine");
Console.WriteLine("2. Validate this machine");
Console.WriteLine("3. Deactivate this machine");
Console.Write("Choose an action: ");

var action = Console.ReadLine()?.Trim();

if (action is not ("1" or "2" or "3"))
{
    Console.Error.WriteLine("Choose 1, 2, or 3.");
    return;
}

var isActivation = action == "1";
var isValidation = action == "2";
var operationName = action switch
{
    "1" => "Activation",
    "2" => "Validation",
    _ => "Deactivation"
};

Console.Write("License key: ");
var licenseKey = Console.ReadLine()?.Trim();

string machineFingerprint;

try
{
    machineFingerprint = GetOrCreateMachineFingerprint();
}
catch (IOException)
{
    Console.Error.WriteLine(
        "The local machine identifier could not be read or created.");
    return;
}
catch (UnauthorizedAccessException)
{
    Console.Error.WriteLine(
        "The local machine identifier could not be accessed.");
    return;
}

Console.WriteLine($"Machine ID: {machineFingerprint}");

if (string.IsNullOrWhiteSpace(licenseKey))
{
    Console.Error.WriteLine("License key is required.");
    return;
}

HttpResponseMessage response;

try
{
    if (isActivation)
    {
        Console.Write("Machine name: ");
        var machineName = Console.ReadLine()?.Trim();

        var request = new ActivationRequest(
            productId,
            licenseKey,
            machineFingerprint,
            machineName);

        response = await httpClient.PostAsJsonAsync(
            "/api/activations",
            request);
    }
    else if (isValidation)
    {
        var request = new ValidateActivationRequest(
            productId,
            licenseKey,
            machineFingerprint);

        response = await httpClient.PostAsJsonAsync(
            "/api/activations/validate",
            request);
    }
    else
    {
        var request = new DeactivateMachineRequest(
            productId,
            licenseKey,
            machineFingerprint);

        response = await httpClient.PostAsJsonAsync(
            "/api/activations/deactivate",
            request);
    }
}
catch (HttpRequestException)
{
    Console.Error.WriteLine(
        $"Could not connect to LicenseHub at {apiBaseUrl}.");
    Console.Error.WriteLine(
        "Check that the backend is running and try again.");
    return;
}
catch (TaskCanceledException)
{
    Console.Error.WriteLine(
        "The LicenseHub server did not respond within 10 seconds.");
    return;
}

using var responseLifetime = response;

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
        $"{operationName} failed: {(int)response.StatusCode} {response.StatusCode}");

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

if (!isActivation && !isValidation)
{
    Console.WriteLine("Deactivation successful");
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
Console.WriteLine($"{operationName} successful");
Console.WriteLine($"Activation ID: {activation.Id}");
Console.WriteLine($"License ID:    {activation.LicenseId}");
Console.WriteLine($"Machine:       {activation.MachineName}");
Console.WriteLine($"Activated at:  {activation.ActivatedAt}");
Console.WriteLine($"Last seen at:  {activation.LastSeenAt}");

static string GetOrCreateMachineFingerprint()
{
    var applicationDataDirectory = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "LicenseHub",
        "ActivationDemo");

    var machineIdPath = Path.Combine(
        applicationDataDirectory,
        "machine-id");

    Directory.CreateDirectory(applicationDataDirectory);

    if (File.Exists(machineIdPath))
    {
        var storedMachineId = File.ReadAllText(machineIdPath).Trim();

        if (Guid.TryParse(storedMachineId, out var parsedMachineId))
        {
            return parsedMachineId.ToString("D");
        }
    }

    var machineId = Guid.NewGuid().ToString("D");
    File.WriteAllText(machineIdPath, machineId);

    return machineId;
}

internal sealed record ActivationRequest(
    Guid ProductId,
    string LicenseKey,
    string MachineFingerprint,
    string? MachineName);

internal sealed record ValidateActivationRequest(
    Guid ProductId,
    string LicenseKey,
    string MachineFingerprint);

internal sealed record DeactivateMachineRequest(
    Guid ProductId,
    string LicenseKey,
    string MachineFingerprint);

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
