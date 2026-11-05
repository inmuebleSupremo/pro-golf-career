import { describe, expect, it } from "vitest";

import { projectRangePoint, projectRangeShotPoint, rangeBayMat, rangeBayOrigin, RANGE_BAY_CENTERS,
  RANGE_MOBILE_VIEWBOX, rangeFieldPath, unprojectRangePoint } from "./practice-range-geometry";

describe("driving range projection", () => {
  it("puts the widened fairway at the scene edges and keeps world targets reversible", () => {
    expect(rangeFieldPath(250)).toBe("M95 80 Q600 75 1105 80 L1260 630 L-60 630Z");
    for (const target of [{ x: -18, y: 36 }, { x: 95, y: 116 }, { x: -145, y: 251 }]) {
      const selected = unprojectRangePoint(projectRangePoint(target), 220);
      expect(selected.x).toBeCloseTo(target.x, 8);
      expect(selected.y).toBeCloseTo(target.y, 8);
      const nearby = unprojectRangePoint(projectRangePoint({ x: target.x + 1.4, y: target.y - 2.3 }), 220);
      expect(nearby.x).toBeCloseTo(target.x + 1.4, 8);
      expect(nearby.y).toBeCloseTo(target.y - 2.3, 8);
      expect(nearby).not.toEqual(selected);
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
      expect(unprojectRangePoint(point, 220).x).toBeCloseTo(target.x, 8);
      expect(unprojectRangePoint(point, 220).y).toBeCloseTo(target.y, 8);
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
