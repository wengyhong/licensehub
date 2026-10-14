import "server-only";
import { backendFetch } from "@/lib/api/server";
import { PagedResponse } from "@/lib/api/types";
import { License } from "./types";

export async function getLicensesForProduct(
  productId: string,
): Promise<PagedResponse<License> | null> {
  const response = await backendFetch(
    `/api/products/${encodeURIComponent(productId)}/licenses`,
  );

  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`Licenses request failed: ${response.status}`);
  }

  return response.json() as Promise<PagedResponse<License>>;
}

export type LicenseLookupResult =
  | { status: "success"; license: License }
  | { status: "unauthorized" }
  | { status: "not-found" };

  export async function getLicense(
  licenseId: string,
): Promise<LicenseLookupResult>
{
    const response = await backendFetch(`/api/licenses/${encodeURIComponent(licenseId)}`);

    if (response.status === 401) {
        return { status: "unauthorized" };
      }

      if (response.status === 404) {
        return { status: "not-found" };
      }

       if (!response.ok) {
        throw new Error(`Product request failed: ${response.status}`);
      }

      const license = (await response.json()) as License;

      return { status: "success", license };


}