import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { RegisterForm } from "@/features/auth/components/register-form";
import { getCurrentUser } from "@/features/auth/server";

export const metadata: Metadata = {
  title: "Create account",
};

export default async function RegisterPage() {
  const user = await getCurrentUser();

  if (user !== null) {
    redirect("/dashboard");
  }

  return (
    <main className="mx-auto w-full max-w-md px-6 py-12">
      <h1 className="text-3xl font-semibold">Create your account</h1>
      <p className="mt-2 text-slate-600">
        Start managing your products and licenses.
      </p>

      <RegisterForm />

      <p className="mt-6 text-center text-sm text-slate-600">
        Already have an account?{" "}
        <Link className="font-medium text-slate-950 underline" href="/login">
          Log in
        </Link>
      </p>
    </main>
  );
}
