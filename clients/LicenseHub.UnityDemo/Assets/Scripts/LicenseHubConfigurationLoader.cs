using System;
using System.IO;
using UnityEngine;

namespace LicenseHub.UnityDemo
{
    internal static class LicenseHubConfigurationLoader
    {
        internal const string FileName = "licensehub-config.json";

        internal static bool TryResolve(
            string inspectorApiBaseUrl,
            string inspectorProductId,
            out string apiBaseUrl,
            out string productId,
            out string source,
            out string error)
        {
            string configurationPath = Path.GetFullPath(
                Path.Combine(Application.dataPath, "..", FileName));

            if (!File.Exists(configurationPath))
            {
                apiBaseUrl = inspectorApiBaseUrl;
                productId = inspectorProductId;
                source = "Unity Inspector";
                error = string.Empty;
                return true;
            }

            try
            {
                string json = File.ReadAllText(configurationPath);
                LicenseHubConfigurationFile configuration =
                    JsonUtility.FromJson<LicenseHubConfigurationFile>(json);

                if (configuration == null ||
                    string.IsNullOrWhiteSpace(configuration.apiBaseUrl) ||
                    string.IsNullOrWhiteSpace(configuration.productId))
                {
                    apiBaseUrl = string.Empty;
                    productId = string.Empty;
                    source = configurationPath;
                    error =
                        $"{FileName} must contain apiBaseUrl and productId.";
                    return false;
                }

                apiBaseUrl = configuration.apiBaseUrl.Trim();
                productId = configuration.productId.Trim();
                source = configurationPath;
                error = string.Empty;
                return true;
            }
            catch (Exception exception)
            {
                apiBaseUrl = string.Empty;
                productId = string.Empty;
                source = configurationPath;
                error = $"Could not read {FileName}: {exception.Message}";
                return false;
            }
        }

        [Serializable]
        private sealed class LicenseHubConfigurationFile
        {
            public string apiBaseUrl = string.Empty;
            public string productId = string.Empty;
        }
    }
}
