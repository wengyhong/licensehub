import type {
    License,
    LicenseStatus,
} from "@/features/licenses/types";

import Link from "next/link";

type LicenseListProps = {
    licenses: License[];
};

const dateFormatter = new Intl.DateTimeFormat("en-MY", {
    dateStyle: "medium",
});

const statusClasses: Record<LicenseStatus, string> = {
    ACTIVE: "bg-green-100 text-green-800",
    SUSPENDED: "bg-amber-100 text-amber-800",
    REVOKED: "bg-red-100 text-red-800",
}

export function LicenseList({ licenses }: LicenseListProps) {
    return (
        <section className="mt-8">
            <div>
                <h2 className="text-2xl font-semibold">Licenses</h2>
                <p className="mt-1 text-sm text-slate-600">
                    Licenses issued for this product.
                </p>
            </div>

            {licenses.length === 0 ? (
                <div className="mt-5 rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
                    <h3 className="font-medium">No licenses issued</h3>
                    <p className="mt-2 text-sm text-slate-600">
                        Issue the first license for this product.
                    </p>
                </div>
            ) : (
                <div className="mt-5 overflow-hidden rounded-lg border border-slate-200 bg-white">
                    <ul className="divide-y divide-slate-200">
                        {licenses.map((license) => (
                            <li className="px-5 py-4" key={license.id}>
                                <div className="flex flex-wrap items-start justify-between gap-3">
                                    <div>
                                        <Link
                                            className="font-mono font-semibold hover:underline"
                                            href={`/dashboard/licenses/${license.id}`}
                                        >
                                            License {license.keyId}
                                        </Link>
                                        <p className="mt-1 text-sm text-slate-600">
                                            {license.customerEmail}
                                        </p>
                                    </div>

                                    <span
                                        className={`rounded-full px-2.5 py-1 text-xs font-medium ${statusClasses[license.status]}`}
                                    >
                                        {license.status}
                                    </span>
                                </div>

                                <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-sm text-slate-600">
                                    <span>
                                        Issued: {dateFormatter.format(new Date(license.createdAt))}
                                    </span>

                                    <span>
                                        Maximum activations: {license.maxActivations}
                                    </span>

                                    <span>
                                        Expires:{" "}
                                        {license.expiresAt === null
                                            ? "Never"
                                            : dateFormatter.format(new Date(license.expiresAt))}
                                    </span>
                                </div>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </section>
    );
}
