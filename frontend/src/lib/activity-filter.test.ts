import { describe, expect, it } from "vitest";
import {
  activityFilterHref,
  activityFilterToBoolean,
  parseActivityFilter,
} from "./activity-filter";

describe("activity filters", () => {
  it.each([
    ["active", "active"],
    ["inactive", "inactive"],
    ["all", "all"],
    ["unknown", "all"],
    [undefined, "all"],
  ] as const)("parses %s as %s", (value, expected) => {
    expect(parseActivityFilter(value)).toBe(expected);
  });

  it("maps filters to the backend boolean", () => {
    expect(activityFilterToBoolean("all")).toBeUndefined();
    expect(activityFilterToBoolean("active")).toBe(true);
    expect(activityFilterToBoolean("inactive")).toBe(false);
  });

  it("builds filter links and preserves the selected filter for later pages", () => {
    expect(activityFilterHref("/items", "all")).toBe("/items");
    expect(activityFilterHref("/items", "active")).toBe(
      "/items?status=active",
    );
    expect(activityFilterHref("/items", "inactive", 3)).toBe(
      "/items?status=inactive&page=3",
    );
  });
});
