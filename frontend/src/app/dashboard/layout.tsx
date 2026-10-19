import { redirect } from "next/navigation";
import { getCurrentUser } from "@/features/auth/server";

export default async function DashboardLayout({
  children,
}: LayoutProps<"/dashboard">) {
  const user = await getCurrentUser();

  if (user === null) {
    redirect("/login");
  }

  return children;
}
