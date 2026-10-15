import Link from "next/link";

type PaginationProps = {
  basePath: string;
  currentPage: number;
  totalPages: number;
};

function pageHref(basePath: string, page: number): string {
  return page === 1 ? basePath : `${basePath}?page=${page}`;
}

export function Pagination({
  basePath,
  currentPage,
  totalPages,
}: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav
      aria-label="Pagination"
      className="mt-6 flex items-center justify-between"
    >
      {currentPage > 1 ? (
        <Link
          className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50"
          href={pageHref(basePath, currentPage - 1)}
        >
          Previous
        </Link>
      ) : (
        <span className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-400">
          Previous
        </span>
      )}

      <span className="text-sm text-slate-600">
        Page {currentPage} of {totalPages}
      </span>

      {currentPage < totalPages ? (
        <Link
          className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50"
          href={pageHref(basePath, currentPage + 1)}
        >
          Next
        </Link>
      ) : (
        <span className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-400">
          Next
        </span>
      )}
    </nav>
  );
}