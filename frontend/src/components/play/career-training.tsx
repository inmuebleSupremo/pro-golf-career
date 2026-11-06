"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { ArrowRight, Check, Flag, HeartPulse, Target, TrendingUp } from "lucide-react";

import { PracticeFacilityHeader } from "@/components/play/practice-facility-nav";
import { DrivingRange } from "@/components/play/driving-range";
import { GreensideTraining } from "@/components/play/greenside-training";
import { PracticePuttingGreen } from "@/components/play/practice-putting-green";
import { Button } from "@/components/ui/button";
import {
  useCareerTraining,
  useCommitCareerPreparation,
  useStartPlayerTraining,
  useAbandonPlayerTraining,
  useSetStandingPreparationFocus,
  type PlayableSession,
} from "@/lib/api/career-training";
import { cn } from "@/lib/utils";
import { humanize } from "@/lib/play/options";

const FOCUSES = [
  { key: "DRIVING", label: "Driving", short: "Accuracy and distance", benefit: "Accuracy and distance", description: "Develop accuracy and distance from the tee.", icon: Target },
  { key: "IRONS", label: "Irons", short: "Accuracy and control", benefit: "Iron accuracy and control", description: "Improve the accuracy and control of your approaches.", icon: TrendingUp },
  { key: "WEDGES", label: "Wedges", short: "Wedge skill growth", benefit: "Wedge play", description: "Build touch and control closer to the green.", icon: Flag },
  { key: "PUTTING", label: "Putting", short: "Accuracy and proximity", benefit: "Putting accuracy and proximity", description: "Develop putting accuracy and proximity on the green.", icon: Target },
  { key: "TOURNAMENT_PREPARATION", label: "Tournament preparation", short: "Readiness for the next event", benefit: "Next-tournament readiness", description: "Carry a small readiness benefit into the next tournament you actually play.", icon: Flag },
  { key: "RECOVERY", label: "Recovery", short: "Reduce fatigue now", benefit: "Fatigue recovery", description: "Reduce fatigue now and give your golfer room to recover.", icon: HeartPulse },
] as const;

type Focus = (typeof FOCUSES)[number]["key"];
type Choice = (typeof FOCUSES)[number];
type TrainingData = NonNullable<ReturnType<typeof useCareerTraining>["data"]>;
type Preparation = TrainingData["careerPreparation"];
type Rules = Preparation["rules"];
type Outcome = NonNullable<ReturnType<typeof useCommitCareerPreparation>["data"]>;
type GrowthOption = { attribute: string; rating: number; potentialHeadroom: number; nextCost?: number | null; remainingSeasonal: number };
type TrainingGain = NonNullable<ReturnType<typeof nextTrainingGain>>;
type ResultProgress = { before: TrainingGain | null; after: TrainingGain | null } | null;
type Rating = { attribute: string; value: number };
type AttributeImprovement = { attribute: string; delta: number; before: number | null; after: number | null };

const FOCUS_ATTRIBUTES: Record<Focus, readonly string[]> = {
  DRIVING: ["DRIVING_ACCURACY", "DRIVING_DISTANCE"],
  IRONS: ["IRONS_ACCURACY", "IRONS_CONTROL"],
  WEDGES: ["WEDGES"],
  PUTTING: ["PUTTING_ACCURACY", "PUTTING_PROXIMITY"],
  TOURNAMENT_PREPARATION: [],
  RECOVERY: [],
};

const choiceFor = (focus: string | null | undefined): Choice =>
  FOCUSES.find((choice) => choice.key === focus) ?? FOCUSES[0];
const technical = (focus: Focus) => FOCUS_ATTRIBUTES[focus].length > 0;
const progressNumber = (value: number) => value.toFixed(2);
const percent = (value: number) => value > 0 && value * 100 < 0.05
  ? "<0.1%" : `${Number((value * 100).toFixed(1))}%`;
const ATTRIBUTE_NAMES: Record<string, string> = {
  DRIVING_ACCURACY: "Driving Accuracy",
  DRIVING_DISTANCE: "Driving Distance",
  IRONS_ACCURACY: "Irons Accuracy",
  IRONS_CONTROL: "Irons Control",
  WEDGES: "Wedges",
  PUTTING_ACCURACY: "Putting Accuracy",
  PUTTING_PROXIMITY: "Putting Proximity",
  COMPOSURE: "Composure",
  COURSE_MANAGEMENT: "Course Management",
};
export const attributeName = (attribute: string) => ATTRIBUTE_NAMES[attribute] ?? attribute.replaceAll("_", " ");
const progressTargetName = (attribute: string) => attribute === "WEDGES" ? "Wedges rating" : attributeName(attribute);
export const recoveryEffective = (fatigue: number) => fatigue > 0;

/** Show rating endpoints only when both authoritative reads agree with the committed gain. */
export function summarizeImprovements(changes: readonly { attribute: string; delta: number }[],
    before: readonly Rating[] | null, after: readonly Rating[] | null): AttributeImprovement[] {
  const totals = new Map<string, number>();
  changes.forEach((change) => totals.set(change.attribute, (totals.get(change.attribute) ?? 0) + change.delta));
  return Array.from(totals, ([attribute, delta]) => {
    const prior = before?.find((rating) => rating.attribute === attribute)?.value;
    const current = after?.find((rating) => rating.attribute === attribute)?.value;
    const verified = prior != null && current != null && current - prior === delta;
    return { attribute, delta, before: verified ? prior : null, after: verified ? current : null };
  });
}

/** The same backend purchase options expose each relevant next-growth cost and current growth limits. */
export function nextTrainingGain(focus: Focus, banked: number, options: readonly GrowthOption[]) {
  const attributes = FOCUS_ATTRIBUTES[focus];
  const eligible = options.filter((option) => attributes.includes(option.attribute)
    && option.potentialHeadroom > 0 && option.remainingSeasonal > 0
    && option.nextCost != null && option.nextCost > 0);
  eligible.sort((a, b) => a.rating - b.rating
    || attributes.indexOf(a.attribute) - attributes.indexOf(b.attribute));
  const target = eligible[0];
  if (!target || target.nextCost == null) return null;
  const threshold = target.nextCost;
  return {
    banked,
    threshold,
    attribute: target.attribute,
    percentage: Math.min(100, Math.max(0, banked / threshold * 100)),
  };
}

