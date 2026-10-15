"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { logout } from "@/features/auth/api";
import { getApiErrorMessage } from "@/lib/api/client";

export function LogoutButton() {
    const router = useRouter();
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    async function handleLogout() {
        setError(null);
        setSubmitting(true);

        try {
            await logout();
            router.replace("/login");
            router.refresh();
        } catch (error){
            setError(getApiErrorMessage(error,"Logout failed. Please try again."));
            setSubmitting(false);
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
        disabled={submitting}
        onClick={() => void handleLogout()}
        type="button"
      >
        {submitting ? "Logging out..." : "Log out"}
      </button>
    </div>
  );
}
