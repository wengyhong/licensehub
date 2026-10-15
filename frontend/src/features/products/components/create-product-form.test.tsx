import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { CreateProductForm } from "./create-product-form";

const mocks = vi.hoisted(() => ({
  createProduct: vi.fn(),
  refresh: vi.fn(),
}));

vi.mock("@/features/products/api", () => ({
  createProduct: mocks.createProduct,
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    refresh: mocks.refresh,
  }),
}));

describe("CreateProductForm", () => {
  beforeEach(() => {
    mocks.createProduct.mockReset();
    mocks.refresh.mockReset();
  });

  it("trims and submits product details", async () => {
    mocks.createProduct.mockResolvedValue(undefined);
    render(<CreateProductForm />);

    fireEvent.change(screen.getByRole("textbox", { name: "Name" }), {
      target: { value: "  LicenseHub Desktop  " },
    });
    fireEvent.change(screen.getByRole("textbox", { name: "Description" }), {
      target: { value: "  Desktop client  " },
    });
    fireEvent.submit(
      screen.getByRole("button", { name: "Create product" }).closest("form")!,
    );

    await waitFor(() => {
      expect(mocks.createProduct).toHaveBeenCalledWith({
        name: "LicenseHub Desktop",
        description: "Desktop client",
      });
    });
    expect(mocks.refresh).toHaveBeenCalledOnce();
    expect(screen.getByRole("textbox", { name: "Name" })).toHaveValue("");
  });

  it("sends an empty description as null", async () => {
    mocks.createProduct.mockResolvedValue(undefined);
    render(<CreateProductForm />);

    fireEvent.change(screen.getByRole("textbox", { name: "Name" }), {
      target: { value: "LicenseHub API" },
    });
    fireEvent.submit(
      screen.getByRole("button", { name: "Create product" }).closest("form")!,
    );

    await waitFor(() => {
      expect(mocks.createProduct).toHaveBeenCalledWith({
        name: "LicenseHub API",
        description: null,
      });
    });
  });

  it("displays backend field validation errors", async () => {
    mocks.createProduct.mockRejectedValue(
      new ApiError(400, {
        title: "Validation failed",
        detail: "One or more fields are invalid",
        errors: { name: "must not be blank" },
      }),
    );
    render(<CreateProductForm />);

    const nameInput = screen.getByRole("textbox", { name: "Name" });
    fireEvent.change(nameInput, { target: { value: "   " } });
    fireEvent.submit(
      screen.getByRole("button", { name: "Create product" }).closest("form")!,
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Name must not be blank",
    );
    expect(nameInput).toHaveAttribute("aria-invalid", "true");
    expect(mocks.refresh).not.toHaveBeenCalled();
  });

  it("displays a general API error when there are no field errors", async () => {
    mocks.createProduct.mockRejectedValue(
      new ApiError(409, {
        title: "Conflict",
        detail: "The product already exists",
      }),
    );
    render(<CreateProductForm />);

    fireEvent.change(screen.getByRole("textbox", { name: "Name" }), {
      target: { value: "LicenseHub API" },
    });
    fireEvent.submit(
      screen.getByRole("button", { name: "Create product" }).closest("form")!,
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The product already exists",
    );
    expect(mocks.refresh).not.toHaveBeenCalled();
  });
});
