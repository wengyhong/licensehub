"use client"

import { useRouter } from "next/navigation"
import { useState, type SubmitEvent } from "react"
import {
    apiFetch,
    getApiErrorMessage,
    getApiFieldErrors,
} from "@/lib/api/client"

type UserResponse = {
    id: string,
    email: string,
    createdAt: string;
};

export function RegisterForm() {
    const router = useRouter();

    const [error, setError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        const email = formData.get("email");
        const password = formData.get("password");

        if (typeof email !== "string" || typeof password !== "string") {
            setError("Email and password are required");
            return;
        }

        setError(null);
        setFieldErrors({});
        setSubmitting(true);

        try {
            await apiFetch<UserResponse>("/api/auth/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ email, password }),
            });



            router.push("/login?registered=true");
        } catch (error) {
            const nextFieldErrors = getApiFieldErrors(error);
            setFieldErrors(nextFieldErrors);

            if (Object.keys(nextFieldErrors).length === 0) {
                setError(
                    getApiErrorMessage(
                        error,
                        "Registration failed. Please check your details.",
                    ),
                );
            }
        }
        finally {
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
                    aria-describedby={fieldErrors.email ? "email-error" : undefined}
                    aria-invalid={fieldErrors.email !== undefined}
                    className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2"
                    id="email"
                    name="email"
                    type="email"
                    autoComplete="email"
                    maxLength={320}
                    required
                />
                {fieldErrors.email && (
                    <p className="mt-1 text-sm text-red-700" id="email-error" role="alert">
                        Email {fieldErrors.email}
                    </p>
                )}
            </div>

            <div>
                <label className="block text-sm font-medium" htmlFor="password">
                    Password
                </label>
                <input
                    aria-describedby={fieldErrors.password ? "password-error" : undefined}
                    aria-invalid={fieldErrors.password !== undefined}
                    className="mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2"
                    id="password"
                    name="password"
                    type="password"
                    autoComplete="new-password"
                    minLength={12}
                    maxLength={72}
                    required
                />
                {fieldErrors.password && (
                    <p
                        className="mt-1 text-sm text-red-700"
                        id="password-error"
                        role="alert"
                    >
                        Password {fieldErrors.password}
                    </p>
                )}
                <p className="mt-2 text-sm text-slate-500">
                    Use between 12 and 72 characters.
                </p>
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
                {submitting ? "Creating account..." : "Create account"}
            </button>
        </form>
    );

}

