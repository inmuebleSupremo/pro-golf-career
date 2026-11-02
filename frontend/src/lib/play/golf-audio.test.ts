import { afterEach, describe, expect, it, vi } from "vitest";

import { startShotPlayback } from "./shot-playback-clock";
import { GolfAudioController, readAudioPreferences } from "./golf-audio";
import { AMBIENCE_SAMPLES, IMPACT_SAMPLES, LANDING_SAMPLES, SampleBag,
  ambienceFamily, impactCategory, landingCategory } from "./golf-audio-policy";
import type { HoleGeom } from "./hole-geometry";
import { tracePlaybackDurationMs, tracePlaybackFrame, type ShotTrace } from "./trace-presentation";

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });

function browserAudio() {
  const saved = new Map<string, string>();
  const media: FakeAudio[] = [];
  let started = 0;
  let decoded = 0;
  const gains: { value: number }[] = [];
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
    createGain() {
      const gain = { value: 1 };
      gains.push(gain);
      return { gain, connect() { }, disconnect() { } };
    }
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
  return { media, saved, started: () => started, decoded: () => decoded,
    gains: () => gains.map((gain) => gain.value) };
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
    expect(IMPACT_SAMPLES.driver).toEqual(["/audio/golf-v1/618277__yukon456__drive_11_dink.wav"]);
  });

  it("exhausts each bag and never repeats across a refill", () => {
    const bag = new SampleBag(["a", "b", "c"], () => 0);
    const picks = Array.from({ length: 12 }, () => bag.next());
    for (let i = 0; i < picks.length - 1; i++) expect(picks[i]).not.toBe(picks[i + 1]);
    for (let i = 0; i < picks.length; i += 3) expect(new Set(picks.slice(i, i + 3)).size).toBe(3);
  });

  it("keeps a single approved driver take available on every shot", () => {
    const bag = new SampleBag(IMPACT_SAMPLES.driver);
    expect(Array.from({ length: 4 }, () => bag.next())).toEqual(Array(4).fill(IMPACT_SAMPLES.driver[0]));
  });

  it("maps first contact surfaces to supplied landing variations without using the final lie", () => {
    expect(["FAIRWAY", "GREEN", "PRIMARY_ROUGH", "DEEP_ROUGH", "BUNKER", "WATER"]
      .map(landingCategory)).toEqual(["fairway", "green", "rough", "rough", "bunker", "water"]);
    expect(["FIRST_CUT", "FRINGE", "TREES", "OUT_OF_BOUNDS", undefined]
      .map(landingCategory)).toEqual([null, null, null, null, null]);
    expect(Object.values(LANDING_SAMPLES).map((samples) => samples.length)).toEqual([24, 2, 1, 3, 3]);
    for (const samples of Object.values(LANDING_SAMPLES).filter((items) => items.length > 1)) {
      const bag = new SampleBag(samples, () => 0);
      const picks = Array.from({ length: samples.length * 3 }, () => bag.next());
      for (let i = 1; i < picks.length; i++) expect(picks[i]).not.toBe(picks[i - 1]);
    }
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
  it("sounds one quiet first contact when the non-putt trace visibly lands", async () => {
    const browser = browserAudio();
    const audio = new GolfAudioController();
    audio.startScene("links");
    audio.unlock();
    await vi.waitFor(() => expect(browser.decoded()).toBe(44));
    const trace: ShotTrace = {
      club: "SEVEN_IRON", origin: { x: 0, y: 0 }, aimPoint: { x: 0, y: 100 },
      contact: { position: { x: 0, y: 100 }, surface: "WATER" },
      airbornePath: [], roll: null,
      transition: { kind: "WATER_DROP", from: { x: 0, y: 100 }, to: { x: 0, y: 85 } },
      finalPoint: { x: 0, y: 85 },
    };
    const duration = tracePlaybackDurationMs(trace);
    const clock = frames();
    const stop = startShotPlayback(duration, (progress) => {
      audio.movement(trace, progress, trace.club);
      if (tracePlaybackFrame(trace, progress).phase === "contact") {
        audio.landing(trace, trace.contact.surface, trace.club);
      }
    }, undefined, clock.request, clock.cancel);
    clock.paint(1000);
    for (let elapsed = 16; elapsed < duration; elapsed += 16) {
      clock.paint(1000 + elapsed);
      if (tracePlaybackFrame(trace, Math.min(1, elapsed / duration)).phase === "airborne") {
        expect(browser.started()).toBe(1);
      }
    }
    expect(browser.started()).toBe(2);
    expect(browser.gains().slice(-2)).toEqual([0.54, 0.18]);
    audio.landing(trace, "GREEN", trace.club); // a later lie cannot replace the first contact
    audio.landing({}, "GREEN", "PUTTER");
    expect(browser.started()).toBe(2);
    audio.setPreferences({ effects: 0.25 });
    expect(browser.gains()[0]).toBe(0.25);
    audio.setPreferences({ muted: true });
    expect(browser.gains()[0]).toBe(0);
    audio.landing({}, "FAIRWAY", "DRIVER");
    expect(browser.started()).toBe(2);
    stop();
    audio.stopScene();
  });

  it("doubles only Parkland-family ambience while retaining sliders, mute, and fades", async () => {
    vi.useFakeTimers();
    const browser = browserAudio();
    const audio = new GolfAudioController();
    audio.startScene("parkland");
    audio.unlock();
    await Promise.resolve();
    vi.advanceTimersByTime(400);
    const ambience = browser.media[0];
    expect(ambience.volume).toBeCloseTo(0.18 * 0.35 * 2);
    audio.setPreferences({ ambience: 1 });
    expect(ambience.volume).toBeCloseTo(0.36);
    audio.setFamily("links");
    vi.advanceTimersByTime(400);
    await Promise.resolve();
    vi.advanceTimersByTime(400);
    expect(ambience.volume).toBeCloseTo(0.18);
    audio.setFamily("desert");
    vi.advanceTimersByTime(400);
    await Promise.resolve();
    vi.advanceTimersByTime(400);
    expect(ambience.volume).toBeCloseTo(0.18);
    audio.setPreferences({ muted: true });
    expect(ambience.volume).toBe(0);
    audio.stopScene();
    vi.advanceTimersByTime(400);
  });

  it("waits for visible movement, ignores a failed submission, plays one impact after remount, and cups only a holed result", async () => {
    const browser = browserAudio();
    const audio = new GolfAudioController();
    audio.startScene("links");
    audio.unlock();
    // Decode happens after the user gesture; a later GraphQL result must not start sound by itself.
    await vi.waitFor(() => expect(browser.decoded()).toBe(44));
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
    await vi.waitFor(() => expect(browser.decoded()).toBe(44));
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
