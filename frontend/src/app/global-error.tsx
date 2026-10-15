"use client";

import { useEffect } from "react";

type GlobalErrorProps = {
  error: Error & {
    digest?: string;
  };
  retry: () => void;
};

export default function GlobalError({
  error,
  retry,
}: GlobalErrorProps) {
  useEffect(() => {
    console.error("Global application error:", error);
  }, [error]);

  return (
    <html lang="en">
      <head>
        <title>Something went wrong | LicenseHub</title>
      </head>

      <body
        style={{
          margin: 0,
          background: "#f8fafc",
          color: "#0f172a",
          fontFamily: "Arial, sans-serif",
        }}
      >
        <main
          style={{
            margin: "0 auto",
            maxWidth: "720px",
            padding: "64px 24px",
          }}
        >
          <h1>LicenseHub could not be loaded</h1>

          <p>
            An unexpected problem occurred. Check that the backend is running
            and try again.
          </p>

          {error.digest && (
            <p style={{ fontSize: "14px", color: "#475569" }}>
              Error reference: {error.digest}
            </p>
          )}

          <button
            onClick={retry}
            style={{
              marginTop: "16px",
              border: 0,
              borderRadius: "6px",
              background: "#0f172a",
              color: "white",
              cursor: "pointer",
              padding: "10px 16px",
            }}
            type="button"
          >
            Try again
          </button>
        </main>
      </body>
    </html>
  );
}