function Steps({ current }: { current: 0 | 1 | 2 }) {
  return <ol aria-label="Training steps" className="text-muted-foreground flex shrink-0 items-center gap-2 text-xs font-semibold">
    {(["Choose", "Confirm", "Review"] as const).map((label, index) => <li key={label} className={cn("flex items-center gap-1.5", index === current && "text-foreground")}>
      {index > 0 ? <span aria-hidden="true" className="border-border-strong mr-1 w-4 border-t" /> : null}
      <span className={cn("border-border-strong grid size-5 place-items-center rounded-full border text-[10px]", index === current && "border-primary bg-primary text-primary-foreground", index < current && "border-primary text-primary")}>{index < current ? <Check className="size-3" /> : index + 1}</span>
      <span>{label}</span>
    </li>)}
  </ol>;
}

function TrainingDetails({ focus, rules, banked, threshold, recoveryUnavailable = false }: {
  focus: Focus; rules: Rules; banked?: number; threshold?: number; recoveryUnavailable?: boolean;
}) {
  return <details className="border-border text-muted-foreground mt-4 border-t pt-3 text-xs">
    <summary className="text-foreground cursor-pointer font-semibold">Training details</summary>
    {technical(focus) ? <div className="mt-2 space-y-1 leading-relaxed">
      <p>Base progress {progressNumber(rules.trainingBaseProgress)} + neutral simulated bonus {progressNumber(rules.simulatedBonusProgress)} (7.5%) = {progressNumber(rules.totalSimulatedProgress)} focus progress.</p>
      {threshold != null ? <p>Banked progress: {progressNumber(banked ?? 0)} of {threshold} needed for the next eligible attribute improvement. Existing potential and seasonal limits apply.</p> : <p>Further attribute growth is currently limited by potential or this season&apos;s allowance. Banked progress is retained.</p>}
      <p>Training adds up to {percent(rules.trainingFatigue)} fatigue. A playable session earns base progress at the start and up to 15% extra after eight resolved challenges; simulation awards a neutral 7.5% bonus.</p>
    </div> : focus === "RECOVERY" ? <p className="mt-2">{recoveryUnavailable
      ? "At zero fatigue, Recovery has no effect and cannot use this week's opportunity."
      : `Recovery reduces fatigue by up to ${percent(rules.recoveryReduction)} and awards no training progress.`}</p>
      : <p className="mt-2">Readiness support is +{rules.readinessSupport.toFixed(2)} for the next played tournament. It survives skipped events and does not stack.</p>}
  </details>;
}

function GainMeter({ gain, label }: { gain: TrainingGain; label: string }) {
  return <div className="mt-3">
    <div className="text-muted-foreground flex flex-wrap items-baseline justify-between gap-2 text-sm">
      <span>{label} <span className="text-foreground font-semibold">{progressTargetName(gain.attribute)}</span></span>
      <strong className="text-foreground tabular-nums">{Math.round(gain.percentage)}%</strong>
    </div>
    <div role="progressbar" aria-label={`${label} ${progressTargetName(gain.attribute)}`} aria-valuemin={0}
      aria-valuemax={gain.threshold} aria-valuenow={Math.min(gain.banked, gain.threshold)}
      className="bg-surface-3 mt-2 h-2 overflow-hidden rounded-full">
      <div className="bg-info h-full rounded-full" style={{ width: `${gain.percentage}%` }} />
    </div>
  </div>;
}

export function AutomaticPreparationPreference({ id, saved, draft, pending, onDraftChange, onSave }: {
  id: string; saved: Focus; draft: Focus; pending: boolean;
  onDraftChange: (focus: Focus) => void; onSave: () => void;
}) {
  const changed = draft !== saved;
  return <section aria-labelledby="automatic-preparation" className="border-border bg-surface rounded-xl border p-4 sm:p-5">
    <div className="grid items-end gap-4 md:grid-cols-[minmax(0,1fr)_minmax(260px,0.8fr)]">
      <div>
        <p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Separate preference</p>
        <h2 id="automatic-preparation" className="text-foreground mt-1 text-lg font-semibold">Automatic career preparation</h2>
        <p className="text-muted-foreground mt-1 text-sm">Saved focus: <strong className="text-foreground">{choiceFor(saved).label}</strong></p>
        <p className="text-muted-foreground mt-2 text-sm">Applied to eligible unspent weeks when you progress or simulate your career through the usual controls.</p>
      </div>
      <div>
        <label htmlFor={`automatic-focus-${id}`} className="text-foreground block text-sm font-semibold">Focus for future eligible weeks</label>
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <select id={`automatic-focus-${id}`} value={draft} onChange={(event) => onDraftChange(event.target.value as Focus)}
            className="border-border-strong bg-background text-foreground focus-visible:outline-ring min-h-10 min-w-44 flex-1 rounded-md border px-3 text-sm focus-visible:outline-2">
            {FOCUSES.map((choice) => <option key={choice.key} value={choice.key}>{choice.label}</option>)}
          </select>
          {changed ? <Button variant="secondary" disabled={pending} onClick={onSave}>{pending ? "Saving…" : "Save automatic focus"}</Button>
            : <span role="status" className="text-primary inline-flex items-center gap-1.5 text-sm font-semibold"><Check className="size-4" aria-hidden="true" /> Saved</span>}
        </div>
        {changed ? <p className="text-muted-foreground mt-2 text-sm">Unsaved selection · current saved focus: {choiceFor(saved).label}. Saving affects future eligible weeks only.</p>
          : <p className="text-muted-foreground mt-2 text-sm">Changing this preference does not use or change this week&apos;s preparation.</p>}
        {(saved === "RECOVERY" || draft === "RECOVERY") ? <p className="text-muted-foreground mt-1 text-xs">At zero fatigue, an automatic week uses a technical focus instead of ineffective Recovery.</p> : null}
      </div>
    </div>
  </section>;
}

export function WeeklyFocusCard({ item, selected, unavailable = false, onSelect }: {
  item: Choice; selected: boolean; unavailable?: boolean; onSelect: () => void;
}) {
  const Icon = item.icon;
  return <button type="button" aria-pressed={selected} disabled={unavailable} onClick={onSelect}
    className={cn("border-border bg-background hover:border-primary/60 flex min-h-28 items-start gap-3 rounded-lg border p-3 text-left transition-colors focus-visible:outline-2 focus-visible:outline-ring", technical(item.key) ? "flex-col" : "min-h-20", selected && "border-primary bg-primary/[0.07]", unavailable && "cursor-not-allowed opacity-60")}>
    <span className="bg-surface-3 text-primary grid size-8 shrink-0 place-items-center rounded-md"><Icon className="size-4" aria-hidden="true" /></span>
    <span><span className="text-foreground block text-sm font-semibold">{item.label}</span><span className="text-muted-foreground mt-1 block text-sm">{unavailable ? "Already fully recovered" : item.short}</span></span>
  </button>;
}

