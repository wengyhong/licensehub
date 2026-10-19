import "server-only";

import { backendFetch } from "@/lib/api/server";
import type { PagedResponse } from "@/lib/api/types";
import type { MachineActivation } from "./types";

export async function getActivationsForLicense(
  licenseId: string,
  page: number,
  active?: boolean,
): Promise<PagedResponse<MachineActivation> | null> {


  const query = new URLSearchParams({ page: page.toString(), size: "10" });

  if (active !== undefined) {
    query.set("active", active.toString());
  }

  const response = await backendFetch(
    `/api/licenses/${encodeURIComponent(licenseId)}/activations?${query.toString()}`,
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
