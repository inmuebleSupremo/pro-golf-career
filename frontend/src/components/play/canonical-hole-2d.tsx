"use client";

import { useEffect, useLayoutEffect, useMemo, useRef, useState, type PointerEvent } from "react";

import { BIOME_KITS, LINKS_MATERIAL, PARKLAND_MATERIAL, resolveBiome, VEG_SYMBOLS, type SvgPrim } from "@/lib/play/biomes";
import { canonicalRenderModel, pointerWorldTarget } from "@/lib/play/canonical-geometry";
import { decorationId, planTreeCanopies, type CanopyPlacement } from "@/lib/play/canonical-hole-decoration";
import type { Point } from "@/lib/play/hole-geometry";
import { feedbackAimPoint, isAimPointInteractionLocked, tracePlaybackDurationMs, tracePlaybackFrame, tracePlaybackStatus, transitionLabel } from "@/lib/play/trace-presentation";
import type { Hole2dProps } from "@/components/play/hole-2d";
import { WindDisplay } from "@/components/play/wind-display";
import { PremiumParklandBackdrop, PremiumParklandCanopies, PremiumParklandDefs, PremiumParklandLight, PremiumParklandPaths, PremiumParklandProperty, PremiumParklandShadows, PremiumParklandSurface } from "@/components/play/premium-parkland-art";
import { PremiumLinksAtmosphere, PremiumLinksDefs, PremiumLinksLandscape, PremiumLinksProperty, PremiumLinksSurface } from "@/components/play/premium-links-art";
import { PremiumHeathlandAtmosphere, PremiumHeathlandCanonicalPines, PremiumHeathlandDefs, PremiumHeathlandLandscape, PremiumHeathlandProperty, PremiumHeathlandSurface, planHeathlandDecoration } from "@/components/play/premium-heathland-art";
import { PremiumDesertAtmosphere, PremiumDesertBunkerMask, PremiumDesertCanonicalTrees, PremiumDesertDefs, PremiumDesertLandscape, PremiumDesertProperty, PremiumDesertSurface, planDesertDecoration } from "@/components/play/premium-desert-art";
import { premiumLinksCorridor } from "@/lib/play/premium-links-corridor";
import { premiumLinksPropertyPlan } from "@/lib/play/premium-links-property";
import { premiumHeathlandCorridor } from "@/lib/play/premium-heathland-corridor";
import { premiumHeathlandPropertyPlan } from "@/lib/play/premium-heathland-property";
import { premiumDesertCorridor } from "@/lib/play/premium-desert-corridor";
import { premiumDesertPropertyPlan } from "@/lib/play/premium-desert-property";
import { premiumDistantTrees, premiumSceneryWidth, premiumSurfacePrecedence, premiumTreePlans } from "@/lib/play/premium-parkland-decoration";
import { isParklandFamily, isWoodlandVariation } from "@/lib/play/play-presentation";
import { scenicFrame } from "@/lib/play/stage-viewport";
import { inPropertyClearing, isPremiumAimCoordinate, isPropertyPathCoordinate, premiumPropertyPlan } from "@/lib/play/premium-parkland-property";

const W = 220;
const H = 440;
const PAD = 16;
const PARKLAND_SCENERY_MARGIN = 105;

/** Fit short canonical holes without changing the projection or aim-point inverse. */
function canonicalViewportHeight(geometry: Hole2dProps["hole"]["geometry"]): number {
  if (!geometry) return H;
  const points = [...geometry.playableBoundary, ...geometry.regions.flatMap((region) => region.boundary)];
  const xs = points.map((point) => point.x);
  const ys = points.map((point) => point.y);
  const width = Math.max(1, Math.max(...xs) - Math.min(...xs));
  const height = Math.max(...ys) - Math.min(...ys);
  return Math.min(H, Math.max(260, Math.ceil(height * (W - PAD * 2) / width + PAD * 2)));
}

const fills = (surface: string, kit: (typeof BIOME_KITS)[keyof typeof BIOME_KITS], artId: string) => {
  if (kit.biome === "parkland") {
    switch (surface) {
      case "FAIRWAY": return `url(#${artId}-fairway)`;
      case "FIRST_CUT": case "FRINGE": return `url(#${artId}-fringe)`;
      case "PRIMARY_ROUGH": case "DEEP_ROUGH": case "RECOVERY_AREA": return `url(#${artId}-rough)`;
      case "GREEN": return `url(#${artId}-green)`;
      case "BUNKER": case "WASTE_AREA": return `url(#${artId}-sand)`;
      case "WATER": return `url(#${artId}-water)`;
      case "TREES": return `url(#${artId}-trees)`;
      default: return "transparent";
    }
  }
  if (kit.biome === "links") {
    switch (surface) {
      case "FAIRWAY": return `url(#${artId}-links-fairway)`;
      case "FIRST_CUT": case "FRINGE": return `url(#${artId}-links-fringe)`;
      case "PRIMARY_ROUGH": return `url(#${artId}-links-fescue)`;
      case "DEEP_ROUGH": return `url(#${artId}-links-deep-fescue)`;
      case "RECOVERY_AREA": return LINKS_MATERIAL.recovery;
      case "GREEN": return `url(#${artId}-links-green)`;
      case "BUNKER": case "WASTE_AREA": return `url(#${artId}-links-sand)`;
      case "WATER": return `url(#${artId}-links-water)`;
      case "TREES": return `url(#${artId}-links-thicket)`;
      default: return "transparent";
    }
  }
  switch (surface) {
    case "FAIRWAY": return kit.fairway;
    case "FIRST_CUT": return kit.fringe;
    case "PRIMARY_ROUGH": return kit.rough;
    case "DEEP_ROUGH": return kit.out;
    case "GREEN": return kit.green;
    case "FRINGE": return kit.fringe;
    case "BUNKER": case "WASTE_AREA": return kit.sand;
    case "WATER": return kit.water ?? "#2f7fb5";
    case "TREES": return "#254d2b";
    case "RECOVERY_AREA": return kit.rough;
    default: return "transparent";
  }
};

function usePrefersReducedMotion(): boolean {
  const [reduced, setReduced] = useState(false);
  useLayoutEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReduced(query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);
  return reduced;
}

