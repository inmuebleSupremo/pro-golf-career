import { describe, expect, it } from "vitest";

import { validShotSelection, type Situation } from "../../components/play/play-shared";
import { formatShotDistance, shotDistanceReadout } from "./distance";

function situation(distanceToPin = 339): Situation {
  const family = (name: string) => ({ family: name, available: true, shapes: [{ shape: "STRAIGHT", available: true }] });
  return {
    holeNumber: 1, par: 4, shotNumber: 1, strokesThisHole: 0, distanceToPin, lie: "FAIRWAY",
    pinLateral: 0, minReach: 0, maxReach: 400, shotRevision: "1:1",
    guidance: {
      safe: { x: 0, y: 240 }, primary: { x: 0, y: 264 }, aggressive: { x: 0, y: 300 },
      strategicOptions: [],
      clubs: [
        { club: "DRIVER", label: "Driver", nominalCarry: 290, normalReach: 306, families: [family("FULL")] },
        { club: "SEVEN_IRON", label: "7 Iron", nominalCarry: 170, normalReach: 181, families: [family("FULL"), family("PITCH")] },
      ],
    },
  };
}

describe("shot distance display", () => {
  it("keeps long and ordinary approach distances in yards", () => {
    expect(formatShotDistance(281)).toBe("281 yd");
    expect(formatShotDistance(50.33)).toBe("50 yd");
  });

  it("shows close shots and physical putting with useful feet precision", () => {
    expect(formatShotDistance(2)).toBe("6.0 ft");
    expect(formatShotDistance(0.4, true)).toBe("1.2 ft");
    expect(formatShotDistance(0.01, true)).toBe("<0.1 ft");
    expect(formatShotDistance(17, true)).toBe("51 ft");
  });

  it("compares pin, manual target and the current server club reach", () => {
    const shot = situation();
    const driver = validShotSelection(shot, { club: "DRIVER", technique: "FULL", shape: "STRAIGHT" });
    expect(shotDistanceReadout(shot, { x: 0, y: 0 }, { x: 0, y: 264 }, driver))
      .toEqual({ pin: "339 yd", target: "264 yd", reach: "306 yd" });
    expect(shotDistanceReadout(shot, { x: 0, y: 0 }, { x: 0, y: 8 }, driver).target).toBe("24 ft");

    const iron = validShotSelection(shot, { club: "SEVEN_IRON", technique: "PITCH", shape: "STRAIGHT" });
    expect(shotDistanceReadout(shot, { x: 0, y: 0 }, { x: 0, y: 264 }, iron).reach).toBe("181 yd");
    expect(shotDistanceReadout(shot, { x: 0, y: 0 }, { x: 0, y: 264 }, { ...iron, technique: "FULL" }).reach)
      .toBe("181 yd"); // The API exposes one normal reach per club, independent of shot family.
  });

  it("uses the next shot's server guidance and avoids a stale or invented reach", () => {
    const next = situation(95);
    next.shotRevision = "1:2";
    next.lie = "PRIMARY_ROUGH";
    next.guidance!.clubs = [{
      club: "PITCHING_WEDGE", label: "Pitching Wedge", nominalCarry: 110, normalReach: 118,
      families: [{ family: "PITCH", available: true, shapes: [{ shape: "STRAIGHT", available: true }] }],
    }];
    const selection = validShotSelection(next, { club: "DRIVER", technique: "FULL", shape: "STRAIGHT" });
    expect(shotDistanceReadout(next, { x: 0, y: 160 }, { x: 0, y: 210 }, selection))
      .toEqual({ pin: "95 yd", target: "50 yd", reach: "118 yd" });
    expect(shotDistanceReadout(next, { x: 0, y: 160 }, { x: 0, y: 210 }, { ...selection, club: "UNKNOWN" }).reach)
      .toBe("—");
  });
});

