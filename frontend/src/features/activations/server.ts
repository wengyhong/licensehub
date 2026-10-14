import "server-only";

import { backendFetch } from "@/lib/api/server";
import type { PagedResponse } from "@/lib/api/types";
import type { MachineActivation } from "./types";

export async function getActivationsForLicense(
  licenseId: string,
): Promise<PagedResponse<MachineActivation> | null> {
  const response = await backendFetch(
    `/api/licenses/${encodeURIComponent(licenseId)}/activations`,
  );

  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`Activations request failed: ${response.status}`);
  }

  return response.json() as Promise<
    PagedResponse<MachineActivation>
  >;
}