import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { tracePlaybackTiming, type ShotTrace } from "@/lib/play/trace-presentation";
import { ShotMotionFrame, ShotMotionOverlay } from "./shot-motion-overlay";

const trace: ShotTrace = {
  club: "SEVEN_IRON", origin: { x: 0, y: 0 }, aimPoint: { x: 0, y: 100 },
  contact: { position: { x: 0, y: 100 }, surface: "GREEN" },
  airbornePath: [
    { progress: 0, position: { x: 0, y: 0 }, height: 0 },
    { progress: .5, position: { x: 0, y: 50 }, height: 15 },
    { progress: 1, position: { x: 0, y: 100 }, height: 0 },
  ],
  roll: { from: { x: 0, y: 100 }, to: { x: 0, y: 104 } },
  transition: null, finalPoint: { x: 0, y: 104 },
};
const project = ({ x, y }: { x: number; y: number }) => ({ x: x + 10, y: 300 - y });
const props = { trace, project, contact: project(trace.contact.position), finalPoint: project(trace.finalPoint),
  contactEffect: "white", ballStroke: "black", statusY: 400, shadowFill: "gray" };
const render = (progress: number) => renderToStaticMarkup(<svg><ShotMotionFrame {...props} progress={progress} /></svg>);

describe("production shot motion SVG layer", () => {
  it("first renders the actual launch position without a final-position flash", () => {
    const initial = renderToStaticMarkup(<svg><ShotMotionOverlay {...props} /></svg>);
    expect(initial).toContain('data-shot-ball="true" cx="10" cy="300"');
    expect(initial).not.toContain('data-shot-final="true"');
    expect(initial).not.toContain('data-shot-contact="true"');
  });

  it("shows height and shadow in flight, then exact contact and final endpoints", () => {
    const timing = tracePlaybackTiming(trace);
    const apex = render(timing.flightMs / timing.totalMs / 2);
    expect(apex).toContain('data-shot-shadow="true" cx="10" cy="250"');
    expect(apex).toContain('data-shot-ball="true" cx="10"');
    expect(apex).not.toContain('data-shot-contact="true"');
    const contact = render(timing.flightMs / timing.totalMs);
    expect(contact).toContain('data-shot-ball="true" cx="10" cy="200"');
    expect(contact).toContain('data-shot-contact="true" cx="10" cy="200"');
    expect(contact).not.toContain('data-shot-shadow="true"');
    const final = render(1);
    expect(final).toContain('data-shot-final="true" cx="10" cy="196"');
    expect(final).not.toContain('data-shot-ball="true"');
  });

  it("preserves launch and release for a long driver trace", () => {
    const driver = { ...trace, club: "DRIVER", contact: { position: { x: 0, y: 255 }, surface: "FAIRWAY" },
      airbornePath: [], roll: { from: { x: 0, y: 255 }, to: { x: 0, y: 277 } },
      finalPoint: { x: 0, y: 277 } } satisfies ShotTrace;
    const driverProps = { ...props, trace: driver, contact: project(driver.contact.position), finalPoint: project(driver.finalPoint) };
    const timing = tracePlaybackTiming(driver);
    const start = renderToStaticMarkup(<svg><ShotMotionFrame {...driverProps} progress={0} /></svg>);
    const rolling = renderToStaticMarkup(<svg><ShotMotionFrame {...driverProps}
      progress={(timing.flightMs + timing.contactMs + timing.rollMs / 2) / timing.totalMs} /></svg>);
    expect(start).toContain('data-shot-ball="true" cx="10" cy="300"');
    expect(rolling).toContain("Rolling");
    expect(rolling).not.toContain('data-shot-final="true"');
  });
});