export function CareerTrainingConfirmation({ focus, rules, season, week, pending, onBack, onCommit, onPlay }: {
  focus: Focus; rules: Rules; season: number; week: number; pending: boolean;
  onBack: () => void; onCommit: () => void; onPlay: () => void;
}) {
  const choice = choiceFor(focus);
  return <section aria-labelledby="training-confirm" className="space-y-5">
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Career Training / Your decision</p>
        <h1 id="training-confirm" className="text-foreground mt-1 text-2xl font-semibold">{technical(focus) ? "How would you like to train?" : "Confirm your preparation."}</h1>
        <p className="text-muted-foreground mt-1 text-sm">{choice.label} · {choice.description}</p></div>
      <Steps current={1} />
    </div>
    <div className="border-border bg-surface rounded-xl border p-4 sm:p-6">
      {technical(focus) ? <div className="grid gap-3 md:grid-cols-2">
        <div className="border-border bg-background rounded-xl border p-4">
          <p className="text-foreground text-lg font-semibold">Play Session</p>
          <p className="text-muted-foreground mt-2 text-sm">Play eight real challenges in the existing practice environment. Base development applies when you start; performance can add up to 15% more.</p>
          <Button className="mt-4 w-full" disabled={pending} onClick={onPlay}>{pending ? "Starting…" : "Start Play Session"}<ArrowRight className="size-4" aria-hidden="true" /></Button>
        </div>
        <div className="border-primary bg-primary/[0.06] rounded-xl border p-4">
          <p className="text-foreground text-lg font-semibold">Simulate Session</p>
          <p className="text-muted-foreground mt-2 text-sm">Complete this week&apos;s technical training instantly. Your golfer gains focus progress and a neutral simulated bonus.</p>
          <Button className="mt-4 w-full" variant="secondary" disabled={pending} onClick={onCommit}>{pending ? "Simulating…" : "Simulate Session"}<ArrowRight className="size-4" aria-hidden="true" /></Button>
        </div>
      </div> : <div className="flex items-start gap-4">
        <span className="bg-primary/10 text-primary grid size-12 shrink-0 place-items-center rounded-lg"><choice.icon className="size-6" aria-hidden="true" /></span>
        <div><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Your weekly activity</p>
          <h2 className="text-foreground mt-1 text-xl font-semibold">{choice.label}</h2>
          <p className="text-muted-foreground mt-2 text-sm">{choice.description}</p></div>
      </div>}
      <TrainingDetails focus={focus} rules={rules} />
      <div className="border-border bg-surface-2 mt-5 flex flex-wrap items-center justify-between gap-4 rounded-lg border p-4">
        <div><p className="text-foreground text-sm font-semibold">{`This uses your one preparation opportunity for Season ${season} · Week ${week}.`}</p>
          <p className="text-muted-foreground mt-1 text-sm">Starting or simulating commits this week&apos;s opportunity once.</p></div>
        <div className="flex flex-wrap gap-2"><Button variant="secondary" disabled={pending} onClick={onBack}>Back</Button>
          {!technical(focus) ? <Button disabled={pending} onClick={onCommit}>{pending ? "Committing…" : "Confirm preparation"}<ArrowRight className="size-4" aria-hidden="true" /></Button> : null}</div>
      </div>
    </div>
  </section>;
}

export function CareerTrainingReview({ outcome, progress, improvements, id, onOverview }: {
  outcome: Outcome; progress: ResultProgress; improvements: readonly AttributeImprovement[];
  id: string; onOverview: () => void;
}) {
  const choice = choiceFor(outcome.focus);
  const isTechnical = technical(choice.key);
  const fatigueRecovered = Math.max(0, outcome.fatigueBefore - outcome.fatigueAfter);
  const headline = isTechnical ? improvements.length ? "A permanent skill gain." : "Good work this week."
    : choice.key === "RECOVERY" ? "Recovery complete." : "Ready for the next one.";
  return <section aria-labelledby="training-result" className="space-y-5">
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Career Training / Session outcome</p>
        <h1 className="text-foreground mt-1 text-2xl font-semibold">Your week, well spent.</h1>
        <p className="text-muted-foreground mt-1 text-sm">One preparation decision, recorded for this career week.</p></div>
      <Steps current={2} />
    </div>
    <div className="border-primary/40 bg-surface overflow-hidden rounded-xl border">
      <div className="bg-primary/[0.07] flex items-center justify-between gap-4 p-5 sm:p-6">
        <div><p className="text-primary text-xs font-bold tracking-widest uppercase">Week {outcome.week} · Complete</p>
          <h2 id="training-result" className="text-foreground mt-2 text-2xl font-semibold">{headline}</h2>
          <p className="text-muted-foreground mt-1 text-sm">{isTechnical ? `${choice.label} training has contributed to your golfer's long-term growth.` : choice.key === "RECOVERY" ? "Your golfer is fresher after this week's recovery." : "Readiness is reserved for the next tournament you play."}</p></div>
        <Check className="text-primary size-9 shrink-0" aria-hidden="true" />
      </div>
      {improvements.length ? <div className="border-primary/40 bg-primary/[0.07] mx-4 mt-4 rounded-lg border p-4 sm:mx-6 sm:mt-6">
        <p className="text-primary text-xs font-bold tracking-widest uppercase">Permanent attribute improvement</p>
        <ul className="text-foreground mt-2 space-y-1 text-xl font-semibold tabular-nums sm:text-2xl">{improvements.map((change) => <li key={change.attribute}>
          {attributeName(change.attribute)} {change.before != null && change.after != null
            ? `${change.before} → ${change.after}` : `+${change.delta}`}
        </li>)}</ul>
      </div> : null}
      <div className="grid gap-3 p-4 sm:grid-cols-2 sm:p-6">
        <div className="border-border bg-background rounded-lg border p-4">
          <p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">{isTechnical ? "Permanent development" : "Preparation benefit"}</p>
          <h3 className="text-foreground mt-2 text-lg font-semibold">{choice.label}</h3>
          {isTechnical ? <>
            {progress?.after ? <div className="mt-3"><p className="text-foreground text-sm font-semibold">Progress toward the next attribute gain</p>
              <GainMeter gain={progress.after} label="Progress toward" />
              {progress.before ? <p className="text-muted-foreground mt-2 text-sm">Before this session: {Math.round(progress.before.percentage)}% toward {progressTargetName(progress.before.attribute)}.</p> : null}
            </div> : progress ? <p className="text-muted-foreground mt-3 text-sm">Further growth is currently limited by potential or this season&apos;s allowance. Banked progress is retained.</p>
              : <p className="text-muted-foreground mt-3 text-sm">Current skill progress could not be refreshed. The training result is recorded.</p>}
          </>
            : choice.key === "RECOVERY" ? <><p className="text-primary mt-2 text-2xl font-semibold">{percent(fatigueRecovered)} <span className="text-muted-foreground text-sm font-normal">fatigue recovered</span></p><p className="text-muted-foreground mt-2 text-sm">Recovery was applied immediately.</p></>
              : <><p className="text-primary mt-2 text-lg font-semibold">Next-tournament readiness</p><p className="text-muted-foreground mt-2 text-sm">{outcome.readinessPending ? "Ready for the next tournament played; skipped events do not use it." : "No readiness is pending."}</p></>}
        </div>
        <div className="border-border bg-background rounded-lg border p-4">
          <p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Weekly impact</p>
          <h3 className="text-foreground mt-2 text-lg font-semibold">{isTechnical ? "Simulated session" : choice.label}</h3>
          <dl className="border-border mt-4 space-y-3 border-t pt-3 text-sm">
            <div className="flex justify-between gap-3"><dt className="text-muted-foreground">Fatigue</dt><dd className="text-foreground">{percent(outcome.fatigueBefore)} → {percent(outcome.fatigueAfter)}</dd></div>
            <div className="flex justify-between gap-3"><dt className="text-muted-foreground">Weekly opportunity</dt><dd className="text-foreground">Used once</dd></div>
            <div className="flex justify-between gap-3"><dt className="text-muted-foreground">Next chance</dt><dd className="text-foreground">Next eligible week</dd></div>
          </dl>
        </div>
      </div>
      <div className="border-border mx-4 border-t pb-5 sm:mx-6">
        <details className="text-muted-foreground pt-3 text-xs"><summary className="text-foreground cursor-pointer font-semibold">Show result breakdown</summary>
          {isTechnical ? <p className="mt-2">Base progress {progressNumber(outcome.baseProgress)} + simulated bonus {progressNumber(outcome.simulatedBonusProgress)} (7.5%) = {progressNumber(outcome.progressAwarded)} focus progress. Attribute gains are subject to potential and seasonal limits.</p>
            : <p className="mt-2">This activity awards no technical progress. Tournament readiness and fatigue changes are reported from the completed backend preparation.</p>}
        </details>
        <div className="mt-5 flex flex-wrap justify-end gap-2"><Button variant="secondary" asChild><Link href={`/career/${id}/practice`}>Explore Free Practice</Link></Button>
          <Button onClick={onOverview}>Back to overview <ArrowRight className="size-4" aria-hidden="true" /></Button></div>
      </div>
    </div>
  </section>;
}

