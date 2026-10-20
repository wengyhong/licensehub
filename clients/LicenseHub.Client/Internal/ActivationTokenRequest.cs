using System;

namespace LicenseHub.Client.Internal
{
    internal sealed class ActivationTokenRequest
    {
        public ActivationTokenRequest(
            Guid productId,
            string activationToken,
            string machineFingerprint)
        {
            ProductId = productId;
            ActivationToken = activationToken;
            MachineFingerprint = machineFingerprint;
        }

        public Guid ProductId { get; }

        public string ActivationToken { get; }

        public string MachineFingerprint { get; }
    }
}
