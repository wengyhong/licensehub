import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Dashboard",
};

export default function DashboardPage() {

  return (
    <main className="mx-auto max-w-6xl px-6 py-12">
      <h1 className="text-3xl font-semibold">Dashboard</h1>
      <p className="mt-2 text-slate-600">
        You are logged in to LicenseHub.
      </p>

      <section className="mt-8 rounded-lg border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-semibold">Products</h2>
        <p className="mt-2 text-sm text-slate-600">
          Create products, issue licenses, and manage machine activations.
        </p>
        <Link
          className="mt-5 inline-flex rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
          href="/dashboard/products"
        >
          Manage products
        </Link>
      </section>
    </main>
  );
}
