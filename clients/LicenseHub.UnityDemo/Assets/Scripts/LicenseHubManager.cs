using System;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using LicenseHub.Client;
using LicenseHub.Client.Models;
using UnityEngine;

namespace LicenseHub.UnityDemo
{
    public sealed class LicenseHubManager : MonoBehaviour
    {
        [SerializeField]
        private string apiBaseUrl = "http://localhost:8080";

        [SerializeField]
        private string productId = "";

        private HttpClient httpClient;
        private LicenseHubClient client;
        private Guid configuredProductId;
        private string machineFingerprint;

        public bool HasSavedActivationToken =>
            client != null &&
            ActivationTokenStore.TryLoad(configuredProductId, out _);

        private void Awake()
        {
            if (!Uri.TryCreate(apiBaseUrl, UriKind.Absolute, out var baseAddress))
            {
                Debug.LogError("LicenseHub API URL is invalid.");
                enabled = false;
                return;
            }

            if (!Guid.TryParse(productId, out configuredProductId))
            {
                Debug.LogError("LicenseHub product ID is invalid.");
                enabled = false;
                return;
            }

            httpClient = new HttpClient
            {
                BaseAddress = baseAddress,
                Timeout = TimeSpan.FromSeconds(10)
            };

            client = new LicenseHubClient(httpClient, configuredProductId);
            machineFingerprint =
                MachineIdentityProvider.Get(configuredProductId);

            Debug.Log("LicenseHub client initialized successfully.");
        }

        public async Task<ActivationResult> ActivateAsync(
            string licenseKey,
            string machineName)
        {
            if (client == null)
            {
                throw new InvalidOperationException(
                    "LicenseHub has not been initialized.");
            }

            ActivationResult activation = await client.ActivateAsync(
                licenseKey,
                machineFingerprint,
                machineName);

            if (string.IsNullOrWhiteSpace(activation.ActivationToken))
            {
                throw new InvalidOperationException(
                    "LicenseHub did not return an activation token.");
            }

            ActivationTokenStore.Save(
                configuredProductId,
                activation.ActivationToken);

            return activation;
        }

        public async Task<ActivationResult> ValidateAsync()
        {
            if (client == null)
            {
                throw new InvalidOperationException(
                    "LicenseHub has not been initialized.");
            }

            if (!ActivationTokenStore.TryLoad(
                    configuredProductId,
                    out string activationToken))
            {
                throw new InvalidOperationException(
                    "No saved activation token was found.");
            }

            try
            {
                return await client.ValidateAsync(
                    activationToken,
                    machineFingerprint);
            }
            catch (LicenseHubApiException exception)
                when (exception.StatusCode == HttpStatusCode.Unauthorized ||
                      exception.StatusCode == HttpStatusCode.Forbidden)
            {
                ActivationTokenStore.Delete(configuredProductId);
                throw;
            }
        }

        private void OnDestroy()
        {
            httpClient?.Dispose();
        }
    }
}
