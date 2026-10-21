using System;
using UnityEditor;
using UnityEngine;

namespace LicenseHub.UnityDemo.Editor
{
    public static class LicenseHubActivationTestTools
    {
        private const string EditorMachineIdOverrideKey =
            "licensehub.editor-machine-id-override";

        [MenuItem("Tools/LicenseHub/Testing/Simulate Lost Activation Token")]
        public static void SimulateLostActivationToken()
        {
            if (!TryGetConfiguredProductId(out Guid productId))
            {
                return;
            }

            DeleteActivationToken(productId);
            PlayerPrefs.Save();

            Debug.Log(
                "LicenseHub simulation: saved activation token deleted. " +
                "The machine fingerprint was not changed.");
        }

        [MenuItem("Tools/LicenseHub/Testing/Simulate Machine Reformat")]
        public static void SimulateMachineReformat()
        {
            if (!TryGetConfiguredProductId(out Guid productId))
            {
                return;
            }

            string simulatedMachineId = Guid.NewGuid().ToString("D");

            PlayerPrefs.SetString(
                EditorMachineIdOverrideKey,
                simulatedMachineId);
            DeleteActivationToken(productId);
            PlayerPrefs.Save();

            Debug.Log(
                "LicenseHub simulation: machine reformatted. " +
                $"The saved token was deleted and the Editor is now using " +
                $"simulated machine ID '{simulatedMachineId}'.");
        }

        [MenuItem("Tools/LicenseHub/Testing/Restore Real Machine Identity")]
        public static void RestoreRealMachineIdentity()
        {
            if (!TryGetConfiguredProductId(out Guid productId))
            {
                return;
            }

            PlayerPrefs.DeleteKey(EditorMachineIdOverrideKey);
            DeleteActivationToken(productId);
            PlayerPrefs.Save();

            Debug.Log(
                "LicenseHub simulation cleared. The Editor will use the real " +
                "machine fingerprint again. The saved activation token was deleted.");
        }

        private static bool TryGetConfiguredProductId(out Guid productId)
        {
            productId = Guid.Empty;

            if (EditorApplication.isPlayingOrWillChangePlaymode)
            {
                Debug.LogWarning(
                    "Exit Play Mode before using LicenseHub testing tools.");
                return false;
            }

            LicenseHubManager manager =
                UnityEngine.Object.FindFirstObjectByType<LicenseHubManager>();

            if (manager == null)
            {
                Debug.LogError(
                    "No LicenseHubManager was found in the active scene.");
                return false;
            }

            var serializedManager = new SerializedObject(manager);
            SerializedProperty productIdProperty =
                serializedManager.FindProperty("productId");

            if (productIdProperty == null ||
                !Guid.TryParse(productIdProperty.stringValue, out productId))
            {
                Debug.LogError(
                    "The LicenseHubManager product ID is missing or invalid.");
                return false;
            }

            return true;
        }

        private static void DeleteActivationToken(Guid productId)
        {
            PlayerPrefs.DeleteKey(
                $"licensehub.activation-token.{productId:D}");
        }
    }
}
