// @vitest-environment happy-dom
import { act, type ComponentProps } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { MobileCareerNavigation, Sidebar } from "./sidebar";
import { IdentityStrip } from "./identity-strip";

const route = vi.hoisted(() => ({ pathname: "/career/test-career" }));

vi.mock("next/link", () => ({
  default: (props: ComponentProps<"a">) => <a {...props} />,
}));
vi.mock("next/navigation", () => ({ usePathname: () => route.pathname, useRouter: () => ({}) }));
vi.mock("@/lib/api/queries", () => ({
  useCareerInbox: () => ({ data: { careerInbox: { items: [{}, {}] } } }),
  useCareerOverview: () => ({ data: { world: { season: 1, hasPendingEvent: false } } }),
  usePlayerProfile: () => ({ data: { playerProfile: null } }),
}));
vi.mock("@/lib/api/play", () => ({
  useAdvanceWeek: () => ({ isPending: false }),
  useAdvanceSeason: () => ({ isPending: false }),
  useCompleteEvent: () => ({ isPending: false }),
}));
vi.mock("@/components/loading/foreground-loading-provider", () => ({
  useForegroundOperation: () => ({ isPending: false, hasPendingForegroundOperation: false }),
}));
vi.mock("@/components/command/career-account-menu", () => ({
  CareerAccountMenu: () => <button type="button" aria-label="Account menu" />,
}));

const destinations = [
  ["Hub", ""],
  ["Inbox", "/inbox"],
  ["Schedule", "/calendar"],
  ["Play Event", "/play"],
  ["Practice Facility", "/practice"],
  ["Profile", "/profile"],
  ["Season Stats", "/seasons"],
  ["Career Records", "/career-records"],
  ["Achievements", "/achievements"],
  ["Hall of Fame", "/hall-of-fame"],
  ["Development", "/manage"],
  ["Equipment", "/equipment"],
  ["Staff", "/staff"],
  ["Finances", "/finances"],
  ["Fitness", "/fitness"],
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
        "Career",
        "Manage",
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
