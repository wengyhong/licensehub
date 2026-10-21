using System;
using UnityEngine;

namespace LicenseHub.UnityDemo
{
    internal static class ActivationTokenStore
    {
        public static void Save(Guid productId, string activationToken)
        {
            if (string.IsNullOrWhiteSpace(activationToken))
            {
                throw new ArgumentException(
                    "Activation token must not be blank.",
                    nameof(activationToken));
            }

            PlayerPrefs.SetString(GetKey(productId), activationToken);
            PlayerPrefs.Save();
        }

        public static bool TryLoad(Guid productId, out string activationToken)
        {
            activationToken = PlayerPrefs.GetString(
                GetKey(productId),
                string.Empty);

            return !string.IsNullOrWhiteSpace(activationToken);
        }

        public static void Delete(Guid productId)
        {
            PlayerPrefs.DeleteKey(GetKey(productId));
            PlayerPrefs.Save();
        }

        private static string GetKey(Guid productId)
        {
            return $"licensehub.activation-token.{productId:D}";
        }
    }
}