using System;
using System.Collections.Generic;
using System.Net;

namespace LicenseHub.Client
{
    public sealed class LicenseHubApiException : Exception
    {
        public LicenseHubApiException(
            HttpStatusCode statusCode,
            string? title,
            string? detail,
            IReadOnlyDictionary<string, string>? errors = null)
            : base(detail ?? title ?? $"LicenseHub returned HTTP {(int)statusCode}.")
        {
            StatusCode = statusCode;
            Title = title;
            Detail = detail;
            Errors = errors;
        }

        public HttpStatusCode StatusCode { get; }

        public string? Title { get; }

        public string? Detail { get; }

        public IReadOnlyDictionary<string, string>? Errors { get; }
    }
}
