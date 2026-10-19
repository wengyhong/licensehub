"use client";

import Link from "next/link";
import { useEffect } from "react";

type DashboardErrorProps = {
  error: Error & {
    digest?: string;
  };
  retry: () => void;
};

export default function DashboardError({
  error,
  retry,
}: DashboardErrorProps) {
  useEffect(() => {
    console.error("Dashboard error:", error);
  }, [error]);

  return (
    <main className="mx-auto max-w-6xl px-6 py-12">
      <section className="rounded-lg border border-red-200 bg-red-50 p-6">
        <h1 className="text-2xl font-semibold text-red-950">
          The dashboard could not be loaded
        </h1>

        <p className="mt-2 text-sm text-red-800">
          A temporary problem occurred while loading your data.
        </p>

        {error.digest && (
          <p className="mt-2 text-xs text-red-700">
            Error reference: {error.digest}
          </p>
        )}

        <div className="mt-6 flex flex-wrap gap-3">
          <button
            className="rounded-md bg-red-900 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
            onClick={retry}
            type="button"
          >
            Try again
          </button>

          <Link
            className="rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-950 hover:bg-red-100"
            href="/dashboard"
          >
            Return to dashboard
          </Link>
        </div>
      </section>
    </main>
  );
}