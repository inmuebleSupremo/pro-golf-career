// @vitest-environment happy-dom
import { act, type ComponentProps } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { MobileCareerNavigation, Sidebar } from "./sidebar";
import { IdentityStrip, NextAction } from "./identity-strip";

const route = vi.hoisted(() => ({ pathname: "/career/test-career" }));
const seasonState = vi.hoisted(() => ({ pendingEvent: false, operation: null as null | {
  requestId: string; startingSeason: number; state: "RUNNING"; completedSeason: null;
} }));
const identityState = vi.hoisted(() => ({ populated: false }));

vi.mock("next/link", () => ({
  default: (props: ComponentProps<"a">) => <a {...props} />,
}));
vi.mock("next/navigation", () => ({ usePathname: () => route.pathname, useRouter: () => ({}) }));
vi.mock("@tanstack/react-query", () => ({ useQueryClient: () => ({ invalidateQueries: vi.fn() }) }));
vi.mock("@/lib/api/queries", () => ({
  useCareerInbox: () => ({ data: { careerInbox: { items: [{}, {}] } } }),
  useCareerOverview: () => ({ data: { world: { season: 1, hasPendingEvent: seasonState.pendingEvent },
    playerSeasonStats: identityState.populated ? [{ season: 1, wins: 0, events: 1, bestFinish: 24 }] : [] } }),
  usePlayerProfile: () => ({ data: { playerProfile: identityState.populated ? {
    firstName: "Ben", lastName: "Dover", tour: "DEVELOPMENT", archetype: "PRECISION_PLAYER",
    age: 21, worldRanking: 115, availableFunds: 30725,
  } : null } }),
}));
vi.mock("@/lib/api/play", () => ({
  useAdvanceWeek: () => ({ isPending: false }),
  useAdvanceSeason: () => ({ isPending: false }),
  useSeasonAdvanceOperation: () => ({ data: { seasonAdvanceOperation: seasonState.operation }, isPending: false, isError: false }),
  useCompleteEvent: () => ({ isPending: false }),
}));
vi.mock("@/components/loading/foreground-loading-provider", () => ({
  useForegroundOperation: () => ({ isPending: false, hasPendingForegroundOperation: false }),
  useSeasonReviewTransition: () => ({ transition: null }),
}));
vi.mock("@/components/command/career-account-menu", () => ({
  CareerAccountMenu: () => <button type="button" aria-label="Account menu">Account</button>,
}));

const destinations = [
  ["Hub", ""],
  ["Inbox", "/inbox"],
  ["Schedule", "/calendar"],
  ["Play Event", "/play"],
  ["Practice Facility", "/practice"],
  ["Development", "/manage"],
  ["Fitness", "/fitness"],
  ["Equipment", "/equipment"],
  ["Staff", "/staff"],
  ["Finances", "/finances"],
  ["Profile", "/profile"],
  ["Season Stats", "/seasons"],
  ["Career Records", "/career-records"],
  ["Achievements", "/achievements"],
  ["Hall of Fame", "/hall-of-fame"],
  ["News", "/news"],
  ["Rankings", "/rankings"],
  ["Rivals", "/rivals"],
  ["Records", "/records"],
] as const;

let host: HTMLDivElement;
let root: Root;

function render(node: React.ReactNode) {
  act(() => root.render(node));
}

function click(element: Element, x = 0, y = 0) {
  act(() =>
    element.dispatchEvent(new MouseEvent("click", { bubbles: true, clientX: x, clientY: y })),
  );
}

function navLinks(nav: Element) {
  return [...nav.querySelectorAll("a")].map((link) => [
    link.textContent?.trim().replace(/\d+$/, ""),
    link.getAttribute("href"),
  ]);
}

beforeEach(() => {
  (
    globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }
  ).IS_REACT_ACT_ENVIRONMENT = true;
  route.pathname = "/career/test-career";
  seasonState.pendingEvent = false;
  seasonState.operation = null;
  identityState.populated = false;
  window.innerWidth = 390;
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
});

afterEach(() => {
  act(() => root.unmount());
  host.remove();
  document.body.style.overflow = "";
  vi.restoreAllMocks();
});

