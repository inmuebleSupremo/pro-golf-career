import { describe, expect, it } from "vitest";

import { validShotSelection, type Situation } from "./play-shared";

const point = { x: 0, y: 150 };

function situation(lie: string, clubs: NonNullable<Situation["guidance"]>["clubs"]): Situation {
  return {
    holeNumber: 1,
    par: 4,
    shotNumber: 2,
    strokesThisHole: 1,
    distanceToPin: 145,
    lie,
    pinLateral: 0,
    minReach: 0,
    maxReach: 300,
    shotRevision: "revision",
    guidance: { safe: point, primary: point, aggressive: point, clubs, strategicOptions: [] },
  };
}

function club(club: string, nominalCarry: number, families: NonNullable<Situation["guidance"]>["clubs"][number]["families"]) {
  return { club, label: club, nominalCarry, normalReach: nominalCarry, families };
}

function family(family: string, available: boolean, shapes: { shape: string; available: boolean }[]) {
  return { family, available, reason: available ? null : "Unavailable from this lie", shapes: shapes.map((shape) => ({ ...shape, reason: shape.available ? null : "Unavailable" })) };
}

describe("validShotSelection", () => {
  it("initializes a bunker shot with the server-authorized wedge and bunker technique", () => {
    const selection = validShotSelection(situation("BUNKER", [
      club("DRIVER", 260, [family("FULL", false, [{ shape: "STRAIGHT", available: false }])]),
      club("SAND_WEDGE", 80, [family("BUNKER", true, [{ shape: "STRAIGHT", available: true }])]),
    ]));

    expect(selection).toEqual({ club: "SAND_WEDGE", technique: "BUNKER", shape: "STRAIGHT" });
  });

  it("replaces an illegal recovery selection while preserving a legal player choice", () => {
    const recovery = situation("TREES", [
      club("DRIVER", 260, [family("FULL", false, [{ shape: "STRAIGHT", available: false }])]),
      club("SEVEN_IRON", 150, [family("FULL", true, [
        { shape: "STRAIGHT", available: true },
        { shape: "DRAW", available: true },
      ])]),
    ]);

    expect(validShotSelection(recovery, { club: "DRIVER", technique: "FULL", shape: "STRAIGHT" }))
      .toEqual({ club: "SEVEN_IRON", technique: "FULL", shape: "STRAIGHT" });
    expect(validShotSelection(recovery, { club: "SEVEN_IRON", technique: "FULL", shape: "DRAW" }))
      .toEqual({ club: "SEVEN_IRON", technique: "FULL", shape: "DRAW" });
  });

  it("keeps the dedicated fringe putting route available without inventing a club strike", () => {
    const selection = validShotSelection(situation("FRINGE", []), {
      club: "PUTTER",
      technique: "PUTT",
      shape: "STRAIGHT",
    });

    expect(selection).toEqual({ club: "PUTTER", technique: "PUTT", shape: "STRAIGHT" });
  });

  it("replaces a saved fringe putt with a legal strike in a physical event", () => {
    const fringe = situation("FRINGE", [
      club("GAP_WEDGE", 90, [
        family("CHIP", true, [{ shape: "STRAIGHT", available: true }]),
        family("PITCH", true, [{ shape: "STRAIGHT", available: true }]),
      ]),
    ]);
    expect(validShotSelection(fringe, { club: "PUTTER", technique: "PUTT", shape: "STRAIGHT" }, false))
      .toEqual({ club: "GAP_WEDGE", technique: "CHIP", shape: "STRAIGHT" });
    expect(validShotSelection(fringe, { club: "GAP_WEDGE", technique: "PITCH", shape: "STRAIGHT" }, false))
      .toEqual({ club: "GAP_WEDGE", technique: "PITCH", shape: "STRAIGHT" });
  });

  it("lets a physical fringe selection switch between a putt and legal chip or pitch", () => {
    const fringe = situation("FRINGE", [club("GAP_WEDGE", 90, [
      family("CHIP", true, [{ shape: "STRAIGHT", available: true }]),
      family("PITCH", true, [{ shape: "STRAIGHT", available: true }]),
    ])]);
    const chip = validShotSelection(fringe);
    expect(chip.technique).toBe("CHIP");
    const putt = validShotSelection(fringe, { ...chip, technique: "PUTT" }, true);
    expect(putt).toEqual({ club: "GAP_WEDGE", technique: "PUTT", shape: "STRAIGHT" });
    expect(validShotSelection(fringe, { ...putt, technique: "PITCH" }, true).technique).toBe("PITCH");
    expect(validShotSelection(situation("PRIMARY_ROUGH", fringe.guidance!.clubs), putt, true).technique)
      .not.toBe("PUTT");
  });
});
