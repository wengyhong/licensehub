import { redirect } from "next/navigation";
import { getCurrentUser } from "@/features/auth/server";
import { LogoutButton } from "@/features/auth/components/logout-button";

export default async function DashboardLayout({
  children,
}: LayoutProps<"/dashboard">) {
  const user = await getCurrentUser();

  if (user === null) {
    redirect("/login");
  }

  return (
    <>
      <div className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-6xl px-6 py-3 text-sm text-slate-600">
          Signed in as{" "}
          <span className="font-medium text-slate-950">{user.email}</span>
        </div>
        <LogoutButton/>
      </div>

      {children}
    </>
  );
}