describe("career navigation", () => {
  it("keeps the golfer identity and current career statistics in the compact header", () => {
    identityState.populated = true;
    render(<IdentityStrip id="test-career" />);
    const header = host.querySelector("header")!;
    for (const detail of ["Ben Dover", "Development", "Precision Player", "Age 21", "Season 1",
      "World Rank", "115", "Bankroll", "$30,725", "Season Wins", "Best this yr", "24th"]) {
      expect(header.textContent).toContain(detail);
    }
  });

  it("keeps a running season operation visible if an overview sees an intermediate event", () => {
    seasonState.pendingEvent = true;
    seasonState.operation = { requestId: "in-progress", startingSeason: 1, state: "RUNNING", completedSeason: null };
    render(<NextAction id="test-career" hasPendingEvent pendingEventFinished={false} hub />);
    expect(host.textContent).toContain("Check Schedule for season progression status");
    expect(host.textContent).not.toContain("Play event");
  });

  it("keeps the desktop sidebar and shows a mobile-only menu trigger", () => {
    render(
      <>
        <Sidebar id="test-career" />
        <IdentityStrip id="test-career" />
      </>,
    );
    const sidebar = host.querySelector("aside")!;
    const trigger = host.querySelector('button[aria-label="Open career navigation"]')!;
    expect(sidebar.className).toContain("hidden");
    expect(sidebar.className).toContain("lg:flex");
    expect(sidebar.className).toContain("lg:sticky");
    expect(sidebar.querySelector("nav")).not.toBeNull();
    expect(trigger.className).toContain("lg:hidden");
    expect(host.querySelector("header")?.contains(trigger)).toBe(true);
    expect(host.querySelector("header")?.contains(host.querySelector('button[aria-label="Account menu"]'))).toBe(true);
    expect(trigger.closest("header")?.querySelector(".truncate")).not.toBeNull();
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
    expect(host.querySelector("dialog")?.hasAttribute("open")).toBe(false);
  });

  it("keeps the compact mobile identity controls without global simulation actions", () => {
    render(<IdentityStrip id="test-career" />);
    const header = host.querySelector("header")!;
    const identityRow = header.children[1];
    const navigation = host.querySelector('button[aria-label="Open career navigation"]')!;
    const account = host.querySelector('button[aria-label="Account menu"]')!;

    expect(header.className).toContain("lg:h-[4.5rem]");
    expect(identityRow.className).toContain("min-w-0");
    expect(identityRow.contains(navigation)).toBe(true);
    expect(header.contains(account)).toBe(true);
    expect(header.textContent).not.toContain("Sim season");
    expect(header.textContent).not.toContain("Advance week");
  });

  it("uses the same destinations, groups, active route and inbox badge in both surfaces", () => {
    route.pathname = "/career/test-career/staff/manage";
    render(
      <>
        <Sidebar id="test-career" />
        <MobileCareerNavigation id="test-career" />
      </>,
    );
    const navs = host.querySelectorAll('nav[aria-label="Career navigation"]');
    const expected = destinations.map(([label, path]) => [label, `/career/test-career${path}`]);
    expect(navs).toHaveLength(2);
    for (const nav of navs) {
      expect(navLinks(nav)).toEqual(expected);
      expect([...nav.querySelectorAll("p")].map((p) => p.textContent?.trim())).toEqual([
        "Compete",
        "Manage",
        "Career",
        "World",
      ]);
      expect(
        nav.querySelector('a[href="/career/test-career/staff"]')?.getAttribute("aria-current"),
      ).toBe("page");
      expect(nav.querySelector('a[href="/career/test-career"]')?.hasAttribute("aria-current")).toBe(
        false,
      );
      expect(nav.querySelector('a[href="/career/test-career/inbox"]')?.textContent).toContain("2");
    }
    expect(host.querySelector('a[href="/saves"]')).toBeNull();
    expect(host.textContent).not.toContain("Sign out");
  });

  it("opens as a modal, focuses its close control and restores focus and scrolling on close", () => {
    render(<MobileCareerNavigation id="test-career" />);
    const trigger = host.querySelector('button[aria-label="Open career navigation"]')!;
    click(trigger);
    const dialog = host.querySelector("dialog")!;
    expect(dialog.open).toBe(true);
    expect(trigger.getAttribute("aria-expanded")).toBe("true");
    expect(document.body.style.overflow).toBe("hidden");
    expect(document.activeElement).toBe(
      host.querySelector('button[aria-label="Close career navigation"]'),
    );
    click(host.querySelector('button[aria-label="Close career navigation"]')!);
    expect(dialog.open).toBe(false);
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
    expect(document.body.style.overflow).toBe("");
    expect(document.activeElement).toBe(trigger);
  });

  it("closes on destination selection, Escape and outside interaction", () => {
    render(<MobileCareerNavigation id="test-career" />);
    const trigger = host.querySelector('button[aria-label="Open career navigation"]')!;
    const dialog = host.querySelector("dialog")!;
    click(trigger);
    click(dialog.querySelector('a[href="/career/test-career/calendar"]')!);
    expect(dialog.open).toBe(false);

    click(trigger);
    act(() => dialog.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape", bubbles: true })));
    expect(dialog.open).toBe(false);
    expect(document.activeElement).toBe(trigger);

    click(trigger);
    click(dialog, 1000, 1000);
    expect(dialog.open).toBe(false);
  });
});
