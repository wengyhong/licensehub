"use client"

import { useRouter } from "next/navigation";
import { useState } from "react";
import { revokeLicense } from "../api";
import { getApiErrorMessage } from "@/lib/api/client";

type RevokeLicenseProps = {
    licenseId: string;
}

export function RevokeLicenseButton( {licenseId} : RevokeLicenseProps) {
    const router = useRouter();
    const [error, setError] = useState<string | null>(null);
    const [revoking, setRevoking] = useState(false);


    async function handleDelete() {
        const confirmed = window.confirm(
            `Revoke this license? It will no longer be accepted for activation.`,
        );

        if (!confirmed) {
            return;
        }

        setRevoking(true);
        setError(null);

        try {
            await revokeLicense(licenseId);
            router.refresh();

        } catch(error) {
            setError(getApiErrorMessage(error,"The license could not be revoked."));
            setRevoking(false);
        }
    }

    return (
    <div className="flex items-center gap-3">
      {error !== null && (
        <span className="text-sm text-red-700" role="alert">
          {error}
        </span>
      )}

      <button
        className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
        disabled={revoking}
        onClick={() => void handleDelete()}
        type="button"
      >
        {revoking ? "Revoking..." : "Revoke"}
      </button>
    </div>
  );
}
