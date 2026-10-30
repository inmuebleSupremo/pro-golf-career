import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import samples from "@/lib/play/course-family-preview-fixtures.json";
import type { HoleGeom, ResolvedShot } from "@/lib/play/hole-geometry";
import type { ShotTrace } from "@/lib/play/trace-presentation";
import { Hole2d } from "./hole-2d";

const hole = (samples as { identity: string; hole: HoleGeom }[])[0].hole;
const tee = hole.geometry!.tee;
const cup = hole.geometry!.cup;
const contact = { x: tee.x + (cup.x - tee.x) * .55, y: tee.y + (cup.y - tee.y) * .55 };
const finalPoint = { x: contact.x, y: contact.y + 2 };
const trace: ShotTrace = { club: "SEVEN_IRON", origin: tee, aimPoint: cup,
  contact: { position: contact, surface: "GREEN" },
  airbornePath: [{ progress: 0, position: tee, height: 0 },
    { progress: 1, position: contact, height: 0 }],
  roll: { from: contact, to: finalPoint }, transition: null, finalPoint };
const shot: ResolvedShot = { finalSurface: "GREEN", carry: 140, lateral: 0,
  distanceRemaining: 35, trace };
const render = (options: { aimPoint: typeof cup; showAimMarker?: boolean; ball?: ResolvedShot }) =>
  renderToStaticMarkup(<Hole2d hole={hole} presentation="premium" previewAspect={1.3} {...options} />);

describe("production course aiming marker lifecycle", () => {
  it("shows an editable target before submission, hides it while pending and during the resolved flight", () => {
    const planning = render({ aimPoint: cup });
    expect(planning).toContain('aria-label="Selected target"');
    const pending = render({ aimPoint: cup, showAimMarker: false });
    expect(pending).not.toContain('aria-label="Selected target"');
    const playback = render({ aimPoint: cup, ball: shot });
    expect(playback).not.toContain('aria-label="Selected target"');
    expect(playback).not.toContain('aria-label="Intended target"');
    expect(playback).toContain('data-shot-ball="true"');
  });

  it("restores the submitted target after failure and uses the next target after completion", () => {
    expect(render({ aimPoint: cup, showAimMarker: true })).toContain('aria-label="Selected target"');
    const next = { x: cup.x + 3, y: cup.y - 8 };
    const nextMarkup = render({ aimPoint: next });
    expect(nextMarkup).toContain('aria-label="Selected target"');
    expect(nextMarkup).not.toContain('aria-label="Intended target"');
  });
});
