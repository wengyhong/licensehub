export type ActivityFilter = "all" | "active" | "inactive";

export function parseActivityFilter(
  value: string | string[] | undefined,
): ActivityFilter {
  const rawValue = Array.isArray(value) ? value[0] : value;

  return rawValue === "active" || rawValue === "inactive"
    ? rawValue
    : "all";
}

export function activityFilterToBoolean(
  filter: ActivityFilter,
): boolean | undefined {
  if (filter === "all") {
    return undefined;
  }

  return filter === "active";
}

export function activityFilterHref(
  basePath: string,
  filter: ActivityFilter,
  page = 1,
): string {
  const query = new URLSearchParams();

  if (filter !== "all") {
    query.set("status", filter);
  }

  if (page > 1) {
    query.set("page", page.toString());
  }

  const queryString = query.toString();
  return queryString === "" ? basePath : `${basePath}?${queryString}`;
}
