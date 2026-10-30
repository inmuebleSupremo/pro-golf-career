import { afterEach, describe, expect, it, vi } from "vitest";

import { startShotPlayback } from "./shot-playback-clock";
import { GolfAudioController, readAudioPreferences } from "./golf-audio";
import { AMBIENCE_SAMPLES, SampleBag, ambienceFamily, impactCategory } from "./golf-audio-policy";
import type { HoleGeom } from "./hole-geometry";

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });

function browserAudio() {
  const saved = new Map<string, string>();
  const media: FakeAudio[] = [];
  let started = 0;
  let decoded = 0;
  class FakeAudio {
    src = "";
    loop = false;
    preload = "";
    volume = 1;
    currentTime = 0;
    paused = true;
    plays = 0;
    constructor() { media.push(this); }
    load() { }
    play() { this.plays++; this.paused = false; return Promise.resolve(); }
    pause() { this.paused = true; }
  }
  class FakeContext {
    state = "running";
    destination = {};
    createGain() { return { gain: { value: 1 }, connect() { }, disconnect() { } }; }
    createBufferSource() {
      return { buffer: null, onended: null, connect() { }, disconnect() { }, start() { started++; }, stop() { } };
    }
    decodeAudioData() { decoded++; return Promise.resolve({}); }
  }
  vi.stubGlobal("window", { location: { href: "http://localhost:3000/play" }, localStorage: {
    getItem: (key: string) => saved.get(key) ?? null,
    setItem: (key: string, value: string) => { saved.set(key, value); },
  } });
  vi.stubGlobal("Audio", FakeAudio);
  vi.stubGlobal("AudioContext", FakeContext);
  vi.stubGlobal("fetch", vi.fn(async () => ({ ok: true, arrayBuffer: async () => new ArrayBuffer(8) })));
  return { media, saved, started: () => started, decoded: () => decoded };
}

function frames() {
  let id = 0;
  const pending = new Map<number, (time: number) => void>();
  return {
    request: (frame: (time: number) => void) => { pending.set(++id, frame); return id; },
    cancel: (frameId: number) => { pending.delete(frameId); },
    paint: (time: number) => {
      const entry = pending.entries().next().value;
      if (!entry) throw new Error("No frame");
      pending.delete(entry[0]); entry[1](time);
    },
    pending,
  };
}

describe("approved golf audio selection", () => {
  it("maps authoritative club IDs and putting technique without label guesses", () => {
    expect(["DRIVER", "THREE_WOOD", "FOUR_HYBRID", "SEVEN_IRON", "GAP_WEDGE", "PUTTER"]
      .map((club) => impactCategory(club))).toEqual(["driver", "wood", "wood", "iron", "wedge", "putt"]);
    expect(impactCategory("SEVEN_IRON", "PUTT")).toBe("putt");
    expect(impactCategory("mystery club")).toBeNull();
  });

  it("exhausts each bag and never repeats across a refill", () => {
    const bag = new SampleBag(["a", "b", "c"], () => 0);
    const picks = Array.from({ length: 12 }, () => bag.next());
    for (let i = 0; i < picks.length - 1; i++) expect(picks[i]).not.toBe(picks[i + 1]);
    for (let i = 0; i < picks.length; i += 3) expect(new Set(picks.slice(i, i + 3)).size).toBe(3);
  });

  it("selects one shared golf-course track for Woodland and Heathland, wind for Links, insects for Desert", () => {
    const hole = (courseType: string) => ({ courseType }) as HoleGeom;
    expect(ambienceFamily(hole("WOODLAND"))).toBe("parkland");
    expect(ambienceFamily(hole("HEATHLAND"))).toBe("parkland");
    expect(ambienceFamily(hole("LINKS"))).toBe("links");
    expect(ambienceFamily(hole("DESERT"))).toBe("desert");
    expect(ambienceFamily(hole("TROPICAL"))).toBe("parkland");
    expect(AMBIENCE_SAMPLES.desert).toContain("trimmed");
  });

  it("reads persisted mute and volumes, clamps invalid ranges, and tolerates blocked storage", () => {
    expect(readAudioPreferences({ getItem: () => '{"muted":true,"effects":0.4,"ambience":2}' }))
      .toEqual({ muted: true, effects: 0.4, ambience: 1 });
    expect(readAudioPreferences({ getItem: () => { throw new Error("blocked"); } })).toEqual({
      muted: false, effects: 0.8, ambience: 0.35,
    });
    const browser = browserAudio();
    const audio = new GolfAudioController();
    audio.setPreferences({ muted: true, effects: 0.3, ambience: 0.2 });
    const restored = new GolfAudioController();
    restored.loadPreferences();
    expect(restored.getPreferences()).toEqual({ muted: true, effects: 0.3, ambience: 0.2 });
    expect(browser.saved.size).toBe(1);
  });
});

