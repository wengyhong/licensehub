using System;

namespace LicenseHub.Client.Internal
{
    internal sealed class ActivationRequest
    {
        public ActivationRequest(
            Guid productId,
            string licenseKey,
            string machineFingerprint,
            string? machineName)
        {
            ProductId = productId;
            LicenseKey = licenseKey;
            MachineFingerprint = machineFingerprint;
            MachineName = machineName;
        }

        public Guid ProductId { get; }

        public string LicenseKey { get; }

        public string MachineFingerprint { get; }

        public string? MachineName { get; }
    }
}
