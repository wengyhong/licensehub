"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { deactivateActivation } from "@/features/activations/api";

type DeactivateActivationButtonProps = {
  activationId: string;
  licenseId: string;
  machineName: string | null;
};

export function DeactivateActivationButton({
  activationId,
  licenseId,
  machineName,
}: DeactivateActivationButtonProps) {
  const router = useRouter();

  const [deactivating, setDeactivating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleDeactivate() {
    const confirmed = window.confirm(
      `Deactivate "${machineName ?? "Unnamed machine"}"?`,
    );

    if (!confirmed) {
      return;
    }

    setError(null);
    setDeactivating(true);

    try {
      await deactivateActivation(licenseId, activationId);
      router.refresh();
    } catch {
      setError("The machine could not be deactivated.");
      setDeactivating(false);
    }
  }

  return (
    <div>
      <button
        className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50"
        disabled={deactivating}
        onClick={() => void handleDeactivate()}
        type="button"
      >
        {deactivating ? "Deactivating..." : "Deactivate"}
      </button>

      {error !== null && (
        <p className="mt-2 text-sm text-red-700" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}