describe("audio on the real shot playback clock", () => {
  it("waits for visible movement, ignores a failed submission, plays one impact after remount, and cups only a holed result", async () => {
    const browser = browserAudio();
    const audio = new GolfAudioController();
    audio.startScene("links");
    audio.unlock();
    // Decode happens after the user gesture; a later GraphQL result must not start sound by itself.
    await vi.waitFor(() => expect(browser.decoded()).toBe(12));
    const shot = { club: "DRIVER" };
    const clock = frames();
    const stop = startShotPlayback(100, (progress) => audio.movement(shot, progress, shot.club),
      () => audio.cup(shot, true), clock.request, clock.cancel);
    expect(browser.started()).toBe(0); // unresolved submission / origin frame
    clock.paint(1000);
    expect(browser.started()).toBe(0);
    clock.paint(1016);
    expect(browser.started()).toBe(1);
    stop(); // cancelled playback cannot emit a delayed cup
    expect(clock.pending.size).toBe(0);
    const remount = startShotPlayback(100, (progress) => audio.movement(shot, progress, shot.club),
      () => audio.cup(shot, true), clock.request, clock.cancel);
    clock.paint(2000); clock.paint(2016); clock.paint(2116); clock.paint(2132);
    clock.paint(2148); clock.paint(2164); clock.paint(2180); clock.paint(2196);
    expect(browser.started()).toBe(2); // exactly one strike and one cup for the same resolved shot
    audio.cup({}, false);
    expect(browser.started()).toBe(2);
    remount();
    audio.stopScene();
  });

  it("keeps one ambience element across impacts, changes family, and stays silent when audio is blocked", async () => {
    vi.useFakeTimers();
    const browser = browserAudio();
    const audio = new GolfAudioController();
    audio.startScene("parkland");
    audio.unlock();
    await Promise.resolve();
    const ambience = browser.media[0];
    expect(ambience.src).toContain("ambience_golf_course_preview");
    const plays = ambience.plays;
    audio.impact({}, "SEVEN_IRON");
    audio.impact({}, "GAP_WEDGE");
    expect(browser.media).toHaveLength(1);
    expect(ambience.plays).toBe(plays);
    audio.setFamily("links");
    vi.advanceTimersByTime(400);
    expect(ambience.src).toContain("ambience_links_wind");
    audio.stopScene();
    vi.advanceTimersByTime(400);
    expect(ambience.paused).toBe(true);

    vi.stubGlobal("AudioContext", class { constructor() { throw new Error("blocked"); } });
    vi.stubGlobal("Audio", class { constructor() { throw new Error("blocked"); } });
    const blocked = new GolfAudioController();
    expect(() => { blocked.startScene("desert"); blocked.unlock(); blocked.impact({}, "DRIVER"); blocked.cup({}, true); })
      .not.toThrow();
  });

  it("plays one putter tap during physical roll and one cup cue only for a made putt", async () => {
    const browser = browserAudio();
    const audio = new GolfAudioController();
    audio.startScene("parkland");
    audio.unlock();
    await vi.waitFor(() => expect(browser.decoded()).toBe(12));
    const madePutt = { holed: true };
    audio.movement(madePutt, 0, "PUTTER", "PUTT");
    expect(browser.started()).toBe(0);
    for (const progress of [0.1, 0.5, 1]) audio.movement(madePutt, progress, "PUTTER", "PUTT");
    expect(browser.started()).toBe(1);
    audio.cup(madePutt, madePutt.holed);
    audio.cup(madePutt, madePutt.holed);
    expect(browser.started()).toBe(2);
    const missedPutt = { holed: false };
    audio.movement(missedPutt, 0.2, "PUTTER", "PUTT");
    audio.cup(missedPutt, missedPutt.holed);
    expect(browser.started()).toBe(3);
    audio.setPreferences({ muted: true });
    audio.movement({}, 0.2, "DRIVER");
    expect(browser.started()).toBe(3);
    expect(browser.media[0].paused).toBe(true);
    audio.stopScene();
  });

  it("drops missing audio without interrupting visible playback", async () => {
    browserAudio();
    vi.stubGlobal("fetch", vi.fn(async () => ({ ok: false })));
    const audio = new GolfAudioController();
    audio.startScene("links");
    audio.unlock();
    await Promise.resolve();
    expect(() => { audio.movement({}, 0.1, "DRIVER"); audio.cup({}, true); }).not.toThrow();
    audio.stopScene();
  });
});
