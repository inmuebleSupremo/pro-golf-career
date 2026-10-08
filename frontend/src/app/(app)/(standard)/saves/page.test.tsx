import { readFileSync } from "node:fs";

import { describe, expect, it } from "vitest";

describe("SavesPage", () => {
  it("mounts the established home menu at the /saves route", () => {
    const source = readFileSync(new URL("./page.tsx", import.meta.url), "utf8");

    expect(source).toContain('import { HomeMenu } from "@/components/app/home-menu"');
    expect(source).toContain("return <HomeMenu />");
  });
});
