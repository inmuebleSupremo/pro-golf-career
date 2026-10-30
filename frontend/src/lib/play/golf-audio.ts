import { AMBIENCE_SAMPLES, CUP_SAMPLE, IMPACT_SAMPLES, SampleBag,
  impactCategory, type AmbienceFamily, type ImpactCategory } from "./golf-audio-policy";

export type AudioPreferences = { muted: boolean; effects: number; ambience: number };
export const DEFAULT_AUDIO_PREFERENCES: AudioPreferences = { muted: false, effects: 0.8, ambience: 0.35 };
const STORAGE_KEY = "progolf.audio.v1";
const EFFECT_GAIN: Record<ImpactCategory | "cup", number> = {
  driver: 0.82, wood: 0.67, iron: 0.54, wedge: 0.39, putt: 0.22, cup: 0.38,
};

function clamp(value: number): number { return Math.max(0, Math.min(1, value)); }

export function readAudioPreferences(storage: Pick<Storage, "getItem">): AudioPreferences {
  try {
    const saved = JSON.parse(storage.getItem(STORAGE_KEY) ?? "null");
    if (!saved || typeof saved !== "object") return { ...DEFAULT_AUDIO_PREFERENCES };
    return {
      muted: typeof saved.muted === "boolean" ? saved.muted : DEFAULT_AUDIO_PREFERENCES.muted,
      effects: Number.isFinite(saved.effects) ? clamp(saved.effects) : DEFAULT_AUDIO_PREFERENCES.effects,
      ambience: Number.isFinite(saved.ambience) ? clamp(saved.ambience) : DEFAULT_AUDIO_PREFERENCES.ambience,
    };
  } catch { return { ...DEFAULT_AUDIO_PREFERENCES }; }
}

/** One browser-side audio owner for the active gameplay scene. It never participates in simulation state. */
export class GolfAudioController {
  private preferences = { ...DEFAULT_AUDIO_PREFERENCES };
  private listeners = new Set<(preferences: AudioPreferences) => void>();
  private context: AudioContext | null = null;
  private master: GainNode | null = null;
  private buffers = new Map<string, AudioBuffer>();
  private downloads = new Map<string, Promise<ArrayBuffer | null>>();
  private sources = new Set<AudioBufferSourceNode>();
  private impactBag = Object.fromEntries(Object.entries(IMPACT_SAMPLES)
    .map(([category, samples]) => [category, new SampleBag(samples)])) as Record<ImpactCategory, SampleBag>;
  private playedImpacts = new WeakSet<object>();
  private playedCups = new WeakSet<object>();
  private ambience: HTMLAudioElement | null = null;
  private family: AmbienceFamily = "parkland";
  private sceneActive = false;
  private fadeTimer: ReturnType<typeof setInterval> | null = null;

  getPreferences(): AudioPreferences { return { ...this.preferences }; }