/** Canonical renderer: terrain and every visible shot point come from backend yard-space facts. */
export function CanonicalHole2d({ hole, ball, className, aimPoint, onAimPoint, presentation = "polished", previewAspect,
  unlitAudit = false, stageViewport }: Hole2dProps) {
  const geometry = hole.geometry!;
  const kit = BIOME_KITS[resolveBiome(hole.courseType)];
  const premiumParkland = isParklandFamily(hole)
    && (presentation === "premium" || presentation === "premium-v1");
  const premiumLinks = kit.biome === "links" && presentation === "premium"
    && hole.courseGeneratorVersion === 8
    && hole.canonicalGeometrySource === "GENERATOR_8_PRINCIPLED_ARCHITECTURE";
  const premiumHeathland = kit.biome === "heathland" && hole.courseFamily === "HEATHLAND"
    && hole.canonicalBiome === "HEATHLAND" && presentation === "premium"
    && hole.courseGeneratorVersion === 8
    && hole.canonicalGeometrySource === "GENERATOR_8_PRINCIPLED_ARCHITECTURE";
  const premiumDesert = kit.biome === "desert" && hole.courseFamily === "DESERT"
    && hole.canonicalBiome === "DESERT" && presentation === "premium"
    && hole.courseGeneratorVersion === 8
    && hole.canonicalGeometrySource === "GENERATOR_8_PRINCIPLED_ARCHITECTURE"
    && geometry.regions.some((region) => region.surface === "GREEN" && region.boundary.length >= 3);
  const variation = isWoodlandVariation(hole) ? "woodland" : "open";
  const fullStage = premiumLinks || premiumHeathland || premiumDesert || premiumParkland && presentation === "premium";
  const safeHeight = useMemo(() => canonicalViewportHeight(geometry), [geometry]);
  const frame = scenicFrame(fullStage ? stageViewport : undefined, safeHeight);
  const viewHeight = frame.height;
  const model = useMemo(() => canonicalRenderModel(geometry, { width: W, height: viewHeight, padding: PAD,
    safeTop: frame.safeTop, safeHeight: frame.safeHeight, center: premiumParkland || premiumLinks || premiumHeathland || premiumDesert }),
    [geometry, viewHeight, frame.safeTop, frame.safeHeight, premiumParkland, premiumLinks, premiumHeathland, premiumDesert]);
  const artId = useMemo(() => `${decorationId(hole.layoutSeed, geometry)}${fullStage ? "-v2" : ""}`,
    [geometry, hole.layoutSeed, fullStage]);
  const treeCanopies = useMemo(() => planTreeCanopies(geometry, hole.layoutSeed), [geometry, hole.layoutSeed]);
  const [measuredAspect, setMeasuredAspect] = useState(previewAspect ?? 1.8);
  const stageAspect = stageViewport && fullStage ? frame.aspect : measuredAspect;
  const v1SceneryX = -PARKLAND_SCENERY_MARGIN;
  const v1SceneryWidth = W + PARKLAND_SCENERY_MARGIN * 2;
  // The original crowns span this full width. A smaller V2 viewBox exposes unlit side strips on narrow stages.
  const sceneryWidth = premiumLinks || premiumHeathland || premiumDesert ? Math.max(W, viewHeight * stageAspect)
    : fullStage ? premiumSceneryWidth(viewHeight, stageAspect, v1SceneryWidth) : v1SceneryWidth;
  const sceneryX = (W - sceneryWidth) / 2;
  const visibleWidth = fullStage && stageViewport ? viewHeight * stageAspect : sceneryWidth;
  const visibleX = (W - visibleWidth) / 2;
  const premiumTrees = useMemo(() => premiumParkland ? premiumTreePlans(geometry, hole.layoutSeed, model,
    fullStage || variation === "woodland" ? variation : "legacy") : [],
  [premiumParkland, geometry, hole.layoutSeed, model, variation, fullStage]);
  const originalTrees = useMemo(() => premiumParkland
    ? premiumDistantTrees(geometry, hole.layoutSeed, model, v1SceneryX, v1SceneryWidth, viewHeight,
      variation === "woodland" ? 570 : fullStage ? 260 : 470,
      fullStage || variation === "woodland" ? variation : "legacy") : [],
  [premiumParkland, geometry, hole.layoutSeed, model, v1SceneryX, v1SceneryWidth, viewHeight, variation, fullStage]);
  const protectedTrees = useMemo(() => [...originalTrees, ...premiumTrees.flatMap((plan) => plan.trees)],
    [originalTrees, premiumTrees]);
  const property = useMemo(() => premiumDesert
    ? premiumDesertPropertyPlan(geometry, model, hole.layoutSeed, hole.par, hole.holeNumber, sceneryX, sceneryWidth, viewHeight)
    : premiumHeathland
    ? premiumHeathlandPropertyPlan(geometry, model, hole.layoutSeed, hole.par, hole.holeNumber, sceneryX, sceneryWidth, viewHeight)
    : premiumLinks
    ? premiumLinksPropertyPlan(geometry, model, hole.layoutSeed, hole.par, hole.holeNumber, sceneryX, sceneryWidth, viewHeight)
    : fullStage
    ? premiumPropertyPlan(geometry, model, hole.layoutSeed, hole.par, hole.holeNumber, sceneryX, sceneryWidth,
      viewHeight, protectedTrees, v1SceneryX, v1SceneryX + v1SceneryWidth)
    : null, [fullStage, premiumLinks, premiumHeathland, premiumDesert, geometry, model, hole.layoutSeed, hole.par, hole.holeNumber, sceneryX, sceneryWidth, viewHeight,
      protectedTrees, v1SceneryX, v1SceneryWidth]);
  const distantTrees = useMemo(() => {
    if (!premiumParkland) return [];
    if (!fullStage || sceneryWidth <= v1SceneryWidth) return originalTrees;
    const widerTrees = premiumDistantTrees(geometry, hole.layoutSeed, model, sceneryX, sceneryWidth, viewHeight,
      variation === "woodland"
        ? Math.min(620, Math.round(570 + Math.max(0, sceneryWidth - v1SceneryWidth) * .07))
        : Math.min(320, Math.round(260 + Math.max(0, sceneryWidth - v1SceneryWidth) * .07)), variation);
    const outerTrees = widerTrees.filter((tree) => (tree.x + tree.size * 1.3 + 2 < v1SceneryX
      || tree.x - tree.size * 1.3 - 2 > v1SceneryX + v1SceneryWidth)
      && !property?.glimpses.some((glimpse) => inPropertyClearing(tree.x, tree.y, tree.size, glimpse)));
    return [...originalTrees, ...outerTrees];
  }, [premiumParkland, geometry, hole.layoutSeed, model, sceneryX, sceneryWidth, viewHeight, fullStage, property,
    originalTrees, v1SceneryX, v1SceneryWidth, variation]);
  const premiumRegions = useMemo(() => premiumParkland ? [...model.terrain].sort((a, b) =>
    premiumSurfacePrecedence(b.surface) - premiumSurfacePrecedence(a.surface)) : [], [premiumParkland, model]);
  const linksRegions = useMemo(() => premiumLinks ? [...model.terrain].sort((a, b) =>
    premiumSurfacePrecedence(b.surface) - premiumSurfacePrecedence(a.surface)) : [], [premiumLinks, model]);
  const linksCorridor = useMemo(() => premiumLinks ? premiumLinksCorridor(geometry, model, viewHeight) : null,
    [premiumLinks, geometry, model, viewHeight]);
  const heathlandRegions = useMemo(() => premiumHeathland ? [...model.terrain].sort((a, b) =>
    premiumSurfacePrecedence(b.surface) - premiumSurfacePrecedence(a.surface)) : [], [premiumHeathland, model]);
  const heathlandCorridor = useMemo(() => premiumHeathland ? premiumHeathlandCorridor(geometry, model, viewHeight) : null,
    [premiumHeathland, geometry, model, viewHeight]);
  const heathlandVegetation = useMemo(() => premiumHeathland
    ? planHeathlandDecoration(geometry, model, heathlandCorridor, property, hole.layoutSeed,
      sceneryX, sceneryWidth, viewHeight) : [],
  [premiumHeathland, geometry, model, heathlandCorridor, property, hole.layoutSeed, sceneryX, sceneryWidth, viewHeight]);
  const desertRegions = useMemo(() => premiumDesert ? [...model.terrain].sort((a, b) =>
    premiumSurfacePrecedence(b.surface) - premiumSurfacePrecedence(a.surface)) : [], [premiumDesert, model]);
  const desertCorridor = useMemo(() => premiumDesert ? premiumDesertCorridor(geometry, model, viewHeight) : null,
    [premiumDesert, geometry, model, viewHeight]);
  const desertDecoration = useMemo(() => premiumDesert
    ? planDesertDecoration(geometry, model, desertCorridor, property, hole.layoutSeed,
      sceneryX, sceneryWidth, viewHeight) : [],
  [premiumDesert, geometry, model, desertCorridor, property, hole.layoutSeed, sceneryX, sceneryWidth, viewHeight]);
  const desertBunkers = useMemo(() => premiumDesert ? [
    ...model.terrain.filter((region) => region.surface === "BUNKER").map((region) => region.path),
    ...(property?.glimpses.flatMap((glimpse) => glimpse.regions.filter((region) => region.surface === "BUNKER")
      .map((region) => region.path)) ?? [])] : [], [premiumDesert, model, property]);
  const svgRef = useRef<SVGSVGElement>(null);
  useLayoutEffect(() => {
    if (!fullStage || !svgRef.current) return;
    const svg = svgRef.current;
    const update = () => {
      const { width, height } = svg.getBoundingClientRect();
      if (width > 0 && height > 0) setMeasuredAspect((current) => Math.abs(current - width / height) > .01 ? width / height : current);
    };
    update();
    window.addEventListener("resize", update);
    const observer = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(update);
    observer?.observe(svg);
    return () => {
      window.removeEventListener("resize", update);
      observer?.disconnect();
    };
  }, [fullStage]);
  const reduced = usePrefersReducedMotion();
  const trace = ball?.trace ?? null;
  const polishedParkland = kit.biome === "parkland" && presentation === "polished";
  const polishedLinks = kit.biome === "links" && presentation === "polished";
  const [progress, setProgress] = useState(1);

  useEffect(() => {
    if (!trace || reduced) {
      const frame = requestAnimationFrame(() => setProgress(1));
      return () => cancelAnimationFrame(frame);
    }
    let frame = 0;
    const start = (started: number) => {
      setProgress(0);
      const advance = (now: number) => {
      const next = Math.min(1, (now - started) / tracePlaybackDurationMs(trace));
      setProgress(next);
      if (next < 1) frame = requestAnimationFrame(advance);
      };
      frame = requestAnimationFrame(advance);
    };
    frame = requestAnimationFrame(start);
    return () => cancelAnimationFrame(frame);
  }, [trace, reduced]);

  const tee = model.project(geometry.tee);
  const cup = model.project(geometry.cup);
  const displayedAim = feedbackAimPoint(trace, aimPoint);
  const planningTarget = !trace && displayedAim ? model.project(displayedAim) : null;
  const traceTarget = trace ? model.project(displayedAim!) : null;
  const contact = trace ? model.project(trace.contact.position) : null;
  const finalPoint = trace ? model.project(trace.finalPoint) : model.project(hole.ball?.position ?? geometry.tee);
  const transitionFrom = trace?.transition ? model.project(trace.transition.from) : null;
  const transitionTo = trace?.transition ? model.project(trace.transition.to) : null;
  const rollFrom = trace?.roll ? model.project(trace.roll.from) : null;
  const rollTo = trace?.roll ? model.project(trace.roll.to) : null;
  const playback = trace ? tracePlaybackFrame(trace, progress) : null;
  const movingBall = playback && !playback.finalVisible ? model.project(playback.point) : null;
  const contactEffect = trace?.contact.surface === "WATER" ? (polishedParkland || premiumParkland ? PARKLAND_MATERIAL.waterLight : "#dff1ff")
    : trace?.contact.surface === "BUNKER" ? kit.sandStroke : "#ffffff";
  const ballStroke = polishedParkland || premiumParkland ? PARKLAND_MATERIAL.markerOutline : "#c99";
  const airbornePath = trace && contact
    ? (trace.airbornePath?.length ?? 0) > 0
      ? `M${trace.airbornePath!.map((point) => `${model.project(point.position).x} ${model.project(point.position).y}`).join(" L")}`
      : `M${model.project(trace.origin).x} ${model.project(trace.origin).y} L${contact.x} ${contact.y}`
    : null;
  const finalMatchesContact = trace != null && trace.finalPoint.x === trace.contact.position.x
    && trace.finalPoint.y === trace.contact.position.y;

  function selectTarget(event: PointerEvent<SVGSVGElement>) {
    if (!onAimPoint || !svgRef.current || isAimPointInteractionLocked(trace != null)) return;
    const transform = svgRef.current.getScreenCTM();
    if (!transform) return;
    const local = new DOMPoint(event.clientX, event.clientY).matrixTransform(transform.inverse());
    if (fullStage && !isPremiumAimCoordinate(local.x, local.y, viewHeight)) return;
    if (property && isPropertyPathCoordinate(local.x, local.y, property.paths)) return;
    onAimPoint(pointerWorldTarget({ x: event.clientX, y: event.clientY }, transform.inverse(), model.unproject));
  }

  const recoveryLabel = transitionLabel(trace);
  const playbackStatus = tracePlaybackStatus(trace, playback);
  // The accepted terrain polygons end at the tee. In a full-height scene that cap can sit
  // above the floating dock, so blend only the terrain into the continuous backdrop.
  const teeTerrainMask = fullStage && stageViewport ? `url(#${artId}-tee-terrain-mask)` : undefined;
  return <svg ref={svgRef} viewBox={`${fullStage || premiumParkland ? visibleX : 0} 0 ${fullStage || premiumParkland ? visibleWidth : W} ${viewHeight}`} className={className} role="img"
    style={premiumLinks || premiumHeathland || premiumDesert ? { overflow: "hidden", backgroundColor: premiumDesert ? "#aa8357" : premiumHeathland ? "#728257" : "#b5a863" } : undefined}
    aria-label={`Hole ${hole.holeNumber}, par ${hole.par}, canonical terrain${trace ? `, ${trace.club} shot result` : onAimPoint ? ", click or tap to aim" : ""}`}
    onPointerDown={selectTarget}>
    {polishedParkland ? <ParklandDefs artId={artId} model={model} treeCanopies={treeCanopies} /> : null}
    {premiumParkland ? <PremiumParklandDefs artId={artId} coreX={v1SceneryX} coreWidth={v1SceneryWidth}
      height={viewHeight} fullStage={fullStage} /> : null}
    {premiumLinks ? <PremiumLinksDefs id={artId} left={sceneryX} width={sceneryWidth} height={viewHeight} /> : null}
    {premiumHeathland ? <PremiumHeathlandDefs id={artId} /> : null}
    {premiumDesert ? <PremiumDesertDefs id={artId} /> : null}
    {premiumDesert ? <PremiumDesertBunkerMask id={artId} left={sceneryX} width={sceneryWidth}
      height={viewHeight} bunkers={desertBunkers} /> : null}
    {teeTerrainMask ? <defs>
      <linearGradient id={`${artId}-tee-terrain-fade`} gradientUnits="userSpaceOnUse"
        x1="0" y1={tee.y - 38} x2="0" y2={tee.y + 1}>
        <stop stopColor="white" /><stop offset="1" stopColor="black" />
      </linearGradient>
      <mask id={`${artId}-tee-terrain-mask`} maskUnits="userSpaceOnUse" x={sceneryX} y="0"
        width={sceneryWidth} height={viewHeight}>
        <rect x={sceneryX} width={sceneryWidth} height={viewHeight}
          fill={`url(#${artId}-tee-terrain-fade)`} />
      </mask>
    </defs> : null}
    {premiumLinks && linksCorridor ? <defs>
      <linearGradient id={`${artId}-links-terrain-fade`} gradientUnits="userSpaceOnUse" x1="0"
        y1={linksCorridor.greenBounds.maxY + 12} x2="0" y2={linksCorridor.greenBounds.maxY + 67}>
        <stop stopColor="black" /><stop offset="1" stopColor="white" />
      </linearGradient>
      <mask id={`${artId}-links-terrain-mask`} maskUnits="userSpaceOnUse" x={sceneryX} y="0"
        width={sceneryWidth} height={viewHeight}>
        <rect x={sceneryX} width={sceneryWidth} height={viewHeight} fill={`url(#${artId}-links-terrain-fade)`} />
      </mask>
    </defs> : null}
    {premiumHeathland && heathlandCorridor ? <defs>
      <linearGradient id={`${artId}-heath-terrain-fade`} gradientUnits="userSpaceOnUse" x1="0"
        y1={heathlandCorridor.greenBounds.maxY + 12} x2="0" y2={heathlandCorridor.greenBounds.maxY + 67}>
        <stop stopColor="black" /><stop offset="1" stopColor="white" />
      </linearGradient>
      <mask id={`${artId}-heath-terrain-mask`} maskUnits="userSpaceOnUse" x={sceneryX} y="0"
        width={sceneryWidth} height={viewHeight}>
        <rect x={sceneryX} width={sceneryWidth} height={viewHeight} fill={`url(#${artId}-heath-terrain-fade)`} />
      </mask>
    </defs> : null}
    {premiumDesert && desertCorridor ? <defs>
      <linearGradient id={`${artId}-desert-terrain-fade`} gradientUnits="userSpaceOnUse" x1="0"
        y1="0" x2="0" y2={viewHeight}>
        <stop offset="0" stopColor="black" />
        <stop offset={Math.min(1, (desertCorridor.greenBounds.maxY + 12) / viewHeight)} stopColor="black" />
        <stop offset={Math.min(1, (desertCorridor.greenBounds.maxY + 67) / viewHeight)} stopColor="white" />
        <stop offset={Math.max(0, (tee.y - 48) / viewHeight)} stopColor="white" />
        <stop offset={Math.max(0, (tee.y + 12) / viewHeight)} stopColor="black" />
        <stop offset="1" stopColor="black" />
      </linearGradient>
      <mask id={`${artId}-desert-terrain-mask`} maskUnits="userSpaceOnUse" x={sceneryX} y="0"
        width={sceneryWidth} height={viewHeight}>
        <rect x={sceneryX} width={sceneryWidth} height={viewHeight} fill={`url(#${artId}-desert-terrain-fade)`} />
      </mask>
    </defs> : null}
    {kit.biome === "links" && !premiumLinks ? <LinksDefs artId={artId} /> : null}
    {polishedParkland ? <><rect width={W} height={viewHeight} fill={`url(#${artId}-out)`} />
      <rect width={W} height={viewHeight} fill={`url(#${artId}-parkland-landscape)`} opacity=".36" /></> : null}
    {polishedLinks ? <rect width={W} height={viewHeight} fill={`url(#${artId}-links-distant)`} /> : null}
    {polishedLinks ? <rect width={W} height={viewHeight} fill={`url(#${artId}-links-dunes)`} opacity=".24" /> : null}
    {premiumParkland ? <PremiumParklandBackdrop artId={artId} x={sceneryX} width={sceneryWidth} height={viewHeight}
      seed={hole.layoutSeed} fullStage={fullStage} coreX={v1SceneryX} coreWidth={v1SceneryWidth}
      openVariation={fullStage && variation === "open"} /> : null}
    {premiumLinks ? <PremiumLinksLandscape id={artId} left={sceneryX} width={sceneryWidth} height={viewHeight} seed={hole.layoutSeed} /> : null}
    {premiumHeathland ? <PremiumHeathlandLandscape id={artId} left={sceneryX} width={sceneryWidth}
      height={viewHeight} vegetation={heathlandVegetation} /> : null}
    {premiumDesert ? <PremiumDesertLandscape id={artId} left={sceneryX} width={sceneryWidth}
      height={viewHeight} decoration={desertDecoration} /> : null}
    {property && premiumParkland ? <PremiumParklandProperty artId={artId} plan={property} /> : null}
    {property && premiumLinks ? <PremiumLinksProperty id={artId} plan={property} /> : null}
    {property && premiumHeathland ? <PremiumHeathlandProperty id={artId} plan={property} /> : null}
    {property && premiumDesert ? <PremiumDesertProperty id={artId} plan={property} /> : null}
    {!premiumParkland && !premiumLinks && !premiumHeathland && !premiumDesert ? <path d={model.playableBoundaryPath} fill={polishedParkland ? `url(#${artId}-out)` : polishedLinks ? `url(#${artId}-links-out)` : kit.out} /> : null}
    {polishedLinks ? <path d={model.playableBoundaryPath} fill={`url(#${artId}-links-dunes)`} opacity=".44" /> : null}
    {premiumDesert ? <>
      <g mask={teeTerrainMask} pointerEvents="none">
      <g mask={`url(#${artId}-desert-bunker-cutouts)`}>
        {desertCorridor?.bands.map((band, index) => <path key={index} d={band.path}
          fill={index === 4 ? `url(#${artId}-desert-turf)` : ["#c39b69", "#5c7c42", "#6f9c47", "#80b250"][index]}
          data-desert-cap-band={index} />)}
        <g mask={desertCorridor ? `url(#${artId}-desert-terrain-mask)` : undefined}>
          {desertRegions.filter((region) => ["DEEP_ROUGH", "PRIMARY_ROUGH", "FIRST_CUT", "FAIRWAY"].includes(region.surface))
            .map((region, index) => <PremiumDesertSurface key={`desert-terrain-${region.surface}-${index}`} id={artId} region={region} />)}
        </g>
        {desertRegions.filter((region) => !["DEEP_ROUGH", "PRIMARY_ROUGH", "FIRST_CUT", "FAIRWAY", "BUNKER", "WATER"].includes(region.surface))
          .map((region, index) => <PremiumDesertSurface key={`desert-top-${region.surface}-${index}`} id={artId} region={region} />)}
      </g>
      {desertRegions.filter((region) => region.surface === "BUNKER" || region.surface === "WATER")
        .map((region, index) => <PremiumDesertSurface key={`desert-hazard-${index}`} id={artId} region={region} />)}
      </g>
      <PremiumDesertCanonicalTrees id={artId} decoration={desertDecoration} />
      <PremiumDesertAtmosphere id={artId} left={sceneryX} width={sceneryWidth} height={viewHeight} />
    </> : premiumHeathland ? <>
      <g mask={teeTerrainMask} pointerEvents="none">
      {heathlandCorridor?.bands.map((band, index) => <path key={index} d={band.path}
        fill={index === 3 ? `url(#${artId}-heath-fairway)` : ["#556a4c", "#687d50", "#8dae62"][index]}
        data-heathland-cap-band={index} />)}
      <g mask={heathlandCorridor ? `url(#${artId}-heath-terrain-mask)` : undefined}>
        {heathlandRegions.filter((region) => ["DEEP_ROUGH", "PRIMARY_ROUGH", "FIRST_CUT", "FAIRWAY"].includes(region.surface))
          .map((region, index) => <PremiumHeathlandSurface key={`heath-terrain-${region.surface}-${index}`} id={artId} region={region} />)}
      </g>
      {heathlandRegions.filter((region) => !["DEEP_ROUGH", "PRIMARY_ROUGH", "FIRST_CUT", "FAIRWAY"].includes(region.surface))
        .map((region, index) => <PremiumHeathlandSurface key={`heath-top-${region.surface}-${index}`} id={artId} region={region} />)}
      </g>
      <PremiumHeathlandCanonicalPines id={artId} vegetation={heathlandVegetation} />
      <PremiumHeathlandAtmosphere id={artId} left={sceneryX} width={sceneryWidth} height={viewHeight} />
    </> : premiumLinks ? <>
      <g mask={teeTerrainMask} pointerEvents="none">
      {linksCorridor?.bands.map((band, index) => <path key={index} d={band.path}
        fill={index === 4 ? `url(#${artId}-links-fairway)` : ["#827f45", "#bfa366", "#697743", "#78944b"][index]}
        opacity={[.63, .94, .95, .99, 1][index]} data-links-cap-band={index} />)}
      <g mask={linksCorridor ? `url(#${artId}-links-terrain-mask)` : undefined}>
        {linksRegions.filter((region) => ["DEEP_ROUGH", "PRIMARY_ROUGH", "FIRST_CUT", "FAIRWAY"].includes(region.surface))
          .map((region, index) => <PremiumLinksSurface key={`links-terrain-${region.surface}-${index}`} id={artId} region={region} />)}
      </g>
      {linksRegions.filter((region) => !["DEEP_ROUGH", "PRIMARY_ROUGH", "FIRST_CUT", "FAIRWAY"].includes(region.surface))
        .map((region, index) => <PremiumLinksSurface key={`links-top-${region.surface}-${index}`} id={artId} region={region} />)}
      </g>
      <PremiumLinksAtmosphere id={artId} left={sceneryX} width={sceneryWidth} height={viewHeight} />
    </> : premiumParkland ? <>
      <g mask={teeTerrainMask} pointerEvents="none">
      {premiumRegions.filter((region) => premiumSurfacePrecedence(region.surface) >= 4).map((region, index) =>
        <PremiumParklandSurface key={`ground-${region.surface}-${index}`} artId={artId} region={region} fullStage={fullStage} />)}
      {premiumRegions.filter((region) => premiumSurfacePrecedence(region.surface) < 4).map((region, index) =>
        <PremiumParklandSurface key={`feature-${region.surface}-${index}`} artId={artId} region={region} fullStage={fullStage} />)}
      </g>
      {property ? <PremiumParklandPaths plan={property} /> : null}
      <PremiumParklandShadows artId={artId} plans={premiumTrees} regions={model.terrain} distantTrees={distantTrees} />
      <PremiumParklandCanopies artId={artId} plans={premiumTrees} distantTrees={distantTrees} />
      {!(unlitAudit && process.env.NODE_ENV === "development") ? <PremiumParklandLight artId={artId} x={sceneryX} width={sceneryWidth} height={viewHeight}
        corridorPath={model.playableBoundaryPath} fullStage={fullStage} /> : null}
    </> : model.terrain.map((region, index) => <TerrainRegion key={`${region.surface}-${index}`} surface={region.surface}
      path={region.path} fill={fills(region.surface, kit, artId)} artId={artId} polishedParkland={polishedParkland}
      polishedLinks={polishedLinks} />)}
    {polishedParkland ? <ParklandCanopies artId={artId} model={model} treeCanopies={treeCanopies} /> : null}
    {!premiumParkland && !premiumLinks && !premiumHeathland && !premiumDesert ? <path d={model.playableBoundaryPath} fill="none" stroke={polishedLinks ? LINKS_MATERIAL.duneContour : "rgba(255,255,255,.28)"}
      strokeWidth={polishedLinks ? ".8" : "1"} opacity={polishedLinks ? ".7" : undefined} pointerEvents="none" /> : null}
    <rect x={tee.x - 7} y={tee.y - 3} width="14" height="6" rx="2" fill={kit.tee}
      stroke={polishedParkland || premiumParkland ? PARKLAND_MATERIAL.markerOutline : undefined} strokeWidth={polishedParkland || premiumParkland ? ".7" : undefined} />
    <circle cx={cup.x} cy={cup.y} r="2" fill="#111" /><path d={`M${cup.x} ${cup.y} v-13 l7 2.5 -7 2.5z`} fill="#e23b3b" />
    <WindDisplay wind={hole.effectiveWind} model={model} tee={geometry.tee} />

    {planningTarget ? <path d={`M${finalPoint.x} ${finalPoint.y} L${planningTarget.x} ${planningTarget.y}`}
      stroke="var(--accent)" strokeWidth="1" strokeDasharray="3 3" opacity=".8" pointerEvents="none" /> : null}
    {traceTarget && contact ? <path d={`M${traceTarget.x} ${traceTarget.y} L${contact.x} ${contact.y}`} stroke="var(--accent)" strokeWidth="1" strokeDasharray="2 2" opacity=".75" /> : null}
    {traceTarget ? <TargetMarker point={traceTarget} label="Intended target" outlined={polishedParkland || premiumParkland} /> : null}
    {planningTarget ? <TargetMarker point={planningTarget} label="Selected target" outlined={polishedParkland || premiumParkland} /> : null}

    {trace && contact && airbornePath ? <>
      {polishedParkland || premiumParkland ? <path d={airbornePath} stroke={PARKLAND_MATERIAL.traceShadow} strokeWidth="2.8" strokeDasharray="3 3" opacity=".5" /> : null}
      <path d={airbornePath} stroke="#ffffff" strokeWidth="1.1" strokeDasharray="3 3" opacity=".8" />
      {movingBall ? <circle cx={movingBall.x} cy={movingBall.y} r="3" fill="#fff" stroke={ballStroke} strokeWidth=".6" /> : null}
      <circle cx={contact.x} cy={contact.y} r="6" fill="none" stroke={contactEffect} strokeWidth="1.5" opacity=".9" />
      <circle cx={contact.x} cy={contact.y} r="2.4" fill="#fff" />
      <text x={contact.x + 5} y={contact.y - 5} fill="#fff" fontSize="5">{finalMatchesContact ? "Contact / ball" : "Contact"}</text>
    </> : null}

    {transitionFrom && transitionTo && recoveryLabel && (playback?.phase === "transition" || playback?.phase === "final") ? <>
      <path d={`M${transitionFrom.x} ${transitionFrom.y} L${transitionTo.x} ${transitionTo.y}`} stroke="#fff" strokeWidth="1.2" strokeDasharray="3 2" opacity=".9" />
      <text x={(transitionFrom.x + transitionTo.x) / 2 + 3} y={(transitionFrom.y + transitionTo.y) / 2 - 3} fill="#fff" fontSize="5">{recoveryLabel}</text>
    </> : null}

    {rollFrom && rollTo && (playback?.phase === "roll" || playback?.phase === "final") ? <>
      <path d={`M${rollFrom.x} ${rollFrom.y} L${rollTo.x} ${rollTo.y}`} stroke="var(--accent)" strokeWidth="1.3" strokeDasharray="2 1" opacity=".95" />
      <text x={(rollFrom.x + rollTo.x) / 2 + 3} y={(rollFrom.y + rollTo.y) / 2 - 3} fill="var(--accent)" fontSize="5">Roll</text>
    </> : null}

    {playbackStatus ? <text x={PAD} y={viewHeight - PAD} fill="var(--accent)" fontSize="6" fontWeight="700">
      {playbackStatus}
    </text> : null}

    {(!trace || playback?.finalVisible) && (!trace || !finalMatchesContact) ? <><circle cx={finalPoint.x} cy={finalPoint.y} r="3" fill="#fff" stroke={ballStroke} strokeWidth=".6" />
      {trace ? <text x={finalPoint.x + 5} y={finalPoint.y + 6} fill="#fff" fontSize="5">Ball</text> : null}</> : null}
  </svg>;
}

