/** Pixel bounds of the full scenic viewport and the unobstructed decision area. */
export type StageViewport = Readonly<{
  width: number;
  height: number;
  safeTop: number;
  safeBottom: number;
}>;

export function scenicFrame(viewport: StageViewport | undefined, safeHeight: number) {
  if (!viewport || viewport.width <= 0 || viewport.height <= 0) {
    return { height: safeHeight, safeTop: 0, safeHeight, aspect: 1.8 };
  }
  const available = Math.max(1, viewport.height - viewport.safeTop - viewport.safeBottom);
  const scale = safeHeight / available;
  return {
    height: viewport.height * scale,
    safeTop: viewport.safeTop * scale,
    safeHeight,
    aspect: viewport.width / viewport.height,
  };
}
