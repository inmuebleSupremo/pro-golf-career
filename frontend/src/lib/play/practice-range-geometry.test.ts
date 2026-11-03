import { describe, expect, it } from "vitest";

import { projectRangePoint, projectRangeShotPoint, rangeBayMat, rangeBayOrigin, RANGE_BAY_CENTERS,
  RANGE_MOBILE_VIEWBOX, rangeFieldPath, unprojectRangePoint } from "./practice-range-geometry";

describe("driving range projection", () => {
  it("puts the widened fairway at the scene edges and keeps world targets reversible", () => {
    expect(rangeFieldPath(250)).toBe("M95 80 Q600 75 1105 80 L1260 630 L-60 630Z");
    for (const target of [{ x: -18, y: 36 }, { x: 95, y: 116 }, { x: -145, y: 251 }]) {
      expect(unprojectRangePoint(projectRangePoint(target), 220)).toEqual(target);
    }
  });

  it("clamps free aim to the same lateral and distance envelope sent to the dock", () => {
    expect(unprojectRangePoint({ x: -100, y: 0 }, 220)).toEqual({ x: -220, y: 320 });
    expect(unprojectRangePoint({ x: 1300, y: 700 }, 220)).toEqual({ x: 220, y: 15 });
  });

  it("keeps every server target visible in the cropped mobile scene", () => {
    const targets = [
      { x: -18, y: 36 }, { x: 48, y: 58 }, { x: -65, y: 89 },
      { x: 95, y: 116 }, { x: -110, y: 167 }, { x: 135, y: 205 }, { x: -145, y: 251 },
    ];
    for (const target of targets) {
      const point = projectRangePoint(target);
      expect(point.x).toBeGreaterThan(RANGE_MOBILE_VIEWBOX.x + 70);
      expect(point.x).toBeLessThan(RANGE_MOBILE_VIEWBOX.x + RANGE_MOBILE_VIEWBOX.width - 70);
      expect(unprojectRangePoint(point, 220)).toEqual(target);
    }
  });

  it("launches from each selected mat and rejoins the fixed world projection before the nearest flag", () => {
    expect(RANGE_MOBILE_VIEWBOX).toEqual({ x: 0, width: 1200 });
    RANGE_BAY_CENTERS.forEach((center, index) => {
      const bayNumber = index + 1;
      expect(projectRangeShotPoint(rangeBayOrigin(bayNumber))).toEqual(rangeBayMat(bayNumber));
      expect(rangeBayMat(bayNumber).x).toBe(center);
    });
    expect(projectRangeShotPoint({ x: 10, y: 25 })).toEqual(projectRangePoint({ x: 10, y: 25 }));
    expect(projectRangeShotPoint({ x: -18, y: 36 })).toEqual(projectRangePoint({ x: -18, y: 36 }));
  });
});
