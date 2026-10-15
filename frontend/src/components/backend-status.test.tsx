import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { BackendStatus } from "./backend-status";

const { getMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
}));

vi.mock("@/lib/api/client", () => ({
  api: {
    get: getMock,
  },
}));

describe("BackendStatus", () => {
  beforeEach(() => {
    getMock.mockReset();
  });

  it("shows the checking state while waiting for a response", () => {
    getMock.mockReturnValue(new Promise(() => {}));

    render(<BackendStatus />);

    expect(screen.getByRole("status")).toHaveTextContent(
      "Checking backend",
    );
  });

  it("shows available when the backend reports UP", async () => {
    getMock.mockResolvedValue({ status: "UP" });

    render(<BackendStatus />);

    expect(
      await screen.findByText("Backend available"),
    ).toBeInTheDocument();
  });

  it("shows unavailable when the backend reports another status", async () => {
    getMock.mockResolvedValue({ status: "DOWN" });

    render(<BackendStatus />);

    expect(
      await screen.findByText("Backend unavailable"),
    ).toBeInTheDocument();
  });

  it("shows unavailable when the request fails", async () => {
    getMock.mockRejectedValue(new Error("Network error"));

    render(<BackendStatus />);

    expect(
      await screen.findByText("Backend unavailable"),
    ).toBeInTheDocument();
  });
});