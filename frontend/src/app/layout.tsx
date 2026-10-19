import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
import { Suspense } from "react";
import { AuthNavigation } from "@/features/auth/components/auth-navigation";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: {
    default: "LicenseHub",
    template: "%s | LicenseHub",
  },
  description: "Manage software products, licenses, and machine activations.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable}`}
    >
      <body className="min-h-screen bg-slate-50 text-slate-950">
        <div className="flex min-h-screen flex-col">
          <header className="border-b border-slate-200 bg-white">
            <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
              <Link className="text-xl font-semibold" href="/">
                LicenseHub
              </Link>

              <Suspense
                fallback={
                  <div
                    aria-hidden="true"
                    className="h-9 w-48 animate-pulse rounded-md bg-slate-100"
                  />
                }
              >
                <AuthNavigation />
              </Suspense>
            </div>
          </header>

          <div className="flex-1">{children}</div>

          <footer className="border-t border-slate-200 bg-white">
            <div className="mx-auto max-w-6xl px-6 py-6 text-sm text-slate-500">
              LicenseHub
            </div>
          </footer>
        </div>
      </body>
    </html>
  );
}