export function CareerTrainingPlayableReview({ session, gain, id, onOverview }: {
  session: PlayableSession; gain: TrainingGain | null; id: string; onOverview: () => void;
}) {
  const firstRatings: Rating[] = [];
  const lastRatings: Rating[] = [];
  session.ratingChanges.forEach((change) => {
    if (!firstRatings.some((rating) => rating.attribute === change.attribute)) {
      firstRatings.push({ attribute: change.attribute, value: change.before });
    }
    const existing = lastRatings.find((rating) => rating.attribute === change.attribute);
    if (existing) existing.value = change.after;
    else lastRatings.push({ attribute: change.attribute, value: change.after });
  });
  const improvements = summarizeImprovements(session.attributeImprovements,
    firstRatings.length ? firstRatings : null, lastRatings.length ? lastRatings : null);
  const finished = session.status === "COMPLETED";
  return <section aria-labelledby="playable-training-result" className="space-y-5">
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Career Training / Session outcome</p>
        <h1 id="playable-training-result" className="text-foreground mt-1 text-2xl font-semibold">{finished ? "Eight challenges complete." : "Session ended."}</h1>
        <p className="text-muted-foreground mt-1 text-sm">{choiceFor(session.focus).label} · Season {session.season}, Week {session.week}</p></div>
      <Steps current={2} />
    </div>
    <div className="border-primary/40 bg-surface overflow-hidden rounded-xl border">
      <div className="bg-primary/[0.07] p-5 sm:p-6">
        <p className="text-primary text-xs font-bold tracking-widest uppercase">{finished ? "Playable training complete" : "Playable training abandoned"}</p>
        <h2 className="text-foreground mt-2 text-2xl font-semibold">{finished ? improvements.length ? "A permanent skill gain." : "Good work this week." : "Base development retained."}</h2>
        <p className="text-muted-foreground mt-1 text-sm">{finished ? `${session.attempts.length} of 8 challenges resolved. Your score and bonus are recorded for this week.` : `${session.attempts.length} of 8 attempts recorded. The performance bonus was forfeited.`}</p>
      </div>
      {improvements.length ? <div className="border-primary/40 bg-primary/[0.07] mx-4 mt-4 rounded-lg border p-4 sm:mx-6 sm:mt-6">
        <p className="text-primary text-xs font-bold tracking-widest uppercase">Permanent attribute improvement</p>
        <ul className="text-foreground mt-2 space-y-1 text-xl font-semibold tabular-nums sm:text-2xl">{improvements.map((change) => <li key={change.attribute}>
          {attributeName(change.attribute)} {change.before != null && change.after != null
            ? `${change.before} → ${change.after}` : `+${change.delta}`}
        </li>)}</ul>
      </div> : null}
      <div className="grid gap-3 p-4 sm:grid-cols-2 sm:p-6">
        <div className="border-border bg-background rounded-lg border p-4">
          <p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Session performance</p>
          <p className="text-foreground mt-2 text-2xl font-semibold tabular-nums">{Math.round(session.score * 100)}%</p>
          <p className="text-muted-foreground mt-1 text-sm">{finished ? "Average of eight backend-scored attempts" : "Average of recorded attempts"}</p>
          <p className="text-primary mt-3 text-lg font-semibold">+{progressNumber(session.bonusProgress)} focus progress</p>
          <p className="text-muted-foreground text-sm">{finished ? "Performance bonus" : "No performance bonus"}</p>
        </div>
        <div className="border-border bg-background rounded-lg border p-4">
          <p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Permanent development</p>
          <h3 className="text-foreground mt-2 text-lg font-semibold">{choiceFor(session.focus).label}</h3>
          {gain ? <GainMeter gain={gain} label="Progress toward" />
            : <p className="text-muted-foreground mt-3 text-sm">Further growth is currently limited by potential or this season&apos;s allowance, or current progress is unavailable.</p>}
        </div>
      </div>
      <div className="border-border mx-4 border-t pb-5 sm:mx-6">
        <dl className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
          <div><dt className="text-muted-foreground">Weekly opportunity</dt><dd className="text-foreground">Used once</dd></div>
          <div><dt className="text-muted-foreground">Fatigue at session start</dt><dd className="text-foreground">{session.fatigueBefore != null && session.fatigueAfter != null
            ? `${percent(session.fatigueBefore)} → ${percent(session.fatigueAfter)}` : "Not retained in this saved session"}</dd></div>
        </dl>
        <details className="text-muted-foreground mt-4 border-t pt-3 text-xs"><summary className="text-foreground cursor-pointer font-semibold">Show result breakdown</summary>
          <p className="mt-2">Base progress {progressNumber(session.baseProgress)} + playable bonus {progressNumber(session.bonusProgress)} = {progressNumber(session.baseProgress + session.bonusProgress)} focus progress. No simulated bonus was applied.</p>
        </details>
        <div className="mt-5 flex flex-wrap justify-end gap-2"><Button variant="secondary" asChild><Link href={`/career/${id}/practice`}>Explore Free Practice</Link></Button>
          <Button onClick={onOverview}>Back to overview <ArrowRight className="size-4" aria-hidden="true" /></Button></div>
      </div>
    </div>
  </section>;
}

