'use client'

import { useEffect, useState } from "react";
import { api } from "@/lib/api/client";
export default function Home() {

  type HealthState = "checking" | "available" | "unavailable";

  type HealthResponse = { status: string; };
  const [health, setHealth] = useState<HealthState>("checking");

  useEffect(() => {
    async function checkBackend() {
      try {
        const response = await api.get<HealthResponse>("/actuator/health");

        setHealth(response.status === "UP" ? "available" : "unavailable");

      } catch {
        setHealth("unavailable");

      }
    }

    void checkBackend();
  }, []);

  return (
    <main>
      <h1>LicenseHub</h1>
      <p>Backend status: {health}</p>
    </main>
  );
}
