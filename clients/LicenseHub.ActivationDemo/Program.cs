using LicenseHub.Client;
using LicenseHub.Client.Models;

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

var licenseHubClient = new LicenseHubClient(httpClient, productId);

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

string? licenseKey = null;
string? activationToken = null;

if (isActivation)
{
    Console.Write("License key: ");
    licenseKey = Console.ReadLine()?.Trim();

    if (string.IsNullOrWhiteSpace(licenseKey))
    {
        Console.Error.WriteLine("License key is required.");
        return;
    }
}
else
{
    try
    {
        activationToken = ReadActivationToken(productId);
    }
    catch (IOException)
    {
        Console.Error.WriteLine("The saved activation token could not be read.");
        return;
    }
    catch (UnauthorizedAccessException)
    {
        Console.Error.WriteLine("The saved activation token could not be accessed.");
        return;
    }

    if (string.IsNullOrWhiteSpace(activationToken))
    {
        Console.Error.WriteLine(
            "No activation token is saved for this product. Activate this machine first.");
        return;
    }
}

ActivationResult? activation = null;

try
{
    if (isActivation)
    {
        Console.Write("Machine name: ");
        var machineName = Console.ReadLine()?.Trim();

        activation = await licenseHubClient.ActivateAsync(
            licenseKey!,
            machineFingerprint,
            machineName);
    }
    else if (isValidation)
    {
        activation = await licenseHubClient.ValidateAsync(
            activationToken!,
            machineFingerprint);
    }
    else
    {
        await licenseHubClient.DeactivateAsync(
            activationToken!,
            machineFingerprint);
    }
}
catch (LicenseHubApiException exception)
{
    Console.Error.WriteLine(
        $"{operationName} failed: {(int)exception.StatusCode} {exception.StatusCode}");

    if (!string.IsNullOrWhiteSpace(exception.Title))
    {
        Console.Error.WriteLine(exception.Title);
    }

    if (!string.IsNullOrWhiteSpace(exception.Detail) &&
        !string.Equals(
            exception.Detail,
            exception.Title,
            StringComparison.OrdinalIgnoreCase))
    {
        Console.Error.WriteLine(exception.Detail);
    }

    if (exception.Errors is not null)
    {
        foreach (var error in exception.Errors)
        {
            Console.Error.WriteLine($"{error.Key}: {error.Value}");
        }
    }

    return;
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
catch (InvalidOperationException exception)
{
    Console.Error.WriteLine($"{operationName} failed: {exception.Message}");
    return;
}

if (!isActivation && !isValidation)
{
    try
    {
        DeleteActivationToken(productId);
    }
    catch (IOException)
    {
        Console.Error.WriteLine(
            "Deactivation succeeded, but the local activation token could not be deleted.");
        return;
    }
    catch (UnauthorizedAccessException)
    {
        Console.Error.WriteLine(
            "Deactivation succeeded, but the local activation token could not be deleted.");
        return;
    }

    Console.WriteLine("Deactivation successful");
    return;
}

if (activation is null)
{
    Console.Error.WriteLine("The server returned an invalid response.");
    return;
}

if (isActivation)
{
    if (string.IsNullOrWhiteSpace(activation.ActivationToken))
    {
        Console.Error.WriteLine("The server did not return an activation token.");
        return;
    }

    try
    {
        SaveActivationToken(productId, activation.ActivationToken);
    }
    catch (IOException)
    {
        Console.Error.WriteLine(
            "Activation succeeded, but the activation token could not be saved locally.");
        return;
    }
    catch (UnauthorizedAccessException)
    {
        Console.Error.WriteLine(
            "Activation succeeded, but the activation token could not be saved locally.");
        return;
    }
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
    var applicationDataDirectory = GetApplicationDataDirectory();

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

static string GetApplicationDataDirectory()
{
    return Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "LicenseHub",
        "ActivationDemo");
}

static string GetActivationTokenPath(Guid productId)
{
    return Path.Combine(
        GetApplicationDataDirectory(),
        $"activation-token-{productId:D}.txt");
}

static string? ReadActivationToken(Guid productId)
{
    var tokenPath = GetActivationTokenPath(productId);
    return File.Exists(tokenPath)
        ? File.ReadAllText(tokenPath).Trim()
        : null;
}

static void SaveActivationToken(Guid productId, string activationToken)
{
    Directory.CreateDirectory(GetApplicationDataDirectory());
    File.WriteAllText(GetActivationTokenPath(productId), activationToken);
}

static void DeleteActivationToken(Guid productId)
{
    var tokenPath = GetActivationTokenPath(productId);

    if (File.Exists(tokenPath))
    {
        File.Delete(tokenPath);
    }
}