function TargetMarker({ point, label, outlined }: { point: Point; label: string; outlined: boolean }) {
  return <g aria-label={label}>{outlined ? <circle cx={point.x} cy={point.y} r="6.7" fill="none" stroke={PARKLAND_MATERIAL.traceShadow} strokeWidth="2.8" opacity=".55" /> : null}
    <circle cx={point.x} cy={point.y} r="6" fill="none" stroke="var(--accent)" strokeWidth="1.5" />
    <path d={`M${point.x - 9} ${point.y}h18M${point.x} ${point.y - 9}v18`} stroke="var(--accent)" strokeWidth="1" /></g>;
}

function TerrainRegion({
  surface, path, fill, artId, polishedParkland, polishedLinks,
}: { surface: string; path: string; fill: string; artId: string; polishedParkland: boolean; polishedLinks: boolean }) {
  const overlay = polishedParkland
    ? surface === "FAIRWAY" ? `${artId}-mow`
      : surface === "PRIMARY_ROUGH" || surface === "DEEP_ROUGH" || surface === "RECOVERY_AREA" ? `${artId}-grass`
        : surface === "BUNKER" || surface === "WASTE_AREA" ? `${artId}-sand-grain`
          : surface === "WATER" ? `${artId}-water-ripple`
            : null
    : null;
  return <g>
    <path d={path} fill={fill} stroke={polishedLinks && (surface === "BUNKER" || surface === "WASTE_AREA") ? LINKS_MATERIAL.potRim : undefined}
      strokeWidth={polishedLinks && (surface === "BUNKER" || surface === "WASTE_AREA") ? "1.8" : undefined} />
    {overlay ? <path d={path} fill={`url(#${overlay})`} opacity={surface === "FAIRWAY" ? ".12" : ".3"} /> : null}
    {polishedLinks && (surface === "BUNKER" || surface === "WASTE_AREA") ? <path d={path} fill="none" stroke={LINKS_MATERIAL.potShadow} strokeWidth=".42" opacity=".75" /> : null}
  </g>;
}

