import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { getProduct } from "@/features/products/server";
import { DeleteProductButton } from "@/features/products/components/delete-product-button";

type ProductDetailsPageProps = {
    params: Promise<{ productId: string }>,
    searchParams: Promise<{
        page?: string | string[];
        status?: string | string[];
    }>;
}
import { getLicensesForProduct } from "@/features/licenses/server";
import { LicenseList } from "@/features/licenses/components/license-list";
import { parsePage } from "@/lib/pagination";
import { Pagination } from "@/components/ui/pagination";
import {
    activityFilterHref,
    activityFilterToBoolean,
    parseActivityFilter,
} from "@/lib/activity-filter";
const dateFormatter = new Intl.DateTimeFormat("en-MY", {
    dateStyle: "medium",
    timeStyle: "short"
});

export default async function ProductDetailsPage({ params, searchParams }: ProductDetailsPageProps) {
    const [{ productId }, query] = await Promise.all([params, searchParams]);

    const requestedPage = parsePage(query.page);
    const filter = parseActivityFilter(query.status);
    const result = await getProduct(productId);

    if (result.status === "unauthorized") {
        redirect("/login");
    }

    if (result.status === "not-found") {
        notFound();
    }

    const product = result.product;
    const productPath = `/dashboard/products/${product.id}`;
    const licensesPage = await getLicensesForProduct(
        product.id,
        requestedPage - 1,
        activityFilterToBoolean(filter),
    );

    if (licensesPage === null) {
        redirect("/login");
    }

    if (
        licensesPage.totalElements > 0 &&
        requestedPage > licensesPage.totalPages
    ) {
        redirect(activityFilterHref(productPath, filter, licensesPage.totalPages));
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
                <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="min-w-0 flex-1">
                        <h1 className="text-3xl font-semibold">{product.name}</h1>

                        {product.description !== null ? (
                            <p className="mt-3 text-slate-600">{product.description}</p>
                        ) : (
                            <p className="mt-3 text-sm italic text-slate-500">
                                No description provided.
                            </p>
                        )}
                    </div>

                    <div className="flex shrink-0 items-start gap-3">
                        <Link
                            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                            href={`/dashboard/products/${product.id}/edit`}
                        >
                            Edit
                        </Link>

                        <DeleteProductButton
                            productId={product.id}
                            productName={product.name}
                        />
                    </div>
                </div>

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

            <LicenseList
                filter={filter}
                filterBasePath={productPath}
                issueLicenseHref={`/dashboard/products/${product.id}/licenses/new`}
                licenses={licensesPage.content}
            />

            <Pagination
                basePath={productPath}
                currentPage={licensesPage.page + 1}
                query={{ status: filter === "all" ? undefined : filter }}
                totalPages={licensesPage.totalPages}
            />


        </main>
    );
}