function AbandonTrainingDialog({ pending, onCancel, onConfirm }: {
  pending: boolean; onCancel: () => void; onConfirm: () => void;
}) {
  return <div className="bg-background/80 fixed inset-0 z-[calc(var(--z-modal)+1)] grid place-items-center p-4">
    <section role="alertdialog" aria-modal="true" aria-labelledby="abandon-training-title"
      className="border-border bg-surface w-full max-w-md rounded-xl border p-5 shadow-lg">
      <h2 id="abandon-training-title" className="text-foreground text-xl font-semibold">Abandon this session?</h2>
      <p className="text-muted-foreground mt-2 text-sm">Your base development stays. The performance bonus is forfeited, and this week&apos;s opportunity remains used.</p>
      <div className="mt-5 flex justify-end gap-2"><Button variant="secondary" disabled={pending} onClick={onCancel}>Keep playing</Button>
        <Button variant="destructive" disabled={pending} onClick={onConfirm}>{pending ? "Abandoning…" : "Abandon Session"}</Button></div>
    </section>
  </div>;
}

export function TrainingChallengeBriefing({ challenge, focus, playable, onBegin, onClose, onAbandon }: {
  challenge: PlayableSession["challenges"][number]; focus: string; playable: boolean;
  onBegin: () => void; onClose: () => void; onAbandon: () => void;
}) {
  const begin = useRef<HTMLButtonElement>(null);
  const close = useRef<HTMLButtonElement>(null);
  useEffect(() => { (playable ? begin : close).current?.focus(); }, [playable]);
  return <div className="bg-background/85 fixed inset-0 z-[calc(var(--z-modal)+1)] grid place-items-center p-4">
    <section role="dialog" aria-modal="true" aria-labelledby="challenge-briefing-title"
      className="border-border bg-surface w-full max-w-lg rounded-xl border p-5 shadow-lg sm:p-7"
      onKeyDown={(event) => {
        if (event.key !== "Tab") return;
        const buttons = [...event.currentTarget.querySelectorAll<HTMLButtonElement>("button:not(:disabled)")];
        if (buttons.length === 0) return;
        const next = event.shiftKey ? buttons.at(-1) : buttons[0];
        if (event.target === (event.shiftKey ? buttons[0] : buttons.at(-1))) {
          event.preventDefault(); next?.focus();
        }
      }}>
      <p className="text-primary text-xs font-bold tracking-widest uppercase">Career Training · Challenge {challenge.number} of 8</p>
      <h2 id="challenge-briefing-title" className="text-foreground mt-2 text-2xl font-semibold">{choiceFor(focus).label}</h2>
      {playable ? <>
        <p className="text-foreground mt-4 text-lg font-semibold">{challenge.putting
          ? `${Math.round(challenge.distance * 3)} ft to Cup ${challenge.cupNumber}`
          : `${Math.round(challenge.distance)} yd to ${challenge.targetLabel}`}</p>
        <p className="text-muted-foreground mt-2 text-sm">{challenge.putting
          ? `Read the green and choose a starting line and pace. Slope at the ball: ${Math.round(challenge.slope * 100)}%. A holed putt scores highest; otherwise your final resting distance counts.`
          : challenge.greenside
          ? `Suggested ${humanize(challenge.suggestedClub ?? "")} · ${humanize(challenge.suggestedFamily ?? "")}. ${challenge.suggestedFamily === "BUNKER" ? "Play from the real greenside bunker" : "Play from this greenside lie"} and finish near the cup. Click or tap the green surroundings to choose a landing point; allow for bounce and rollout. Your final resting distance counts.`
          : `Suggested ${humanize(challenge.suggestedClub ?? "")} · ${humanize(challenge.suggestedFamily ?? "")}. Finish on the highlighted ${challenge.landingZone ? "zone" : "target green"}. Click or tap the range to set intended carry and direction; allow for bounce and rollout.`}</p>
        <p className="text-muted-foreground mt-2 text-sm">Scoring tolerance: {challenge.putting
          ? `${Math.round(challenge.tolerance * 3)} ft` : `${Math.round(challenge.tolerance)} yd`}. One scored attempt.</p>
      </> : <p className="text-muted-foreground mt-4 text-sm">This saved challenge was generated with an earlier club or target combination that is no longer supported. Your base development remains earned. You can abandon the session without a performance bonus.</p>}
      <div className="mt-6 flex flex-wrap justify-end gap-2">
        <Button ref={close} variant="secondary" onClick={onClose}>Return to overview</Button>
        <Button variant="secondary" onClick={onAbandon}>Abandon Session</Button>
        {playable ? <Button ref={begin} onClick={onBegin}>Begin Challenge <ArrowRight className="size-4" aria-hidden="true" /></Button> : null}
      </div>
    </section>
  </div>;
}