function LinksDefs({ artId }: { artId: string }) {
  return <defs>
    <linearGradient id={`${artId}-links-distant`} x1="0" y1="0" x2="1" y2="1"><stop stopColor={LINKS_MATERIAL.distantDune} /><stop offset="1" stopColor={LINKS_MATERIAL.duneShade} /></linearGradient>
    <linearGradient id={`${artId}-links-out`} x1="0" y1="0" x2=".8" y2="1"><stop stopColor={LINKS_MATERIAL.duneLight} /><stop offset="1" stopColor={LINKS_MATERIAL.duneShade} /></linearGradient>
    <linearGradient id={`${artId}-links-fairway`} x1="0" y1="0" x2=".85" y2="1"><stop stopColor={LINKS_MATERIAL.fairwayLight} /><stop offset="1" stopColor={LINKS_MATERIAL.fairwayDark} /></linearGradient>
    <linearGradient id={`${artId}-links-fringe`} x1="0" y1="0" x2="1" y2="1"><stop stopColor={LINKS_MATERIAL.fairwayLight} /><stop offset="1" stopColor={LINKS_MATERIAL.fescueLight} /></linearGradient>
    <linearGradient id={`${artId}-links-green`} x1=".25" y1="0" x2=".8" y2="1"><stop stopColor={LINKS_MATERIAL.greenLight} /><stop offset="1" stopColor={LINKS_MATERIAL.greenDark} /></linearGradient>
    <linearGradient id={`${artId}-links-sand`} x1="0" y1="0" x2=".7" y2="1"><stop stopColor={LINKS_MATERIAL.potLight} /><stop offset="1" stopColor={LINKS_MATERIAL.potSand} /></linearGradient>
    <linearGradient id={`${artId}-links-water`} x1="0" y1="0" x2=".2" y2="1"><stop stopColor={LINKS_MATERIAL.waterLight} /><stop offset="1" stopColor={LINKS_MATERIAL.waterDark} /></linearGradient>
    <pattern id={`${artId}-links-fescue`} width="13" height="15" patternUnits="userSpaceOnUse">
      <rect width="13" height="15" fill={LINKS_MATERIAL.fescueLight} />
      <path d="M2 14 4 8M4 14 6 6M8 14 9 9M10 14 12 7" stroke={LINKS_MATERIAL.fescueDark} strokeWidth=".5" opacity=".45" />
      <path d="M1 7 2 3M7 6 9 2" stroke={LINKS_MATERIAL.fescueBlue} strokeWidth=".42" opacity=".34" />
    </pattern>
    <pattern id={`${artId}-links-deep-fescue`} width="13" height="15" patternUnits="userSpaceOnUse">
      <rect width="13" height="15" fill={LINKS_MATERIAL.deepFescue} />
      <path d="M1 14 3 5M4 14 6 4M7 14 10 6M10 14 12 3" stroke={LINKS_MATERIAL.fescueDark} strokeWidth=".7" opacity=".57" />
      <path d="M2 6 3 2M8 7 9 1" stroke={LINKS_MATERIAL.fescueBlue} strokeWidth=".5" opacity=".4" />
    </pattern>
    <pattern id={`${artId}-links-thicket`} width="11" height="11" patternUnits="userSpaceOnUse">
      <rect width="11" height="11" fill={LINKS_MATERIAL.thicket} />
      <path d="M1 9 3 3M4 11 6 2M7 10 10 4" stroke={LINKS_MATERIAL.deepFescue} strokeWidth="1" opacity=".7" />
    </pattern>
    <pattern id={`${artId}-links-dunes`} width="45" height="54" patternUnits="userSpaceOnUse">
      <path d="M-6 16 Q11 6 25 12 T51 10M-7 22 Q11 12 26 18 T51 17M-5 44 Q12 32 27 38 T50 35" fill="none" stroke={LINKS_MATERIAL.duneContour} strokeWidth=".6" opacity=".45" />
      <path d="M5 31h1M35 28h1M21 50h1" stroke={LINKS_MATERIAL.fescueDark} strokeWidth="1" opacity=".3" />
    </pattern>
  </defs>;
}

