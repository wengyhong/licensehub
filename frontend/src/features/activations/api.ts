import { api } from "@/lib/api/client";

export function deactivateActivation(
  licenseId: string,
  activationId: string,
): Promise<void> {
  return api.post<void>(
    `/api/licenses/${encodeURIComponent(licenseId)}/activations/${encodeURIComponent(activationId)}/deactivate`,
  );
}