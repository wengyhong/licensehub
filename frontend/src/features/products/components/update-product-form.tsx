"use client"

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type SubmitEvent } from "react";
import { updateProduct } from "@/features/products/api";
import type { Product } from "../types";
import { getApiErrorMessage, getApiFieldErrors } from "@/lib/api/client";
type EditProductFormProps = {
    product: Product
};
export function UpdateProductForm({ product }: EditProductFormProps) {
    const router = useRouter();

    const [error, setError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
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
        setFieldErrors({});
        setSubmitting(true);

        try {
            await updateProduct(product.id, {
                name: name.trim(),
                description:
                    description.trim() === "" ? null : description.trim(),
            });
            router.replace(`/dashboard/products/${product.id}`);
            router.refresh();
        } catch(error) {
            const nextFieldErrors = getApiFieldErrors(error);
            setFieldErrors(nextFieldErrors);

            if (Object.keys(nextFieldErrors).length === 0) {
                setError(
                    getApiErrorMessage(error, "The product could not be updated."),
                );
            }
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <form
            className="mt-8 space-y-5 rounded-lg border border-slate-200 bg-white p-6"
            onSubmit={handleSubmit}
        >
            <h2 className="text-lg font-semibold">Edit product</h2>

            <div>
                <label className="block text-sm font-medium" htmlFor="product-name">
                    Name
                </label>
                <input defaultValue={product.name}
                    aria-describedby={
                        fieldErrors.name ? "update-product-name-error" : undefined
                    }
                    aria-invalid={fieldErrors.name !== undefined}
                    className="mt-2 w-full rounded-md border border-slate-300 px-3 py-2"
                    id="product-name"
                    name="name"
                    maxLength={150}
                    required
                />
                {fieldErrors.name && (
                    <p
                        className="mt-1 text-sm text-red-700"
                        id="update-product-name-error"
                        role="alert"
                    >
                        Name {fieldErrors.name}
                    </p>
                )}
            </div>

            <div>
                <label
                    className="block text-sm font-medium"
                    htmlFor="product-description"
                >
                    Description
                </label>
                <textarea defaultValue={product.description??""}
                    aria-describedby={
                        fieldErrors.description
                            ? "update-product-description-error"
                            : undefined
                    }
                    aria-invalid={fieldErrors.description !== undefined}
                    className="mt-2 min-h-24 w-full rounded-md border border-slate-300 px-3 py-2"
                    id="product-description"
                    name="description"
                    maxLength={1000}
                />
                {fieldErrors.description && (
                    <p
                        className="mt-1 text-sm text-red-700"
                        id="update-product-description-error"
                        role="alert"
                    >
                        Description {fieldErrors.description}
                    </p>
                )}
            </div>

            {error !== null && (
                <p className="text-sm text-red-700" role="alert">
                    {error}
                </p>
            )}

            <div className="flex flex-wrap items-center gap-3">
                <button
                    className="rounded-md bg-slate-900 px-4 py-2 text-white hover:bg-slate-700 disabled:opacity-50"
                    disabled={submitting}
                    type="submit"
                >
                    {submitting ? "Saving..." : "Save changes"}
                </button>

                <Link
                    className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                    href={`/dashboard/products/${product.id}`}
                >
                    Cancel
                </Link>
            </div>
        </form>
    );
}