  subscribe(listener: (preferences: AudioPreferences) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  loadPreferences(): void {
    if (typeof window === "undefined") return;
    try { this.preferences = readAudioPreferences(window.localStorage); }
    catch { this.preferences = { ...DEFAULT_AUDIO_PREFERENCES }; }
    this.applyVolume();
    this.emit();
  }

  setPreferences(update: Partial<AudioPreferences>): void {
    this.preferences = {
      muted: update.muted ?? this.preferences.muted,
      effects: update.effects === undefined ? this.preferences.effects : clamp(update.effects),
      ambience: update.ambience === undefined ? this.preferences.ambience : clamp(update.ambience),
    };
    try { window.localStorage.setItem(STORAGE_KEY, JSON.stringify(this.preferences)); } catch { /* optional storage */ }
    this.applyVolume();
    if (this.preferences.muted || this.preferences.ambience === 0) this.ambience?.pause();
    this.emit();
  }

  startScene(family: AmbienceFamily): void {
    this.sceneActive = true;
    this.setFamily(family);
    if (this.context?.state === "running") this.startAmbience();
    // Fetch only short effects here. Decode after a user gesture creates/resumes the audio context.
    for (const path of [...Object.values(IMPACT_SAMPLES).flat(), CUP_SAMPLE]) this.download(path);
  }

  setFamily(family: AmbienceFamily): void {
    if (family === this.family && this.ambience) return;
    this.family = family;
    if (typeof Audio === "undefined") return;
    const change = () => {
      if (!this.sceneActive) return;
      if (!this.ambience) {
        try { this.ambience = new Audio(); }
        catch { return; }
        this.ambience.loop = true;
        this.ambience.preload = "auto";
      }
      if (this.ambience.src !== new URL(AMBIENCE_SAMPLES[family], window.location.href).href) {
        this.ambience.src = AMBIENCE_SAMPLES[family];
        this.ambience.load();
      }
      this.ambience.volume = 0;
      if (this.context?.state === "running") this.startAmbience();
    };
    if (this.ambience && !this.ambience.paused) this.fadeAmbience(0, change);
    else change();
  }

  /** Call synchronously from a Play Shot, mute, or volume interaction before any network wait. */
  unlock(): void {
    if (typeof window === "undefined") return;
    try {
      if (!this.context && typeof AudioContext !== "undefined") {
        this.context = new AudioContext();
        this.master = this.context.createGain();
        this.master.connect(this.context.destination);
        this.applyVolume();
        void this.decodeEffects();
      }
      if (this.context?.state === "suspended") void this.context.resume().catch(() => {});
      this.startAmbience();
    } catch { /* unavailable or blocked audio must not affect play */ }
  }

  impact(shot: object, club: string | null | undefined, technique?: string): void {
    if (this.playedImpacts.has(shot)) return;
    this.playedImpacts.add(shot);
    const category = impactCategory(club, technique);
    if (category) this.playEffect(this.impactBag[category].next(), EFFECT_GAIN[category]);
  }

  movement(shot: object, progress: number, club: string | null | undefined, technique?: string): void {
    if (progress > 0) this.impact(shot, club, technique);
  }

  cup(shot: object, holed: boolean): void {
    if (!holed || this.playedCups.has(shot)) return;
    this.playedCups.add(shot);
    this.playEffect(CUP_SAMPLE, EFFECT_GAIN.cup);
  }

  stopEffects(): void {
    for (const source of this.sources) {
      try { source.stop(); } catch { /* already ended */ }
    }
    this.sources.clear();
  }

  stopScene(): void {
    this.sceneActive = false;
    this.stopEffects();
    this.fadeAmbience(0, () => {
      this.ambience?.pause();
      if (this.ambience) this.ambience.currentTime = 0;
    });
  }

  private emit(): void { for (const listener of this.listeners) listener(this.getPreferences()); }

  private applyVolume(): void {
    if (this.master) this.master.gain.value = this.preferences.muted ? 0 : this.preferences.effects;
    if (this.ambience && !this.fadeTimer) this.ambience.volume = this.ambientVolume();
  }

  private ambientVolume(): number { return this.preferences.muted ? 0 : 0.18 * this.preferences.ambience; }

  private startAmbience(): void {
    if (!this.sceneActive || !this.ambience || this.preferences.muted || this.preferences.ambience === 0) return;
    try {
      void this.ambience.play().then(() => this.fadeAmbience(this.ambientVolume())).catch(() => {});
    } catch { /* autoplay or missing media */ }
  }

  private fadeAmbience(target: number, done?: () => void): void {
    if (this.fadeTimer) clearInterval(this.fadeTimer);
    this.fadeTimer = null;
    const audio = this.ambience;
    if (!audio) { done?.(); return; }
    const initial = audio.volume;
    let step = 0;
    this.fadeTimer = setInterval(() => {
      step++;
      const currentTarget = target > 0 ? this.ambientVolume() : 0;
      audio.volume = clamp(initial + (currentTarget - initial) * Math.min(1, step / 8));
      if (step >= 8) {
        if (this.fadeTimer) clearInterval(this.fadeTimer);
        this.fadeTimer = null;
        done?.();
      }
    }, 50);
  }

  private download(path: string): Promise<ArrayBuffer | null> {
    if (!this.downloads.has(path)) {
      this.downloads.set(path, fetch(path).then((response) => response.ok ? response.arrayBuffer() : null)
        .catch(() => null));
    }
    return this.downloads.get(path)!;
  }

  private async decodeEffects(): Promise<void> {
    const context = this.context;
    if (!context) return;
    await Promise.all([...Object.values(IMPACT_SAMPLES).flat(), CUP_SAMPLE].map(async (path) => {
      const data = await this.download(path);
      if (!data) return;
      try { this.buffers.set(path, await context.decodeAudioData(data.slice(0))); }
      catch { /* unsupported codec */ }
    }));
  }

  private playEffect(path: string, gain: number): void {
    const context = this.context;
    const buffer = this.buffers.get(path);
    if (!this.sceneActive || this.preferences.muted || this.preferences.effects === 0
      || !context || context.state !== "running" || !this.master || !buffer) return;
    try {
      const source = context.createBufferSource();
      const volume = context.createGain();
      source.buffer = buffer;
      volume.gain.value = gain;
      source.connect(volume);
      volume.connect(this.master);
      source.onended = () => { this.sources.delete(source); source.disconnect(); volume.disconnect(); };
      this.sources.add(source);
      source.start();
    } catch { /* audio never blocks a shot */ }
  }
}

export const golfAudio = new GolfAudioController();
