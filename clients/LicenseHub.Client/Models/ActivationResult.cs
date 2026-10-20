using System;

namespace LicenseHub.Client.Models
{
    public sealed class ActivationResult
    {
        public Guid Id { get; set; }

        public Guid LicenseId { get; set; }

        public string? MachineName { get; set; }

        public DateTimeOffset ActivatedAt { get; set; }

        public DateTimeOffset LastSeenAt { get; set; }

        public string? ActivationToken { get; set; }
    }
}