function ParklandDefs({
  artId, model, treeCanopies,
}: { artId: string; model: ReturnType<typeof canonicalRenderModel>; treeCanopies: ReturnType<typeof planTreeCanopies> }) {
  return <defs>
    <linearGradient id={`${artId}-out`} x1="0" y1="0" x2="0.8" y2="1"><stop stopColor={PARKLAND_MATERIAL.outerLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.outerDark} /></linearGradient>
    <pattern id={`${artId}-parkland-landscape`} width="68" height="80" patternUnits="userSpaceOnUse">
      <path d="M-12 20 Q15 5 40 17 T80 9M-10 27 Q18 13 41 24 T78 17M-8 68 Q17 52 42 62 T80 55" fill="none" stroke={PARKLAND_MATERIAL.roughLight} strokeWidth=".9" opacity=".28" />
      <path d="M10 43l2-4m14 32 2-3m35-33 2-4m-14 40 2-3" stroke={PARKLAND_MATERIAL.roughLight} strokeWidth=".7" opacity=".3" />
    </pattern>
    <linearGradient id={`${artId}-rough`} x1="0" y1="0" x2="0" y2="1"><stop stopColor={PARKLAND_MATERIAL.roughLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.roughDark} /></linearGradient>
    <linearGradient id={`${artId}-fairway`} x1="0" y1="0" x2="0" y2="1"><stop stopColor={PARKLAND_MATERIAL.fairwayLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.fairwayDark} /></linearGradient>
    <radialGradient id={`${artId}-green`} cx="42%" cy="34%"><stop stopColor={PARKLAND_MATERIAL.greenLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.greenDark} /></radialGradient>
    <linearGradient id={`${artId}-fringe`} x1="0" y1="0" x2="1" y2="1"><stop stopColor={PARKLAND_MATERIAL.fringeLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.fringeDark} /></linearGradient>
    <linearGradient id={`${artId}-sand`} x1="0" y1="0" x2="0.8" y2="1"><stop stopColor={PARKLAND_MATERIAL.sandLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.sandDark} /></linearGradient>
    <linearGradient id={`${artId}-water`} x1="0" y1="0" x2="0.2" y2="1"><stop stopColor={PARKLAND_MATERIAL.waterLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.waterDark} /></linearGradient>
    <linearGradient id={`${artId}-trees`} x1="0" y1="0" x2="0.9" y2="1"><stop stopColor={PARKLAND_MATERIAL.outerLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.treeBase} /></linearGradient>
    <pattern id={`${artId}-mow`} width="10" height="16" patternUnits="userSpaceOnUse"><rect width="10" height="8" fill={PARKLAND_MATERIAL.fairwayLight} /><rect y="8" width="10" height="8" fill={PARKLAND_MATERIAL.fairwayDark} /></pattern>
    <pattern id={`${artId}-grass`} width="9" height="11" patternUnits="userSpaceOnUse"><path d="M1 10 3 4M4 10 5 5M7 10 8 6" stroke={PARKLAND_MATERIAL.roughLight} strokeWidth=".55" opacity=".6" /></pattern>
    <pattern id={`${artId}-sand-grain`} width="7" height="7" patternUnits="userSpaceOnUse"><circle cx="1" cy="2" r=".45" fill={PARKLAND_MATERIAL.sandDark} /><circle cx="5" cy="5" r=".35" fill={PARKLAND_MATERIAL.sandDark} /></pattern>
    <pattern id={`${artId}-water-ripple`} width="13" height="10" patternUnits="userSpaceOnUse"><path d="M1 4 Q4 2 7 4 T13 4" fill="none" stroke={PARKLAND_MATERIAL.waterLight} strokeWidth=".55" opacity=".65" /></pattern>
    <filter id={`${artId}-canopy-shadow`} x="-40%" y="-40%" width="180%" height="180%"><feDropShadow dx="1" dy="1.6" stdDeviation="1.2" floodColor={PARKLAND_MATERIAL.treeOutline} floodOpacity=".55" /></filter>
    {treeCanopies.map((plan) => <clipPath key={plan.regionIndex} id={`${artId}-tree-clip-${plan.regionIndex}`}><path d={model.terrain[plan.regionIndex].path} /></clipPath>)}
  </defs>;
}

