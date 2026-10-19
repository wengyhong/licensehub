import Link from "next/link";
import {
  activityFilterHref,
  type ActivityFilter,
} from "@/lib/activity-filter";

type ActivityFilterLinksProps = {
  ariaLabel: string;
  basePath: string;
  value: ActivityFilter;
};

const options: Array<{ label: string; value: ActivityFilter }> = [
  { label: "All", value: "all" },
  { label: "Active", value: "active" },
  { label: "Inactive", value: "inactive" },
];

export function ActivityFilterLinks({
  ariaLabel,
  basePath,
  value,
}: ActivityFilterLinksProps) {
  return (
    <nav
      aria-label={ariaLabel}
      className="mt-4 inline-flex overflow-hidden rounded-md border border-slate-300 bg-white"
    >
      {options.map((option) => {
        const selected = option.value === value;

        return (
          <Link
            aria-current={selected ? "page" : undefined}
            className={
              selected
                ? "bg-slate-900 px-3 py-2 text-sm font-medium text-white"
                : "border-l border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 first:border-l-0 hover:bg-slate-50"
            }
            href={activityFilterHref(basePath, option.value)}
            key={option.value}
          >
            {option.label}
          </Link>
        );
      })}
    </nav>
  );
}
