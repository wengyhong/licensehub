import Link from "next/link";

export default function LicenseNotFound() {
    return (
        <main className="mx-auto max-w-6xl px-6 py-12">
            <h1 className="text-3xl font-semibold">License not found</h1>
            <p className="mt-2 text-slate-600">
                The license does not exist or you do not have access to it.
            </p>

            <Link
                className="mt-6 inline-block font-medium underline"
                href="/dashboard/products"
            >
                Return to products
            </Link>
        </main>
    );
}