function ParklandCanopies({
  artId, model, treeCanopies,
}: { artId: string; model: ReturnType<typeof canonicalRenderModel>; treeCanopies: ReturnType<typeof planTreeCanopies> }) {
  return <>{treeCanopies.map((plan) => <g key={plan.regionIndex} clipPath={`url(#${artId}-tree-clip-${plan.regionIndex})`} filter={`url(#${artId}-canopy-shadow)`}>
    {plan.placements.map((placement, index) => <DeciduousCanopy key={index} point={model.project(placement)} placement={placement} />)}
  </g>)}</>;
}

function DeciduousCanopy({ point, placement }: { point: Point; placement: CanopyPlacement }) {
  const variants = placement.variant === 0 ? VEG_SYMBOLS.deciduous : [...VEG_SYMBOLS.deciduous].reverse();
  return <g transform={`translate(${point.x} ${point.y}) scale(${placement.scale})`}>
    {variants.map((primitive, index) => <SvgPrimitive key={index} primitive={primitive} />)}
  </g>;
}

function SvgPrimitive({ primitive }: { primitive: SvgPrim }) {
  if (primitive.t === "path") return <path d={primitive.d} fill={primitive.fill} opacity={primitive.opacity} />;
  if (primitive.t === "circle") return <circle cx={primitive.cx ?? 0} cy={primitive.cy ?? 0} r={primitive.r} fill={primitive.fill} opacity={primitive.opacity} />;
  if (primitive.t === "ellipse") return <ellipse cx={primitive.cx ?? 0} cy={primitive.cy ?? 0} rx={primitive.rx} ry={primitive.ry} fill={primitive.fill} opacity={primitive.opacity} />;
  return <path d={primitive.d} fill="none" stroke={primitive.stroke} strokeWidth={primitive.width} />;
}
