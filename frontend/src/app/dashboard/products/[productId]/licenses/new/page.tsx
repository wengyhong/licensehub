import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { IssueLicenseForm } from "@/features/licenses/components/issue-license-form";
import { getProduct } from "@/features/products/server";

export const metadata: Metadata = {
  title: "Issue license",
};

type IssueLicensePageProps = {
  params: Promise<{
    productId: string;
  }>;
};

export default async function IssueLicensePage({
  params,
}: IssueLicensePageProps) {
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
        <h1 className="text-3xl font-semibold">Issue license</h1>
        <p className="mt-2 text-slate-600">
          Generate a license for {result.product.name}.
        </p>
      </div>

      <IssueLicenseForm productId={result.product.id} />
    </main>
  );
}
