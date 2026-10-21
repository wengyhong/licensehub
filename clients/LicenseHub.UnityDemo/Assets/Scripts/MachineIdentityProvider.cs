using System;
using UnityEngine;

namespace LicenseHub.UnityDemo
{
    internal static class MachineIdentityProvider
    {
        private const string FallbackMachineIdKey =
            "licensehub.fallback-machine-id";

#if UNITY_EDITOR
        private const string EditorMachineIdOverrideKey =
            "licensehub.editor-machine-id-override";
#endif

        public static string Get(Guid productId)
        {
#if UNITY_EDITOR
            string editorOverride = PlayerPrefs.GetString(
                EditorMachineIdOverrideKey,
                string.Empty);

            if (Guid.TryParse(editorOverride, out var editorMachineId))
            {
                return $"{productId:D}:{editorMachineId:D}";
            }
#endif

            string deviceId = SystemInfo.deviceUniqueIdentifier;

            if (string.IsNullOrWhiteSpace(deviceId) ||
                string.Equals(
                    deviceId,
                    SystemInfo.unsupportedIdentifier,
                    StringComparison.Ordinal))
            {
                deviceId = GetOrCreateFallbackMachineId();
            }

            // Namespacing prevents different products from sending the
            // same raw identifier to LicenseHub.
            return $"{productId:D}:{deviceId}";
        }

        private static string GetOrCreateFallbackMachineId()
        {
            string storedId = PlayerPrefs.GetString(
                FallbackMachineIdKey,
                string.Empty);

            if (Guid.TryParse(storedId, out var parsedId))
            {
                return parsedId.ToString("D");
            }

            string generatedId = Guid.NewGuid().ToString("D");

            PlayerPrefs.SetString(FallbackMachineIdKey, generatedId);
            PlayerPrefs.Save();

            return generatedId;
        }
    }
}
