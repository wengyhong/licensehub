"use client";

import { useRouter } from "next/navigation";
import { useState, type SubmitEvent } from "react";
import { createProduct } from "@/features/products/api";

export function CreateProductForm() {
  const router = useRouter();

  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    const form = event.currentTarget;
    const formData = new FormData(form);
    const name = formData.get("name");
    const description = formData.get("description");

    if (typeof name !== "string" || typeof description !== "string") {
      setError("Invalid product details.");
      return;
    }

    setError(null);
    setSubmitting(true);

    try {
      await createProduct({
        name: name.trim(),
        description:
          description.trim() === "" ? null : description.trim(),
      });

      form.reset();
      router.refresh();
    } catch {
      setError("The product could not be created.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form
      className="mt-8 space-y-5 rounded-lg border border-slate-200 bg-white p-6"
      onSubmit={handleSubmit}
    >
      <h2 className="text-lg font-semibold">Create product</h2>

      <div>
        <label className="block text-sm font-medium" htmlFor="product-name">
          Name
        </label>
        <input
          className="mt-2 w-full rounded-md border border-slate-300 px-3 py-2"
          id="product-name"
          name="name"
          maxLength={150}
          required
        />
      </div>

      <div>
        <label
          className="block text-sm font-medium"
          htmlFor="product-description"
        >
          Description
        </label>
        <textarea
          className="mt-2 min-h-24 w-full rounded-md border border-slate-300 px-3 py-2"
          id="product-description"
          name="description"
          maxLength={1000}
        />
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
        {submitting ? "Creating..." : "Create product"}
      </button>
    </form>
  );
}