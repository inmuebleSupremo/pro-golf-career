import { renderToStaticMarkup } from "react-dom/server";
import { beforeEach, expect, it, vi } from "vitest";

import { HubDashboard } from "./hub-dashboard";

const state = vi.hoisted(() => ({
  overview: null as unknown,
  records: null as unknown,
  fitness: null as unknown,
  profile: null as unknown,
  achievements: [] as unknown[],
}));

vi.mock("next/link", () => ({
  default: ({
    href,
    children,
    className,
  }: {
    href: string;
    children: React.ReactNode;
    className?: string;
  }) => (
    <a href={href} className={className}>
      {children}
    </a>
  ),
}));
vi.mock("next/image", () => ({
  default: ({ src }: { src: string }) => <span data-image={src} />,
}));
vi.mock("@/lib/play/scene", () => ({
  sceneBackdrop: () => ({ src: "/scenes/links-1.jpg" }),
  applySceneFallback: vi.fn(),
}));
vi.mock("@/components/command/identity-strip", () => ({
  NextAction: ({
    hasPendingEvent,
    pendingEventFinished,
    eventInProgress,
  }: {
    hasPendingEvent: boolean;
    pendingEventFinished: boolean;
    eventInProgress: boolean;
  }) => (
    <span
      data-progression={
        pendingEventFinished
          ? "finish"
          : hasPendingEvent
            ? eventInProgress
              ? "continue"
              : "play"
            : "advance"
      }
    />
  ),
}));
vi.mock("@/lib/api/queries", () => ({
  useCareerOverview: () => ({ data: state.overview, isPending: false, isError: false }),
  usePlayerProfile: () => ({ data: { playerProfile: state.profile } }),
  usePlayerFitness: () => ({ data: { playerFitness: state.fitness } }),
  useCareerRecords: () => ({
    data: { careerRecords: state.records },
    isPending: false,
    isError: false,
  }),
  useAchievements: () => ({ data: { achievements: state.achievements } }),
}));

beforeEach(() => {
  state.overview = {
    world: { season: 1, week: 4, hasPendingEvent: false, pendingEventFinished: false },
    currentEvent: null,
    playerScorecard: null,
    playerSchedule: [
      {
        tournamentId: "1",
        name: "West Bluff Open",
        location: "Sandbelt",
        courseType: "LINKS",
        tier: "DEVELOPMENT",
        prestige: "REGULAR",
        entered: true,
        week: 6,
      },
      {
        tournamentId: "2",
        name: "Later Open",
        location: "North",
        courseType: "PARKLAND",
        tier: "DEVELOPMENT",
        prestige: "REGULAR",
        entered: true,
        week: 8,
      },
    ],
    playerSeasonStats: [
      { season: 1, events: 1, wins: 0, topTens: 0, bestFinish: 24, earnings: 725 },
    ],
    newsFeed: [
      { season: 1, type: "RISING_PROSPECT", headline: "Soto makes strong start", prominence: 1 },
    ],
  };
  state.profile = {
    attributes: [
      { attribute: "DRIVING_ACCURACY", value: 77 },
      { attribute: "IRONS_ACCURACY", value: 76 },
    ],
  };
  state.fitness = { fitness: 0.92, availability: "AVAILABLE" };
  state.records = {
    events: [
      { eventName: "Sand Valley Open", results: [{ season: 1, date: "2026-04-12", position: 24 }] },
    ],
  };
  state.achievements = [
    { id: "first-win", title: "First Win", unlocked: false, seasonUnlocked: null },
  ];
});

it("shows real event, golfer, season and all four weekly summaries with existing destinations", () => {
  const html = renderToStaticMarkup(<HubDashboard id="career-1" />);
  expect(html).toContain("West Bluff Open");
  expect(html).toContain("Sandbelt");
  expect(html).toContain("Development Tour");
  expect(html).toContain('data-progression="advance"');
  expect(html).toContain("Condition");
  expect(html).toContain("92%");
  expect(html).toContain("Travel");
  expect(html).toContain("Next stop · Week 6");
  expect(html).toContain("Next open week");
  expect(html).toContain("Week 5");
  expect(html.match(/Season 1 · Week 4/g)).toHaveLength(1);
  expect(html).toContain("Last result");
  expect(html).toContain("24th");
  expect(html).toContain("Sand Valley Open");
  expect(html).toContain("76.5");
  for (const route of ["profile", "seasons", "achievements", "news"]) {
    expect(html).toContain(`href="/career/career-1/${route}"`);
  }
  expect(html).not.toContain("World Rank");
  expect(html).not.toContain("Hall of Fame");
});

it("shows neutral first-career states and advancement without an event", () => {
  state.overview = {
    ...(state.overview as object),
    playerSchedule: [],
    playerSeasonStats: [],
    newsFeed: [],
  };
  state.records = { events: [] };
  const html = renderToStaticMarkup(<HubDashboard id="career-1" />);
  expect(html).toContain("No upcoming event");
  expect(html).toContain('data-progression="advance"');
  expect(html).toContain("Debut pending");
  expect(html).toContain("No open week confirmed");
  expect(html).not.toContain("Next rest window");
  expect(html).toContain("No upcoming entered event");
});

it("keeps pending and finished tournament actions distinct", () => {
  state.overview = {
    ...(state.overview as object),
    world: { season: 1, week: 6, hasPendingEvent: true, pendingEventFinished: false },
    currentEvent: { name: "West Bluff Open", location: "Sandbelt", courseType: "LINKS" },
    playerScorecard: { roundNumber: 2, currentHole: 3, totalStrokes: 7 },
  };
  expect(renderToStaticMarkup(<HubDashboard id="career-1" />)).toContain(
    'data-progression="continue"',
  );
  state.overview = {
    ...(state.overview as object),
    world: { season: 1, week: 6, hasPendingEvent: true, pendingEventFinished: true },
  };
  expect(renderToStaticMarkup(<HubDashboard id="career-1" />)).toContain(
    'data-progression="finish"',
  );
});
