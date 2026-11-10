import { expect, it } from "vitest";

import { seasonAdvancePollingInterval } from "./play";

it("polls a confirmed running operation but stops after a failed status read with stale RUNNING data", () => {
  expect(seasonAdvancePollingInterval("success", "RUNNING")).toBe(2_000);
  expect(seasonAdvancePollingInterval("error", "RUNNING")).toBe(false);
  expect(seasonAdvancePollingInterval("success", "COMPLETED")).toBe(false);
  expect(seasonAdvancePollingInterval("success", "FAILED")).toBe(false);
  expect(seasonAdvancePollingInterval("pending", null)).toBe(false);
});
