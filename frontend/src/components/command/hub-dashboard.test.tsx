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
        hostName: "Royal Bluff Golf Club",
        hostRegion: "Sandbelt",
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
  expect(html).toContain("Week 6");
  expect(html).toContain("Host: Royal Bluff Golf Club");
  expect(html.match(/Sandbelt/g)).toHaveLength(1);
  expect(html.indexOf("West Bluff Open")).toBeLessThan(html.indexOf("Sandbelt"));
  expect(html.indexOf("Sandbelt")).toBeLessThan(html.indexOf("Development Tour"));
  expect(html.indexOf("Development Tour")).toBeLessThan(html.indexOf("Host: Royal Bluff Golf Club"));
  expect(html).not.toContain("This tournament is on your schedule");
  expect(html).toContain("career-hub-hero");
  expect(html).toContain("career-hub-week");
  expect(html).toContain('data-image="/scenes/links-1.jpg"');
  expect(html).toContain("Career progression");
  expect(html).toContain('data-progression="advance"');
  expect(html).toContain("Condition");
  expect(html).toContain("92%");
  expect(html).toContain("Travel");
  expect(html).toContain("Week 8");
  expect(html).toContain("North");
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
  expect(html).toContain("No next stop scheduled");
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
  expect(renderToStaticMarkup(<HubDashboard id="career-1" />)).toContain(
    "Week 8",
  );
  state.overview = {
    ...(state.overview as object),
    world: { season: 1, week: 6, hasPendingEvent: true, pendingEventFinished: true },
  };
  expect(renderToStaticMarkup(<HubDashboard id="career-1" />)).toContain(
    'data-progression="finish"',
  );
});

it("shows the current host separately and travels to the next entered tournament", () => {
  state.overview = {
    ...(state.overview as object),
    world: { season: 1, week: 2, hasPendingEvent: true, pendingEventFinished: false },
    currentEvent: {
      name: "Sandhills Classic",
      location: "Pinehurst region, North Carolina, USA",
      courseType: "HEATHLAND",
      hostName: "North Point Golf Club",
      hostRegion: "Pinehurst region, North Carolina",
    },
    playerSchedule: [
      { tournamentId: "1", name: "Sandhills Classic", location: "Pinehurst region, North Carolina, USA", tier: "DEVELOPMENT", entered: true, week: 2 },
      { tournamentId: "2", name: "Available Open", location: "South Carolina, USA", tier: "DEVELOPMENT", entered: false, week: 3 },
      { tournamentId: "3", name: "Later Open", location: "Arizona, USA", tier: "DEVELOPMENT", entered: true, week: 8 },
      { tournamentId: "4", name: "Southeastern Invitational", location: "Georgia, USA", tier: "DEVELOPMENT", entered: true, week: 5 },
    ],
  };
  const html = renderToStaticMarkup(<HubDashboard id="career-1" />);
  expect(html).toContain("Sandhills Classic");
  expect(html).toContain("Pinehurst region, North Carolina, USA");
  expect(html.match(/Pinehurst region/g)).toHaveLength(1);
  expect(html).toContain("Development Tour");
  expect(html).toContain("Week 2");
  expect(html).toContain("Host: North Point Golf Club");
  expect(html).not.toContain("Host: North Point Golf Club · Pinehurst region");
  expect(html).toContain("Georgia");
  expect(html).toContain("Week 5 · USA");
  expect(html).not.toContain("Week 8 · USA");
});

it("shows no travel commitment when only unentered events follow a legacy current event", () => {
  state.overview = {
    ...(state.overview as object),
    world: { season: 1, week: 6, hasPendingEvent: true, pendingEventFinished: false },
    currentEvent: { name: "West Bluff Open", location: "Sandbelt", courseType: "LINKS" },
    playerSchedule: [
      { tournamentId: "1", name: "West Bluff Open", location: "Sandbelt", tier: "DEVELOPMENT", entered: true, week: 6 },
      { tournamentId: "2", name: "Available Open", location: "Ohio", tier: "DEVELOPMENT", entered: false, week: 7 },
    ],
  };
  const html = renderToStaticMarkup(<HubDashboard id="career-1" />);
  expect(html).toContain("No next stop scheduled");
  expect(html).not.toContain("Host:");
  expect(html).not.toContain("Ohio");
  expect(html).toContain('data-progression="play"');
});

it("keeps an unfamiliar destination complete within the two compact Travel lines", () => {
  state.overview = {
    ...(state.overview as object),
    playerSchedule: [
      { tournamentId: "1", name: "Current Open", location: "Sandbelt", tier: "DEVELOPMENT", entered: true, week: 6 },
      { tournamentId: "2", name: "Later Open", location: "Old Coast, Eastern District, Newland", tier: "DEVELOPMENT", entered: true, week: 8 },
    ],
  };
  const html = renderToStaticMarkup(<HubDashboard id="career-1" />);
  const travelBlock = html.split(">Travel</h3>")[1]?.split(">Next open week</h3>")[0];
  expect(travelBlock).toContain("Old Coast, Eastern District, Newland");
  expect(travelBlock).toContain("Week 8");
  expect(travelBlock?.match(/<p /g)).toHaveLength(2);
  expect(travelBlock).toContain("overflow-x-auto whitespace-nowrap");
});
