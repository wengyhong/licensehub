import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getProducts } from "@/features/products/server";
import Link from "next/link";
import { Pagination } from "@/components/ui/pagination";
import { parsePage } from "@/lib/pagination";
export const metadata: Metadata = {
    title: "Products"
};

type ProductsPageProps = {
    searchParams: Promise<{
        page?: string | string[];
    }>;
};


export default async function ProductsPage({ searchParams }: ProductsPageProps) {


    const params = await searchParams;
    const requestedPage = parsePage(params.page);

    const productsPage = await getProducts(requestedPage - 1);
    if (productsPage === null) {
        redirect("/login");
    }
    if (
        productsPage.totalElements > 0 &&
        requestedPage > productsPage.totalPages
    ) {
        redirect(
            `/dashboard/products?page=${productsPage.totalPages}`,
        );
    }
    return (
        <main className="mx-auto max-w-6xl px-6 py-12">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-semibold">Products</h1>
                    <p className="mt-2 text-slate-600">
                        Manage the software products you license.
                    </p>
                </div>

                <Link
                    className="rounded-md bg-slate-900 px-4 py-2 text-white hover:bg-slate-700"
                    href="/dashboard/products/new"
                >
                    Create product
                </Link>
            </div>

            {productsPage.content.length === 0 ? (
                <div className="mt-8 rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
                    <h2 className="font-medium">No products yet</h2>
                    <p className="mt-2 text-sm text-slate-600">
                        Create your first product to begin issuing licenses.
                    </p>
                </div>
            ) : (
                <div className="mt-8 overflow-hidden rounded-lg border border-slate-200 bg-white">
                    <ul className="divide-y divide-slate-200">
                        {productsPage.content.map((product) => (
                            <li className="px-5 py-4" key={product.id}>
                                <h2 className="font-medium">
                                    <Link
                                        className="hover:underline"
                                        href={`/dashboard/products/${product.id}`}
                                    >
                                        {product.name}
                                    </Link>
                                </h2>

                                {product.description !== null && (
                                    <p className="mt-1 text-sm text-slate-600">
                                        {product.description}
                                    </p>
                                )}

                                <p className="mt-2 text-xs text-slate-500">
                                    Created{" "}
                                    {new Intl.DateTimeFormat("en-MY", {
                                        dateStyle: "medium",
                                    }).format(new Date(product.createdAt))}
                                </p>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            <Pagination
                basePath="/dashboard/products"
                currentPage={productsPage.page + 1}
                totalPages={productsPage.totalPages}
            />
        </main>
    );

}
