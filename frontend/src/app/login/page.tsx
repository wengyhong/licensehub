import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { LoginForm } from "@/features/auth/components/login-form";
import { getCurrentUser } from "@/features/auth/server";

export const metadata: Metadata = {
  title: "Log in",
};

export default async function LoginPage() {
  const user = await getCurrentUser();

  if (user !== null) {
    redirect("/dashboard");
  }

  return (
    <main className="mx-auto w-full max-w-md px-6 py-12">
      <h1 className="text-3xl font-semibold">Log in</h1>
      <p className="mt-2 text-slate-600">
        Access your products, licenses, and activations.
      </p>

      <LoginForm />

      <p className="mt-6 text-center text-sm text-slate-600">
        Need an account?{" "}
        <Link className="font-medium text-slate-950 underline" href="/register">
          Create one
        </Link>
      </p>
    </main>
  );
}
