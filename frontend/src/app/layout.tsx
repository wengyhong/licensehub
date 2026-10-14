import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
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

              <nav aria-label="Primary navigation" className="flex gap-4">
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