export function CareerTraining({ id }: { id: string }) {
  const query = useCareerTraining(id);
  const standing = useSetStandingPreparationFocus(id);
  const commit = useCommitCareerPreparation(id);
  const start = useStartPlayerTraining(id);
  const abandon = useAbandonPlayerTraining(id);
  const [selectedFocus, setSelectedFocus] = useState<Focus | null>(null);
  const [standingDraft, setStandingDraft] = useState<Focus | null>(null);
  const [stage, setStage] = useState<"choose" | "confirm" | "result">("choose");
  const [stageWeek, setStageWeek] = useState<string | null>(null);
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  const [resultProgress, setResultProgress] = useState<ResultProgress>(null);
  const [resultImprovements, setResultImprovements] = useState<AttributeImprovement[]>([]);
  const [finalizing, setFinalizing] = useState(false);
  const [playing, setPlaying] = useState(false);
  const [visibleChallengeNumber, setVisibleChallengeNumber] = useState<number | null>(null);
  const [briefedChallenge, setBriefedChallenge] = useState<string | null>(null);
  const [confirmAbandon, setConfirmAbandon] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const world = query.data?.world;
  const preparation = query.data?.careerPreparation;
  const session = query.data?.playerTrainingSession;
  const challengePlayable = query.data?.playerTrainingChallengePlayable ?? false;
  const currentSession = session && world && session.season === world.season && session.week === world.week ? session : null;
  const fitness = query.data?.playerFitness;
  const retired = query.data?.playerProfile?.retired;
  const focus = selectedFocus ?? (preparation?.standingFocus as Focus | undefined) ?? "DRIVING";
  const automaticFocus = standingDraft ?? (preparation?.standingFocus as Focus | undefined) ?? "DRIVING";
  const choice = choiceFor(focus);
  const recoveryUnavailable = focus === "RECOVERY" && !recoveryEffective(fitness?.fatigue ?? 0);
  const weekKey = world ? `${world.season}:${world.week}` : null;
  const activeStage = stageWeek === weekKey ? stage : "choose";
  const resultForWeek = Boolean(world && outcome?.committed && outcome.season === world.season && outcome.week === world.week);
  const used = Boolean(resultForWeek || (world && preparation && preparation.consumedSeason === world.season && preparation.consumedWeek === world.week));
  const available = Boolean(preparation?.available && !retired && !used);
  const usedFocus = resultForWeek ? outcome?.focus : preparation?.committedFocus;
  const banked = preparation?.progress.find((entry) => entry.focus === focus)?.amount ?? 0;
  const gain = query.data?.playerDevelopment ? nextTrainingGain(focus, banked, query.data.playerDevelopment.options) : null;
  const sessionBanked = preparation?.progress.find((entry) => entry.focus === session?.focus)?.amount ?? 0;
  const sessionGain = session && query.data?.playerDevelopment
    ? nextTrainingGain(session.focus as Focus, sessionBanked, query.data.playerDevelopment.options) : null;

  async function reconcileSession(): Promise<PlayableSession | null> {
    const refreshed = await query.refetch();
    return refreshed.data?.playerTrainingSession ?? null;
  }

  async function startPlaySession() {
    setActionError(null);
    setFinalizing(true);
    try {
      const current = await query.refetch();
      const existing = current.data?.playerTrainingSession;
      if (existing?.status === "ACTIVE" && existing.season === current.data?.world?.season
          && existing.week === current.data?.world?.week) {
        setVisibleChallengeNumber(existing.nextChallenge);
        setPlaying(true);
        return;
      }
      if (!current.data?.careerPreparation.available || !technical(focus)) {
        setStage("choose");
        setActionError("This week's playable preparation is no longer available.");
        return;
      }
      const started = await start.mutateAsync(focus);
      const refreshed = await reconcileSession();
      if (refreshed?.id !== started.id || refreshed.status !== "ACTIVE")
        throw new Error("The session started, but its challenges could not be loaded. Refresh to resume it.");
      setVisibleChallengeNumber(refreshed.nextChallenge);
      setPlaying(true);
    } catch (error) {
      try {
        const resumed = await reconcileSession();
        if (resumed?.status === "ACTIVE" && resumed.season === world?.season && resumed.week === world?.week) {
          setVisibleChallengeNumber(resumed.nextChallenge);
          setPlaying(true);
          return;
        }
      } catch { /* Keep the original error if the status read also failed. */ }
      setActionError(error instanceof Error ? error.message : "The playable session could not be started.");
    } finally {
      setFinalizing(false);
    }
  }

  async function abandonSession() {
    if (!session) return;
    setActionError(null);
    setFinalizing(true);
    try {
      await abandon.mutateAsync(session.id);
      await reconcileSession();
      setPlaying(false);
      setConfirmAbandon(false);
      setStageWeek(weekKey);
      setStage("result");
    } catch (error) {
      try {
        const current = await reconcileSession();
        if (current?.status === "ABANDONED") {
          setPlaying(false);
          setConfirmAbandon(false);
          setStageWeek(weekKey);
          setStage("result");
          return;
        }
      } catch { /* Surface the original failure. */ }
      setActionError(error instanceof Error ? error.message : "The session could not be abandoned.");
    } finally {
      setFinalizing(false);
    }
  }

  async function saveStandingFocus() {
    setActionError(null);
    try {
      const saved = await standing.mutateAsync(automaticFocus);
      if (!saved) {
        setActionError("The automatic focus could not be saved. Refresh the current career and try again.");
        return;
      }
      const refreshed = await query.refetch();
      if (refreshed.data?.careerPreparation.standingFocus === automaticFocus) setStandingDraft(null);
      else setActionError("The focus was saved, but the current status could not be refreshed.");
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "The automatic focus could not be saved.");
    }
  }

  async function commitPreparation() {
    setActionError(null);
    setFinalizing(true);
    try {
      const current = await query.refetch();
      if (!current.isSuccess || !current.data?.careerPreparation.available) {
        setStage("choose");
        setActionError("This week's preparation is no longer available. The current status has been refreshed.");
        return;
      }
      if (focus === "RECOVERY" && !recoveryEffective(current.data.playerFitness?.fatigue ?? 0)) {
        setStage("choose");
        setActionError("Your golfer is already fully recovered. Choose another activity for this week.");
        return;
      }
      const beforeRatings = current.data.playerProfile?.attributes ?? null;
      const beforeAmount = current.data.careerPreparation.progress.find((entry) => entry.focus === focus)?.amount ?? 0;
      const before = technical(focus)
        ? nextTrainingGain(focus, beforeAmount, current.data.playerDevelopment.options) : null;
      const result = await commit.mutateAsync(focus);
      if (!result.committed) {
        setStage("choose");
        setActionError("This week's preparation is no longer available. The current status has been refreshed.");
        await query.refetch();
        return;
      }
      let progressAfter: ResultProgress = null;
      let afterRatings: readonly Rating[] | null = null;
      try {
        const refreshed = await query.refetch();
        if (refreshed.isSuccess && refreshed.data?.world?.season === result.season
            && refreshed.data.world?.week === result.week) {
          afterRatings = refreshed.data.playerProfile?.attributes ?? null;
          if (technical(focus)) {
            const afterAmount = refreshed.data.careerPreparation.progress.find((entry) => entry.focus === focus)?.amount ?? 0;
            progressAfter = { before, after: nextTrainingGain(focus, afterAmount, refreshed.data.playerDevelopment.options) };
          }
        }
      } catch { /* The authoritative result still renders if the follow-up read fails. */ }
      setResultProgress(progressAfter);
      setResultImprovements(summarizeImprovements(result.attributeImprovements, beforeRatings, afterRatings));
      setOutcome(result);
      setStageWeek(`${result.season}:${result.week}`);
      setStage("result");
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Preparation could not be committed. Refresh the weekly status before retrying.");
      await query.refetch();
    } finally {
      setFinalizing(false);
    }
  }

  const lastRecordedAttempt = currentSession?.attempts.at(-1);
  const sceneChallenge = currentSession?.challenges[(visibleChallengeNumber ?? (currentSession.nextChallenge || 8)) - 1];
  if (playing && currentSession && currentSession.status !== "ABANDONED" && sceneChallenge) {
    const briefingKey = `${currentSession.id}:${sceneChallenge.number}`;
    const briefingActive = currentSession.status === "ACTIVE" && currentSession.nextChallenge === sceneChallenge.number
      && briefedChallenge !== briefingKey;
    const unsupported = currentSession.status === "ACTIVE" && !challengePlayable;
    const training = { session: currentSession, challenge: sceneChallenge, onReconcile: reconcileSession,
      briefingActive, challengePlayable,
      onAdvance: (number: number) => { setVisibleChallengeNumber(number); setBriefedChallenge(null); },
      onReview: () => { setPlaying(false); setStageWeek(weekKey); setStage("result"); },
      onAbandon: () => setConfirmAbandon(true) };
    return <><div inert={briefingActive || unsupported}>{currentSession.focus === "PUTTING" ? <PracticePuttingGreen key={`${currentSession.id}-${sceneChallenge.number}`} id={id} training={training} />
      : sceneChallenge.greenside ? <GreensideTraining key={`${currentSession.id}-${sceneChallenge.number}`} id={id} training={training} />
        : <DrivingRange key={`${currentSession.id}-${sceneChallenge.number}`} id={id} training={training} />}</div>
      {briefingActive || unsupported ? <TrainingChallengeBriefing key={briefingKey} challenge={sceneChallenge}
        focus={currentSession.focus} playable={challengePlayable}
        onBegin={() => setBriefedChallenge(briefingKey)} onClose={() => setPlaying(false)}
        onAbandon={() => setConfirmAbandon(true)} /> : null}
      {confirmAbandon ? <AbandonTrainingDialog pending={finalizing} onCancel={() => setConfirmAbandon(false)} onConfirm={abandonSession} /> : null}</>;
  }

  return <div className="practice-facility-page bg-background fixed inset-0 z-[var(--z-modal)] isolate flex h-dvh min-h-0 min-w-0 flex-col overflow-hidden">
    <PracticeFacilityHeader id={id} active="training" title="Career Training" subtitle="Weekly preparation for your golfer" />
    <main className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-3 py-4 sm:px-5">
      <div className="mx-auto max-w-7xl space-y-5 pb-6">
        {query.isLoading ? <p role="status" className="text-muted-foreground">Loading career preparation…</p> : null}
        {query.isError ? <div role="alert" className="border-destructive bg-surface rounded-lg border p-4"><p className="text-foreground">Career preparation could not be loaded.</p><Button className="mt-3" variant="secondary" onClick={() => query.refetch()}>Try again</Button></div> : null}
        {!query.isError && preparation && world ? <>
          <div className="border-border bg-surface-2 grid gap-3 rounded-xl border px-4 py-3 text-sm sm:grid-cols-2 xl:grid-cols-[auto_auto_auto_1fr] xl:items-center xl:gap-5">
            <div><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Career calendar</p><p className="text-foreground mt-1 font-semibold">Season {world.season} · Week {world.week}</p></div>
            <div><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">This week&apos;s preparation</p><p className={cn("mt-1 font-semibold", available ? "text-primary" : "text-foreground")}>{used ? "Completed" : world.hasPendingEvent ? "Tournament play pending" : retired ? "Career complete" : available ? "Available to use" : "Unavailable"}</p></div>
            <div><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Golfer fatigue</p><p className="text-foreground mt-1 font-semibold">{fitness ? percent(fitness.fatigue) : "—"}</p>{fitness?.injury ? <p className="text-muted-foreground text-sm">{fitness.injury.severity.toLowerCase()} {fitness.injury.type.toLowerCase()} injury</p> : null}</div>
            <div className="xl:justify-self-end"><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Saved automatic focus</p><p className="text-foreground mt-1 font-semibold">{choiceFor(preparation.standingFocus).label}</p></div>
          </div>

          {currentSession && currentSession.status !== "ACTIVE" && activeStage === "result" ? <CareerTrainingPlayableReview session={currentSession} gain={sessionGain} id={id} onOverview={() => setStage("choose")} />
            : activeStage === "result" && outcome ? <CareerTrainingReview outcome={outcome} progress={resultProgress} improvements={resultImprovements} id={id} onOverview={() => setStage("choose")} />
            : activeStage === "confirm" && available && !recoveryUnavailable ? <CareerTrainingConfirmation focus={focus} rules={preparation.rules} season={world.season} week={world.week} pending={commit.isPending || start.isPending || finalizing} onBack={() => setStage("choose")} onCommit={commitPreparation} onPlay={startPlaySession} />
              : <section aria-labelledby="training-choice" className="space-y-4">
                <div className="flex flex-wrap items-end justify-between gap-3">
                  <div><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Career Training / Weekly preparation</p>
                    <h1 id="training-choice" className="text-foreground mt-1 text-2xl font-semibold">{currentSession?.status === "ACTIVE" ? "Playable session in progress." : used ? "Preparation complete." : "What would you like your golfer to work on this week?"}</h1>
                    <p className="text-muted-foreground mt-1 text-sm">{currentSession?.status === "ACTIVE" ? "Your attempts are saved. Resume when you're ready, or leave Practice Facility without losing progress." : used ? "This week's activity is settled. Your future automatic focus can still be changed below." : "Select this week's activity. The opportunity is used only when you confirm."}</p></div>
                  <Steps current={used ? 2 : 0} />
                </div>

                {used ? <div className="border-primary/40 bg-primary/[0.06] flex flex-wrap items-center gap-3 rounded-xl border p-4">
                  <span className="bg-primary/15 text-primary grid size-10 shrink-0 place-items-center rounded-lg"><Check className="size-5" aria-hidden="true" /></span>
                  <div className="min-w-0 flex-1"><p className="text-foreground font-semibold">{choiceFor(usedFocus).label} {currentSession?.status === "ACTIVE" ? "in progress" : currentSession?.status === "ABANDONED" ? "session abandoned" : "complete"}</p><p className="text-muted-foreground text-sm">{currentSession?.status === "ACTIVE" ? `${currentSession.attempts.length} of 8 attempts recorded. Your base progress is saved.` : "Your weekly opportunity is used. Another session cannot earn a second reward."}</p>
                    {currentSession?.status === "ACTIVE" && lastRecordedAttempt ? <p className="text-muted-foreground mt-1 text-sm">Last recorded: Challenge {lastRecordedAttempt.number} · {Math.round(lastRecordedAttempt.score * 100)}% score.</p> : null}</div>
                  {currentSession?.status === "ACTIVE" ? <div className="flex flex-wrap gap-2"><Button onClick={() => { setVisibleChallengeNumber(currentSession.nextChallenge); setPlaying(true); }}>Resume Session</Button><Button variant="secondary" onClick={() => setConfirmAbandon(true)}>Abandon Session</Button></div>
                    : currentSession?.status === "COMPLETED" ? <Button variant="secondary" onClick={() => { setStageWeek(weekKey); setStage("result"); }}>Review session result</Button>
                      : currentSession?.status === "ABANDONED" ? <Button variant="secondary" onClick={() => { setStageWeek(weekKey); setStage("result"); }}>Review retained progress</Button>
                        : outcome && outcome.season === world.season && outcome.week === world.week ? <Button variant="secondary" onClick={() => setStage("result")}>Review this result</Button> : null}
                </div> : null}
                {session && !currentSession ? <p role="status" className="border-border bg-surface rounded-lg border p-3 text-sm">Previous playable session: {session.status.toLowerCase()}. It belongs to an earlier career week and cannot be resumed.</p> : null}
                {world.hasPendingEvent && !used ? <p role="status" className="border-border bg-surface rounded-lg border p-3 text-sm">Tournament play is pending. Finish the current event before committing preparation.</p> : null}
                {retired ? <p role="status" className="border-border bg-surface rounded-lg border p-3 text-sm">This golfer has retired from career preparation.</p> : null}
                {!available && !used && !world.hasPendingEvent && !retired ? <p role="status" className="border-border bg-surface rounded-lg border p-3 text-sm">Preparation is unavailable for this career week. You can still change the automatic focus or use Free Practice.</p> : null}

                {!used ? <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1.85fr)_minmax(300px,0.8fr)]">
                  <div className="border-border bg-surface rounded-xl border p-4 sm:p-5">
                    <div className="flex flex-wrap items-baseline justify-between gap-2"><h2 className="text-foreground font-semibold">This week&apos;s preparation</h2><span className="text-muted-foreground text-sm">One choice for Season {world.season} · Week {world.week}</span></div>
                    <p className="text-subtle-foreground mt-4 text-xs font-bold tracking-widest uppercase">Technical development</p>
                    <div className="mt-2 grid grid-cols-2 gap-2 xl:grid-cols-4">{FOCUSES.filter((item) => technical(item.key)).map((item) => <WeeklyFocusCard key={item.key} item={item} selected={focus === item.key} onSelect={() => { setSelectedFocus(item.key); setActionError(null); }} />)}</div>
                    <div className="border-border my-4 border-t" />
                    <p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Preparation & condition</p>
                    <div className="mt-2 grid gap-2 sm:grid-cols-2">{FOCUSES.filter((item) => !technical(item.key)).map((item) => <WeeklyFocusCard key={item.key} item={item} selected={focus === item.key} unavailable={item.key === "RECOVERY" && !recoveryEffective(fitness?.fatigue ?? 0)} onSelect={() => { setSelectedFocus(item.key); setActionError(null); }} />)}</div>
                  </div>

                  <div className="border-primary/30 bg-surface-2 rounded-xl border p-4 sm:p-5">
                      <p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Selected for this week</p>
                      <h2 className="text-foreground mt-2 text-xl font-semibold">{choice.label}</h2>
                      <p className="text-muted-foreground mt-2 text-sm">{recoveryUnavailable ? "Your golfer is already fully recovered at 0% fatigue. Recovery would have no effect this week." : choice.description}</p>
                      <div className="mt-3 flex flex-wrap gap-2 text-xs font-semibold">
                        <span className="border-border text-foreground rounded-md border px-2 py-1">{recoveryUnavailable ? "No fatigue to reduce" : technical(focus) ? "Permanent growth" : choice.benefit}</span>
                        {technical(focus) ? <span className="border-border text-foreground rounded-md border px-2 py-1">Small fatigue cost</span> : null}
                      </div>
                      {technical(focus) ? <div className="mt-4">
                        {gain ? <GainMeter gain={gain} label="Progress toward" />
                          : <p className="text-muted-foreground text-sm">Attribute growth has reached a potential or seasonal limit. Banked progress is retained.</p>}
                      </div> : null}
                      <TrainingDetails focus={focus} rules={preparation.rules} banked={banked} threshold={gain?.threshold} recoveryUnavailable={recoveryUnavailable} />
                      <Button className="mt-4 w-full" disabled={!available || recoveryUnavailable} onClick={() => { setStageWeek(weekKey); setStage("confirm"); setActionError(null); }}>{recoveryUnavailable ? "Recovery unavailable at 0% fatigue" : `Continue with ${choice.label}`} {!recoveryUnavailable ? <ArrowRight className="size-4" aria-hidden="true" /> : null}</Button>
                      <p className="text-muted-foreground mt-2 text-center text-sm">{recoveryUnavailable ? "Choose another activity for this week." : "No weekly opportunity used until you confirm."}</p>
                  </div>
                </div> : null}
                <AutomaticPreparationPreference id={id} saved={preparation.standingFocus as Focus} draft={automaticFocus}
                  pending={standing.isPending} onDraftChange={setStandingDraft} onSave={saveStandingFocus} />
              </section>}
          {fitness?.injury ? <p className="text-muted-foreground text-xs">Physical status: {fitness.availability.toLowerCase()} · {fitness.injury.rehabWeeksRemaining} rehab weeks remaining. Recovery can reduce fatigue while injured; rehabilitation advances through normal week progression.</p> : null}
          {preparation.ready ? <p className="text-muted-foreground text-xs">Tournament readiness is pending for the next event you actually play.</p> : null}
          {actionError ? <p role="alert" className="text-destructive text-sm">{actionError}</p> : null}
          {confirmAbandon ? <AbandonTrainingDialog pending={finalizing} onCancel={() => setConfirmAbandon(false)} onConfirm={abandonSession} /> : null}
        </> : null}
      </div>
    </main>
  </div>;
}
