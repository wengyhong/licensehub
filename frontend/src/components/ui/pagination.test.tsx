import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Pagination } from "./pagination";

describe("Pagination", () => {
    it("renders nothing when there is only one page", () => {
        const { container } = render(
            <Pagination
                basePath="/dashboard/products"
                currentPage={1}
                totalPages={1}
            />,
        );

        expect(container).toBeEmptyDOMElement();
    });

    it("disables Previous on the first page", () => {
        render(
            <Pagination
                basePath="/dashboard/products"
                currentPage={1}
                totalPages={3}
            />,
        );

        expect(
            screen.queryByRole("link", { name: "Previous" }),
        ).not.toBeInTheDocument();

        expect(screen.getByText("Previous")).toBeInTheDocument();
        expect(screen.getByText("Page 1 of 3")).toBeInTheDocument();
    });

    it("links to the second page from the first page", () => {
        render(
            <Pagination
                basePath="/dashboard/products"
                currentPage={1}
                totalPages={3}
            />,
        );

        expect(screen.getByRole("link", { name: "Next" })).toHaveAttribute(
            "href",
            "/dashboard/products?page=2",
        );
    });

    it("creates correct links on a middle page", () => {
        render(
            <Pagination
                basePath="/dashboard/products"
                currentPage={2}
                totalPages={3}
            />,
        );

        expect(
            screen.getByRole("link", { name: "Previous" }),
        ).toHaveAttribute("href", "/dashboard/products");

        expect(screen.getByRole("link", { name: "Next" })).toHaveAttribute(
            "href",
            "/dashboard/products?page=3",
        );
    });

    it("preserves filters in pagination links", () => {
        render(
            <Pagination
                basePath="/dashboard/products/123"
                currentPage={2}
                query={{ status: "inactive" }}
                totalPages={3}
            />,
        );

        expect(
            screen.getByRole("link", { name: "Previous" }),
        ).toHaveAttribute(
            "href",
            "/dashboard/products/123?status=inactive",
        );

        expect(screen.getByRole("link", { name: "Next" })).toHaveAttribute(
            "href",
            "/dashboard/products/123?status=inactive&page=3",
        );
    });

    it("disables Next on the final page", () => {
        render(
            <Pagination
                basePath="/dashboard/products"
                currentPage={3}
                totalPages={3}
            />,
        );

        expect(
            screen.queryByRole("link", { name: "Next" }),
        ).not.toBeInTheDocument();

        expect(screen.getByText("Next")).toBeInTheDocument();
        expect(screen.getByText("Page 3 of 3")).toBeInTheDocument();
    });
});
