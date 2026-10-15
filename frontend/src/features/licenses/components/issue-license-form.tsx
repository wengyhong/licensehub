"use client";

import { useRouter } from "next/navigation";
import { useState, type SubmitEvent } from "react";
import {
  issueLicense,
  type IssuedLicenseResponse,
} from "@/features/licenses/api";
import { getApiErrorMessage, getApiFieldErrors } from "@/lib/api/client";

type IssueLicenseFormProps = {
  productId: string;
};

export function IssueLicenseForm({
  productId,
}: IssueLicenseFormProps) {
  const router = useRouter();

  const [issued, setIssued] =
    useState<IssuedLicenseResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [copied, setCopied] = useState(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    const form = event.currentTarget;
    const formData = new FormData(form);

    const customerEmail = formData.get("customerEmail");
    const maxActivationsValue = formData.get("maxActivations");
    const expiresAtValue = formData.get("expiresAt");

    if (
      typeof customerEmail !== "string" ||
      typeof maxActivationsValue !== "string" ||
      typeof expiresAtValue !== "string"
    ) {
      setError("Invalid license details.");
      return;
    }

    const maxActivations = Number(maxActivationsValue);

    if (!Number.isInteger(maxActivations) || maxActivations < 1) {
      setError("Maximum activations must be a positive whole number.");
      return;
    }

    const expiration =
      expiresAtValue === "" ? null : new Date(expiresAtValue);

    if (expiration !== null && Number.isNaN(expiration.getTime())) {
      setError("The expiration date is invalid.");
      return;
    }

    setError(null);
    setFieldErrors({});
    setIssued(null);
    setCopied(false);
    setSubmitting(true);

    try {
      const response = await issueLicense(productId, {
        customerEmail: customerEmail.trim(),
        maxActivations,
        expiresAt:
          expiration === null ? null : expiration.toISOString(),
      });

      setIssued(response);
      form.reset();
      router.refresh();
    } catch (error) {
      const nextFieldErrors = getApiFieldErrors(error);
      setFieldErrors(nextFieldErrors);

      if (Object.keys(nextFieldErrors).length === 0) {
        setError(
          getApiErrorMessage(error, "The license could not be issued."),
        );
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCopy() {
    if (issued === null) {
      return;
    }

    try {
      await navigator.clipboard.writeText(issued.licenseKey);
      setCopied(true);
    } catch {
      setError("The license key could not be copied.");
    }
  }

  return (
    <section className="mt-8 rounded-lg border border-slate-200 bg-white p-6">
      <h2 className="text-xl font-semibold">Issue license</h2>
      <p className="mt-1 text-sm text-slate-600">
        Generate a license for a customer.
      </p>

      <form className="mt-6 space-y-5" onSubmit={handleSubmit}>
        <div>
          <label
            className="block text-sm font-medium"
            htmlFor="customer-email"
          >
            Customer email
          </label>
          <input
            aria-describedby={
              fieldErrors.customerEmail ? "customer-email-error" : undefined
            }
            aria-invalid={fieldErrors.customerEmail !== undefined}
            className="mt-2 w-full rounded-md border border-slate-300 px-3 py-2"
            id="customer-email"
            name="customerEmail"
            type="email"
            autoComplete="email"
            maxLength={320}
            required
          />
          {fieldErrors.customerEmail && (
            <p
              className="mt-1 text-sm text-red-700"
              id="customer-email-error"
              role="alert"
            >
              Customer email {fieldErrors.customerEmail}
            </p>
          )}
        </div>

        <div>
          <label
            className="block text-sm font-medium"
            htmlFor="max-activations"
          >
            Maximum activations
          </label>
          <input
            aria-describedby={
              fieldErrors.maxActivations
                ? "max-activations-error"
                : undefined
            }
            aria-invalid={fieldErrors.maxActivations !== undefined}
            className="mt-2 w-full rounded-md border border-slate-300 px-3 py-2"
            defaultValue={1}
            id="max-activations"
            name="maxActivations"
            type="number"
            min={1}
            step={1}
            required
          />
          {fieldErrors.maxActivations && (
            <p
              className="mt-1 text-sm text-red-700"
              id="max-activations-error"
              role="alert"
            >
              Maximum activations {fieldErrors.maxActivations}
            </p>
          )}
        </div>

        <div>
          <label
            className="block text-sm font-medium"
            htmlFor="expires-at"
          >
            Expiration date
          </label>
          <input
            aria-describedby={
              fieldErrors.expiresAt ? "expires-at-error" : undefined
            }
            aria-invalid={fieldErrors.expiresAt !== undefined}
            className="mt-2 w-full rounded-md border border-slate-300 px-3 py-2"
            id="expires-at"
            name="expiresAt"
            type="datetime-local"
          />
          {fieldErrors.expiresAt && (
            <p
              className="mt-1 text-sm text-red-700"
              id="expires-at-error"
              role="alert"
            >
              Expiration date {fieldErrors.expiresAt}
            </p>
          )}
          <p className="mt-2 text-sm text-slate-500">
            Leave blank for a license that does not expire.
          </p>
        </div>

        {error !== null && (
          <p className="text-sm text-red-700" role="alert">
            {error}
          </p>
        )}

        <button
          className="rounded-md bg-slate-900 px-4 py-2 text-white hover:bg-slate-700 disabled:opacity-50"
          disabled={submitting}
          type="submit"
        >
          {submitting ? "Issuing..." : "Issue license"}
        </button>
      </form>

      {issued !== null && (
        <div
          className="mt-6 rounded-md border border-amber-300 bg-amber-50 p-4"
          role="status"
        >
          <h3 className="font-semibold text-amber-950">
            Save this license key now
          </h3>
          <p className="mt-1 text-sm text-amber-900">
            LicenseHub will not display the full key again.
          </p>

          <code className="mt-4 block break-all rounded bg-white p-3 text-sm">
            {issued.licenseKey}
          </code>

          <button
            className="mt-3 rounded-md border border-amber-400 bg-white px-3 py-2 text-sm font-medium"
            onClick={() => void handleCopy()}
            type="button"
          >
            {copied ? "Copied" : "Copy key"}
          </button>
        </div>
      )}
    </section>
  );
}
