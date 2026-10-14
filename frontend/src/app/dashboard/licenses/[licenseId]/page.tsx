import { getLicense } from "@/features/licenses/server";
import { RevokeLicenseButton } from "@/features/licenses/components/revoke-license-button";
import type { LicenseStatus } from "@/features/licenses/types";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";

type LicenseDetailsPageProps = {
    params: Promise<{
        licenseId: string;
    }>;
};

const dateFormatter = new Intl.DateTimeFormat("en-MY", {
    dateStyle: "medium",
    timeStyle: "short",
});

const statusClasses: Record<LicenseStatus, string> = {
    ACTIVE: "bg-green-100 text-green-800",
    SUSPENDED: "bg-amber-100 text-amber-800",
    REVOKED: "bg-red-100 text-red-800",
};
import { getActivationsForLicense } from "@/features/activations/server";
import { ActivationList } from "@/features/activations/components/activation-list";
export default async function LicenseDetailsPage({ params }: LicenseDetailsPageProps) {

    const { licenseId } = await params;
    const result = await getLicense(licenseId);

    if (result.status === "unauthorized") {
        redirect("/login");
    }

    if (result.status === "not-found") {
        notFound();
    }

    const license = result.license;


    const activationsPage = await getActivationsForLicense(license.id);

    if (activationsPage === null) {
        redirect("/login");
    }
    return (
        <main className="mx-auto max-w-6xl px-6 py-12">
            <Link
                className="text-sm font-medium text-slate-600 hover:text-slate-950"
                href={`/dashboard/products/${license.productId}`}
            >
                &larr; Back to product
            </Link>

            <div className="mt-6 rounded-lg border border-slate-200 bg-white p-6">
                <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        <h1 className="text-3xl font-semibold">License details</h1>
                        <p className="mt-2 text-slate-600">{license.customerEmail}</p>
                    </div>

                    <span
                        className={`rounded-full px-3 py-1 text-sm font-medium ${statusClasses[license.status]}`}
                    >
                        {license.status}
                    </span>
                </div>

                <dl className="mt-8 grid gap-6 border-t border-slate-200 pt-6 sm:grid-cols-2">
                    <div>
                        <dt className="text-sm font-medium text-slate-500">Key ID</dt>
                        <dd className="mt-1 font-mono text-sm">{license.keyId}</dd>
                    </div>

                    <div>
                        <dt className="text-sm font-medium text-slate-500">
                            Maximum activations
                        </dt>
                        <dd className="mt-1 text-sm">{license.maxActivations}</dd>
                    </div>

                    <div>
                        <dt className="text-sm font-medium text-slate-500">Expires</dt>
                        <dd className="mt-1 text-sm">
                            {license.expiresAt === null
                                ? "Never"
                                : dateFormatter.format(new Date(license.expiresAt))}
                        </dd>
                    </div>

                    <div>
                        <dt className="text-sm font-medium text-slate-500">Issued</dt>
                        <dd className="mt-1 text-sm">
                            {dateFormatter.format(new Date(license.createdAt))}
                        </dd>
                    </div>

                    <div>
                        <dt className="text-sm font-medium text-slate-500">License ID</dt>
                        <dd className="mt-1 break-all font-mono text-sm">{license.id}</dd>
                    </div>
                </dl>
            </div>

            <ActivationList activations={activationsPage.content} />

            {license.status !== "REVOKED" && (
                <section className="mt-8 rounded-lg border border-red-200 bg-red-50 p-6">
                    <h2 className="text-lg font-semibold text-red-950">Revoke license</h2>
                    <p className="mt-1 text-sm text-red-800">
                        Revoking prevents this license from being used for future activations.
                    </p>

                    <div className="mt-4">
                        <RevokeLicenseButton licenseId={license.id} />
                    </div>
                </section>
            )}
        </main>
    );
}
