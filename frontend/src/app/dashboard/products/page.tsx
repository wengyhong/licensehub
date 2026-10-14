import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getProducts } from "@/features/products/server";
import { CreateProductForm } from "@/features/products/components/create-product-form";
import Link from "next/link";
export const metadata: Metadata = {
    title: "Products"
};



export default async function ProductsPage() {
    const productsPage = await getProducts();
    if (productsPage === null) {
        redirect("/login");
    }

    return (
        <main className="mx-auto max-w-6xl px-6 py-12">
            <div>
                <h1 className="text-3xl font-semibold">Products</h1>
                <p className="mt-2 text-slate-600">
                    Manage the software products you license.
                </p>
            </div>
            <CreateProductForm />

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
        </main>
    );

}