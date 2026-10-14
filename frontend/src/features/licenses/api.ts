import { api } from "@/lib/api/client";
import type { License } from "@/features/licenses/types";

export type CreateLicenseRequest = {
  customerEmail: string;
  maxActivations: number;
  expiresAt: string | null;
};

export type IssuedLicenseResponse = {
  license: License;
  licenseKey: string;
};

export function issueLicense(
  productId: string,
  request: CreateLicenseRequest,
): Promise<IssuedLicenseResponse> {
  return api.post<IssuedLicenseResponse>(
    `/api/products/${encodeURIComponent(productId)}/licenses`,
    request,
  );
}

export function revokeLicense(licenseId: string): Promise<void> {
  return api.post<void>(
    `/api/licenses/${encodeURIComponent(licenseId)}/revoke`,
  );
}
