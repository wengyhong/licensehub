import type { Metadata } from "next";
import Link from "next/link";
import { CreateProductForm } from "@/features/products/components/create-product-form";

export const metadata: Metadata = {
  title: "Create product",
};

export default function NewProductPage() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-12">
      <Link
        className="text-sm font-medium text-slate-600 hover:text-slate-900"
        href="/dashboard/products"
      >
        &larr; Back to products
      </Link>

      <div className="mt-6">
        <h1 className="text-3xl font-semibold">Create product</h1>
        <p className="mt-2 text-slate-600">
          Add a software product that you can issue licenses for.
        </p>
      </div>

      <CreateProductForm />
    </main>
  );
}
