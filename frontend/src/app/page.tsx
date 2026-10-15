import Link from "next/link";
import { BackendStatus } from "@/components/backend-status";
import { getCurrentUser } from "@/features/auth/server";

const features = [
  {
    title: "Products",
    description:
      "Organize the software products for which you issue licenses.",
  },
  {
    title: "Licenses",
    description:
      "Issue licenses with expiration dates and activation limits.",
  },
  {
    title: "Machine activations",
    description:
      "See which machines are using a license and deactivate them when needed.",
  },
];

export default async function Home() {
  const user = await getCurrentUser();

  return (
    <main className="mx-auto max-w-6xl px-6 py-20">
      <section className="max-w-3xl">
        <BackendStatus />

        <h1 className="mt-8 text-5xl font-bold tracking-tight text-zinc-950">
          Manage software licenses with confidence.
        </h1>

        <p className="mt-6 text-lg leading-8 text-zinc-600">
          LicenseHub gives software teams one place to manage products,
          issue licenses, and control machine activations.
        </p>

        <div className="mt-8 flex flex-wrap gap-4">
          {user === null ? (
            <>
              <Link
                href="/register"
                className="rounded-md bg-zinc-900 px-5 py-3 font-medium text-white hover:bg-zinc-700"
              >
                Create account
              </Link>

              <Link
                href="/login"
                className="rounded-md border border-zinc-300 px-5 py-3 font-medium hover:bg-zinc-50"
              >
                Log in
              </Link>
            </>
          ) : (
            <>
              <Link
                href="/dashboard"
                className="rounded-md bg-zinc-900 px-5 py-3 font-medium text-white hover:bg-zinc-700"
              >
                Go to dashboard
              </Link>

              <Link
                href="/dashboard/products"
                className="rounded-md border border-zinc-300 px-5 py-3 font-medium hover:bg-zinc-50"
              >
                Manage products
              </Link>
            </>
          )}
        </div>
      </section>

      <section
        aria-labelledby="features-heading"
        className="mt-20 grid gap-6 md:grid-cols-3"
      >
        <h2 id="features-heading" className="sr-only">
          LicenseHub features
        </h2>

        {features.map((feature) => (
          <article
            key={feature.title}
            className="rounded-xl border border-zinc-200 p-6"
          >
            <h3 className="text-lg font-semibold text-zinc-950">
              {feature.title}
            </h3>

            <p className="mt-2 leading-7 text-zinc-600">
              {feature.description}
            </p>
          </article>
        ))}
      </section>
    </main>
  );
}