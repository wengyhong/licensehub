import Link from "next/link";
import { getCurrentUser } from "@/features/auth/server";
import { LogoutButton } from "@/features/auth/components/logout-button";

export async function AuthNavigation() {
  const user = await getCurrentUser();

  if (user === null) {
    return (
      <nav aria-label="Primary navigation" className="flex items-center gap-4">
        <Link
          className="text-sm text-slate-600 hover:text-slate-950"
          href="/login"
        >
          Log in
        </Link>

        <Link
          className="rounded-md bg-slate-900 px-3 py-2 text-sm text-white hover:bg-slate-700"
          href="/register"
        >
          Create account
        </Link>
      </nav>
    );
  }

  return (
    <nav aria-label="Primary navigation" className="flex items-center gap-4">
      <Link
        className="text-sm text-slate-600 hover:text-slate-950"
        href="/dashboard"
      >
        Dashboard
      </Link>

      <Link
        className="text-sm text-slate-600 hover:text-slate-950"
        href="/dashboard/products"
      >
        Products
      </Link>

      <LogoutButton />
    </nav>
  );
}
