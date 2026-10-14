"use client"

import { useRouter } from "next/navigation";
import { useState, type SubmitEvent } from "react";
import { login } from "@/features/auth/api";

export function LoginForm() {

    const router = useRouter();
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
        event.preventDefault();

        const formData = new FormData(event.currentTarget);
        const email = formData.get("email");
        const password = formData.get("password");

        if (typeof email !== "string" || typeof password !== "string") {
            setError("Email and password are required.");
            return;
        }

        setError(null);
        setSubmitting(true);

        try {
            await login({ email, password });
            router.replace("/dashboard");
        } catch {
            setError("Invalid email or password.");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
            <div>
                <label className="block text-sm font-medium" htmlFor="email">
                    Email
                </label>
                <input
                    className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2"
                    id="email"
                    name="email"
                    type="email"
                    autoComplete="email"
                    maxLength={320}
                    required
                />
            </div>

            <div>
                <label className="block text-sm font-medium" htmlFor="password">
                    Password
                </label>
                <input
                    className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2"
                    id="password"
                    name="password"
                    type="password"
                    autoComplete="current-password"
                    maxLength={72}
                    required
                />
            </div>

            {error !== null && (
                <p className="text-sm text-red-700" role="alert">
                    {error}
                </p>
            )}

            <button
                className="w-full rounded-md bg-slate-900 px-4 py-2 text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
                disabled={submitting}
                type="submit"
            >
                {submitting ? "Logging in..." : "Log in"}
            </button>
        </form>
    );
}