import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { getProduct } from "@/features/products/server";
import { UpdateProductForm } from "@/features/products/components/update-product-form";
import { DeleteProductButton } from "@/features/products/components/delete-product-button";

type ProductDetailsPageProps = {
    params: Promise<{ productId: string }>;
}
import { getLicensesForProduct } from "@/features/licenses/server";
import { LicenseList } from "@/features/licenses/components/license-list";
import { IssueLicenseForm } from "@/features/licenses/components/issue-license-form";
const dateFormatter = new Intl.DateTimeFormat("en-MY", {
    dateStyle: "medium",
    timeStyle: "short"
});

export default async function ProductDetailsPage({ params, }: ProductDetailsPageProps) {
    const { productId } = await params;
    const result = await getProduct(productId);

    if (result.status === "unauthorized") {
        redirect("/login");
    }

    if (result.status === "not-found") {
        notFound();
    }

    const product = result.product;
    const licensesPage = await getLicensesForProduct(product.id);

    if (licensesPage === null) {
        redirect("/login");
    }
    return (
        <main className="mx-auto max-w-6xl px-6 py-12">
            <Link
                className="text-sm font-medium text-slate-600 hover:text-slate-950"
                href="/dashboard/products"
            >
                ← Back to products
            </Link>

            <div className="mt-6 rounded-lg border border-slate-200 bg-white p-6">
                <h1 className="text-3xl font-semibold">{product.name}</h1>

                {product.description !== null ? (
                    <p className="mt-3 text-slate-600">{product.description}</p>
                ) : (
                    <p className="mt-3 text-sm italic text-slate-500">
                        No description provided.
                    </p>
                )}

                <dl className="mt-8 grid gap-6 border-t border-slate-200 pt-6 sm:grid-cols-2">
                    <div>
                        <dt className="text-sm font-medium text-slate-500">Product ID</dt>
                        <dd className="mt-1 break-all font-mono text-sm">{product.id}</dd>
                    </div>

                    <div>
                        <dt className="text-sm font-medium text-slate-500">Created</dt>
                        <dd className="mt-1 text-sm">
                            {dateFormatter.format(new Date(product.createdAt))}
                        </dd>
                    </div>

                    <div>
                        <dt className="text-sm font-medium text-slate-500">Last updated</dt>
                        <dd className="mt-1 text-sm">
                            {dateFormatter.format(new Date(product.updatedAt))}
                        </dd>
                    </div>
                </dl>
            </div>

            <UpdateProductForm product={product} />

            <DeleteProductButton
                productId={product.id}
                productName={product.name}
            />
            <IssueLicenseForm productId={product.id} />

            <LicenseList licenses={licensesPage.content} />
        </main>
    );
}