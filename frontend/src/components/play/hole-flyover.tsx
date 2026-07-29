"use client";

import { useEffect, useState } from "react";
import Image from "next/image";

import { cn } from "@/lib/utils";
import type { SceneBackdrop } from "@/lib/play/scene";

export interface HoleFlyoverProps {
  readonly holeNumber: number;
  readonly par: number;
  readonly length: number;
  readonly backdrop: SceneBackdrop;
}

/**
 * The broadcast-style per-hole intro (spec: web-hole-visualization): the event's scene photograph with the
 * hole's identity, shown briefly when the player reaches a new hole and then receding to reveal the persistent
 * 2D schematic beneath. Overlays the hole stage; auto-dismisses, or dismiss on click. Under reduced motion it
 * still appears but fades instantly.
 */
export function HoleFlyover({ holeNumber, par, length, backdrop }: HoleFlyoverProps) {
  const [show, setShow] = useState(true);
  // Re-show on each new hole (render-phase reset), then auto-dismiss via the timer below.
  const [shownHole, setShownHole] = useState(holeNumber);
  if (holeNumber !== shownHole) {
    setShownHole(holeNumber);
    setShow(true);
  }

  useEffect(() => {
    const timer = setTimeout(() => setShow(false), 1600);
    return () => clearTimeout(timer);
  }, [holeNumber]);

  return (
    <button
      type="button"
      aria-label={`Hole ${holeNumber}, par ${par}, ${length} yards. Tap to start.`}
      onClick={() => setShow(false)}
      tabIndex={show ? 0 : -1}
      className={cn(
        "absolute inset-0 isolate flex flex-col justify-end overflow-hidden text-left transition-opacity duration-500 motion-reduce:duration-0",
        show ? "opacity-100" : "pointer-events-none opacity-0",
      )}
    >
      <Image src={backdrop.src} alt="" fill sizes="(min-width: 1024px) 640px, 100vw" className="object-cover object-center" />
      <div className="from-background via-background/40 absolute inset-0 bg-gradient-to-t to-transparent" />
      <div className="relative flex items-baseline gap-3 p-5">
        <span className="text-foreground font-serif text-3xl font-medium tracking-[-0.01em]">Hole {holeNumber}</span>
        <span className="text-foreground/80 font-mono text-sm tabular-nums">
          Par {par} · {length}y
        </span>
      </div>
    </button>
  );
}
