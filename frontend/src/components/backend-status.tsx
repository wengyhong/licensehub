"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api/client";

type HealthState = "checking" | "available" | "unavailable";

type HealthResponse = {
  status: string;
};

const labels: Record<HealthState, string> = {
  checking: "Checking backend",
  available: "Backend available",
  unavailable: "Backend unavailable",
};

const dotClasses: Record<HealthState, string> = {
  checking: "bg-amber-500",
  available: "bg-green-500",
  unavailable: "bg-red-500",
};

export function BackendStatus() {
  const [health, setHealth] = useState<HealthState>("checking");

  useEffect(() => {
    let cancelled = false;

    async function checkBackend() {
      try {
        const response =
          await api.get<HealthResponse>("/actuator/health");

        if (!cancelled) {
          setHealth(
            response.status === "UP" ? "available" : "unavailable",
          );
        }
      } catch {
        if (!cancelled) {
          setHealth("unavailable");
        }
      }
    }

    void checkBackend();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div
      role="status"
      className="inline-flex items-center gap-2 rounded-full border px-3 py-1 text-sm text-zinc-600"
    >
      <span
        aria-hidden="true"
        className={`h-2 w-2 rounded-full ${dotClasses[health]}`}
      />
      {labels[health]}
    </div>
  );
}