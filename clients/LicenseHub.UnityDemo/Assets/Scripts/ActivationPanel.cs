using System;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using LicenseHub.Client;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

namespace LicenseHub.UnityDemo
{
    public sealed class ActivationPanel : MonoBehaviour
    {
        [SerializeField]
        private LicenseHubManager licenseHubManager;

        [SerializeField]
        private TMP_InputField licenseKeyInput;

        [SerializeField]
        private TMP_InputField machineNameInput;

        [SerializeField]
        private Button activateButton;

        [SerializeField]
        private TMP_Text statusText;

        [SerializeField]
        private GameObject licensedContentRoot;

        [SerializeField]
        private TMP_Text licensedStatusText;

        private TMP_Text activateButtonLabel;
        private bool retriesValidation;

        private void Awake()
        {
            SetLicensedContentVisible(false);

            activateButtonLabel =
                activateButton.GetComponentInChildren<TMP_Text>();

            if (string.IsNullOrWhiteSpace(machineNameInput.text))
            {
                machineNameInput.text = SystemInfo.deviceName;
            }
        }

        private async void Start()
        {
            if (licenseHubManager.HasSavedActivationToken)
            {
                await ValidateSavedActivationAsync();
                return;
            }

            ShowActivationForm(string.Empty);
        }

        private void OnEnable()
        {
            activateButton.onClick.AddListener(HandlePrimaryActionClicked);
        }

        private void OnDisable()
        {
            activateButton.onClick.RemoveListener(HandlePrimaryActionClicked);
        }

        private async void HandlePrimaryActionClicked()
        {
            if (retriesValidation)
            {
                await ValidateSavedActivationAsync();
                return;
            }

            await ActivateAsync();
        }

        private async Task ActivateAsync()
        {
            string licenseKey = licenseKeyInput.text.Trim();

            if (string.IsNullOrWhiteSpace(licenseKey))
            {
                statusText.text = "Enter a license key.";
                return;
            }

            string machineName = machineNameInput.text.Trim();

            activateButton.interactable = false;
            statusText.text = "Activating...";

            try
            {
                var activation = await licenseHubManager.ActivateAsync(
                    licenseKey,
                    machineName);

                licenseKeyInput.text = string.Empty;
                ShowLicensed(
                    $"License active.\nActivation ID: {activation.Id}");
            }
            catch (LicenseHubApiException exception)
            {
                statusText.text =
                    exception.Detail ??
                    exception.Title ??
                    $"Activation failed ({(int)exception.StatusCode}).";
            }
            catch (HttpRequestException)
            {
                statusText.text =
                    "Could not connect to LicenseHub. " +
                    "Check that the backend is running.";
            }
            catch (TaskCanceledException)
            {
                statusText.text = "LicenseHub did not respond in time.";
            }
            catch (Exception exception)
            {
                statusText.text = "An unexpected activation error occurred.";
                Debug.LogException(exception);
            }
            finally
            {
                activateButton.interactable = true;
            }
        }

        private async Task ValidateSavedActivationAsync()
        {
            ShowValidating();

            try
            {
                var activation = await licenseHubManager.ValidateAsync();

                ShowLicensed(
                    $"License valid. Game can start.\n" +
                    $"Activation ID: {activation.Id}");
                Debug.Log("LicenseHub saved activation validated successfully.");
            }
            catch (LicenseHubApiException exception)
                when (exception.StatusCode == HttpStatusCode.Unauthorized ||
                      exception.StatusCode == HttpStatusCode.Forbidden)
            {
                ShowActivationForm(
                    "The saved activation is no longer valid. " +
                    "Enter your license key again.");
            }
            catch (LicenseHubApiException exception)
            {
                ShowActivationForm(
                    exception.Detail ??
                    exception.Title ??
                    $"License validation failed ({(int)exception.StatusCode}).");
            }
            catch (HttpRequestException)
            {
                ShowValidationRetry(
                    "Could not connect to LicenseHub. " +
                    "Your saved activation has been preserved.");
            }
            catch (TaskCanceledException)
            {
                ShowValidationRetry(
                    "LicenseHub did not respond in time. " +
                    "Your saved activation has been preserved.");
            }
            catch (Exception exception)
            {
                ShowValidationRetry(
                    "An unexpected validation error occurred. " +
                    "Your saved activation has been preserved.");
                Debug.LogException(exception);
            }
        }

        private void ShowActivationForm(string message)
        {
            retriesValidation = false;
            SetLicensedContentVisible(false);
            licenseKeyInput.gameObject.SetActive(true);
            machineNameInput.gameObject.SetActive(true);
            activateButton.gameObject.SetActive(true);
            activateButton.interactable = true;
            activateButtonLabel.text = "Activate";
            statusText.text = message;
        }

        private void ShowValidating()
        {
            retriesValidation = false;
            SetLicensedContentVisible(false);
            licenseKeyInput.gameObject.SetActive(false);
            machineNameInput.gameObject.SetActive(false);
            activateButton.gameObject.SetActive(false);
            statusText.text = "Validating saved license...";
        }

        private void ShowLicensed(string message)
        {
            retriesValidation = false;
            licenseKeyInput.gameObject.SetActive(false);
            machineNameInput.gameObject.SetActive(false);
            activateButton.gameObject.SetActive(false);
            statusText.text = message;

            if (licensedStatusText != null)
            {
                licensedStatusText.text = message;
            }

            SetLicensedContentVisible(true);

            if (licensedContentRoot != null)
            {
                gameObject.SetActive(false);
            }
        }

        private void ShowValidationRetry(string message)
        {
            retriesValidation = true;
            SetLicensedContentVisible(false);
            licenseKeyInput.gameObject.SetActive(false);
            machineNameInput.gameObject.SetActive(false);
            activateButton.gameObject.SetActive(true);
            activateButton.interactable = true;
            activateButtonLabel.text = "Retry";
            statusText.text = message;
        }

        private void SetLicensedContentVisible(bool visible)
        {
            if (licensedContentRoot != null)
            {
                licensedContentRoot.SetActive(visible);
            }
        }
    }
}
