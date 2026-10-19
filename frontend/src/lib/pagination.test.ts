import { describe, expect, it } from "vitest";
import { parsePage } from "./pagination";

describe("parsePage", () => {
  it.each([undefined, "", "abc", "0", "-2", "1.5"])(
    "returns page 1 for invalid value %s",
    (value) => {
      expect(parsePage(value)).toBe(1);
    },
  );

  it("returns a valid positive page number", () => {
    expect(parsePage("4")).toBe(4);
  });

  it("uses the first value when Next.js provides an array", () => {
    expect(parsePage(["3", "8"])).toBe(3);
  });
});
