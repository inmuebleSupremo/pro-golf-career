"use client";

import { useEffect, useState } from "react";
import { Volume2, VolumeX } from "lucide-react";

import { golfAudio } from "@/lib/play/golf-audio";

/** Small gameplay-only control; opening it is also a valid browser audio gesture. */
export function AudioControls() {
  const [settings, setSettings] = useState(() => golfAudio.getPreferences());
  useEffect(() => {
    const unsubscribe = golfAudio.subscribe(setSettings);
    golfAudio.loadPreferences();
    return unsubscribe;
  }, []);

  return <details className="group relative shrink-0">
    <summary onClick={() => golfAudio.unlock()} aria-label="Audio settings"
      className="border-border text-muted-foreground hover:text-foreground flex cursor-pointer list-none items-center rounded-md border px-2.5 py-1 focus-visible:outline-2 focus-visible:outline-primary [&::-webkit-details-marker]:hidden">
      {settings.muted ? <VolumeX className="size-4" aria-hidden="true" /> : <Volume2 className="size-4" aria-hidden="true" />}
      <span className="sr-only">Audio settings</span>
    </summary>
    <div className="border-border bg-surface text-foreground absolute top-full right-0 z-50 mt-2 w-56 rounded-lg border p-4 shadow-lg">
      <button type="button" aria-pressed={settings.muted}
        className="border-border hover:bg-surface-3 mb-4 w-full rounded-md border px-3 py-2 text-left text-sm"
        onClick={() => { golfAudio.setPreferences({ muted: !settings.muted }); golfAudio.unlock(); }}>
        {settings.muted ? "Unmute audio" : "Mute audio"}
      </button>
      <label className="mb-3 block text-xs">
        <span className="mb-1 flex justify-between"><span>Sound effects</span><span>{Math.round(settings.effects * 100)}%</span></span>
        <input type="range" min="0" max="1" step="0.05" value={settings.effects}
          onChange={(event) => { golfAudio.setPreferences({ effects: Number(event.target.value) }); golfAudio.unlock(); }}
          className="w-full accent-primary" />
      </label>
      <label className="block text-xs">
        <span className="mb-1 flex justify-between"><span>Ambience</span><span>{Math.round(settings.ambience * 100)}%</span></span>
        <input type="range" min="0" max="1" step="0.05" value={settings.ambience}
          onChange={(event) => { golfAudio.setPreferences({ ambience: Number(event.target.value) }); golfAudio.unlock(); }}
          className="w-full accent-primary" />
      </label>
    </div>
  </details>;
}
