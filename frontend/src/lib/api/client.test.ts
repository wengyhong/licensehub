import { afterEach, describe, expect, it, vi } from "vitest";
import {
  api,
  apiFetch,
  ApiError,
  getApiErrorMessage,
  getApiFieldErrors,
} from "./client";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("ApiError", () => {
  it("uses the Problem Details detail as its message", () => {
    const error = new ApiError(400, {
      title: "Validation failed",
      detail: "One or more fields are invalid",
    });

    expect(error.message).toBe("One or more fields are invalid");
    expect(error.status).toBe(400);
    expect(error.name).toBe("ApiError");
  });

  it("uses the title when detail is missing", () => {
    const error = new ApiError(409, {
      title: "Product has existing licenses",
    });

    expect(error.message).toBe("Product has existing licenses");
  });

  it("returns field errors from a Problem Details response", () => {
    const error = new ApiError(400, {
      errors: {
        name: "must not be blank",
      },
    });

    expect(getApiFieldErrors(error)).toEqual({
      name: "must not be blank",
    });
  });

  it("uses safe fallbacks for unexpected errors", () => {
    const error = new Error("Network connection failed");

    expect(getApiErrorMessage(error, "Request failed.")).toBe(
      "Request failed.",
    );
    expect(getApiFieldErrors(error)).toEqual({});
  });
});

describe("apiFetch", () => {
  it("returns a successful JSON response", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: "product-1" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch<{ id: string }>("/api/products/1")).resolves.toEqual(
      { id: "product-1" },
    );
    expect(fetchMock).toHaveBeenCalledWith("/api/products/1", {
      credentials: "include",
    });
  });

  it("throws an ApiError containing backend Problem Details", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            title: "Validation failed",
            status: 400,
            detail: "One or more fields are invalid",
            errors: { name: "must not be blank" },
          }),
          {
            status: 400,
            headers: { "Content-Type": "application/problem+json" },
          },
        ),
      ),
    );

    const request = apiFetch("/api/products");

    await expect(request).rejects.toMatchObject({
      name: "ApiError",
      status: 400,
      message: "One or more fields are invalid",
      problem: {
        errors: { name: "must not be blank" },
      },
    });
  });

  it("adds the CSRF token to mutation requests", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            headerName: "X-CSRF-TOKEN",
            parameterName: "_csrf",
            token: "csrf-token",
          }),
          {
            status: 200,
            headers: { "Content-Type": "application/json" },
          },
        ),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await api.delete("/api/products/product-1");

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[0][0]).toBe("/api/auth/csrf");

    const [path, options] = fetchMock.mock.calls[1] as [string, RequestInit];
    const headers = new Headers(options.headers);

    expect(path).toBe("/api/products/product-1");
    expect(options.method).toBe("DELETE");
    expect(options.credentials).toBe("include");
    expect(headers.get("X-CSRF-TOKEN")).toBe("csrf-token");
  });
});
