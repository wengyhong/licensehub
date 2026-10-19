import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { UpdateProductForm } from "@/features/products/components/update-product-form";
import { getProduct } from "@/features/products/server";

export const metadata: Metadata = {
  title: "Edit product",
};

type EditProductPageProps = {
  params: Promise<{
    productId: string;
  }>;
};

export default async function EditProductPage({
  params,
}: EditProductPageProps) {
  const { productId } = await params;
  const result = await getProduct(productId);

  if (result.status === "unauthorized") {
    redirect("/login");
  }

  if (result.status === "not-found") {
    notFound();
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-12">
      <Link
        className="text-sm font-medium text-slate-600 hover:text-slate-950"
        href={`/dashboard/products/${result.product.id}`}
      >
        &larr; Back to product
      </Link>

      <div className="mt-6">
        <h1 className="text-3xl font-semibold">Edit {result.product.name}</h1>
        <p className="mt-2 text-slate-600">
          Update the product name or description.
        </p>
      </div>

      <UpdateProductForm product={result.product} />
    </main>
  );
}
