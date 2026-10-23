import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { StartingBackgroundSelect } from "./archetype-select";

describe("StartingBackgroundSelect", () => {
  it("presents all three challenge levels as one accessible choice", () => {
    const html = renderToStaticMarkup(
      <StartingBackgroundSelect value="ELITE_AMATEUR" onValueChange={() => {}} />,
    );

    expect(html).toContain('aria-label="Starting Background"');
    expect(html).toContain("Club Champion");
    expect(html).toContain("A challenging climb");
    expect(html).toContain("Elite Amateur");
    expect(html).toContain("The standard journey");
    expect(html).toContain("Highly Rated Prospect");
    expect(html).toContain("A stronger start");
    expect(html).toContain('aria-checked="true"');
  });
});
