/** Short enough to acknowledge an unusual wait without interrupting routine work. */
export const FOREGROUND_LOADING_DELAY_MS = 1_200;

export type ForegroundPresentationPolicy = "immediate" | "delayed" | "none";

export type ForegroundOperationOptions = {
  readonly label: string;
  readonly presentation: ForegroundPresentationPolicy;
  /** Optional only for deliberately enrolled uncertain work. */
  readonly delayMs?: number;
};

export type ForegroundOperationSnapshot = {
  readonly activeCount: number;
  readonly visibleOperations: readonly { id: string; label: string }[];
};

type Operation = {
  readonly id: string;
  readonly label: string;
  timer: ReturnType<typeof setTimeout> | null;
  visible: boolean;
};

type Listener = () => void;

/**
 * Tracks genuine foreground work independently from its presentation. Each start call
 * receives its own completion callback, so one completed request cannot hide another
 * request that is still running.
 */
export class DelayedOperationRegistry {
  private readonly operations = new Map<string, Operation>();
  private readonly listeners = new Set<Listener>();
  private nextId = 0;

  start({ label, presentation, delayMs }: ForegroundOperationOptions): () => void {
    const id = `foreground-operation-${this.nextId++}`;
    const operation: Operation = { id, label, timer: null, visible: false };

    if (presentation === "immediate") {
      operation.visible = true;
    } else if (presentation === "delayed") {
      operation.timer = setTimeout(() => {
        const active = this.operations.get(id);
        if (!active) return;
        active.timer = null;
        active.visible = true;
        this.emit();
      }, delayMs ?? FOREGROUND_LOADING_DELAY_MS);
    }

    this.operations.set(id, operation);
    this.emit();

    let finished = false;
    return () => {
      if (finished) return;
      finished = true;
      this.finish(id);
    };
  }

  snapshot(): ForegroundOperationSnapshot {
    return {
      activeCount: this.operations.size,
      visibleOperations: [...this.operations.values()]
        .filter((operation) => operation.visible)
        .map(({ id, label }) => ({ id, label })),
    };
  }

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  dispose(): void {
    for (const operation of this.operations.values()) {
      if (operation.timer) clearTimeout(operation.timer);
    }
    this.operations.clear();
    this.emit();
    this.listeners.clear();
  }

  private finish(id: string): void {
    const operation = this.operations.get(id);
    if (!operation) return;
    if (operation.timer) clearTimeout(operation.timer);
    this.operations.delete(id);
    this.emit();
  }

  private emit(): void {
    for (const listener of this.listeners) listener();
  }
}

/** Guarantees cleanup for both fulfilled and rejected foreground work. */
export async function runTrackedOperation<T>(
  registry: DelayedOperationRegistry,
  options: ForegroundOperationOptions,
  work: () => Promise<T>,
): Promise<T> {
  const finish = registry.start(options);
  try {
    return await work();
  } finally {
    finish();
  }
}

/** A synchronous gate for click handlers, before React has rendered their pending state. */
export class SingleFlight {
  private active = false;

  tryStart(): boolean {
    if (this.active) return false;
    this.active = true;
    return true;
  }

  finish(): void {
    this.active = false;
  }
}

/** The loading mark is intentionally still under a reduced-motion preference. */
export function shouldAnimateGolfLoading(reducedMotion: boolean | null): boolean {
  return !reducedMotion;
}
