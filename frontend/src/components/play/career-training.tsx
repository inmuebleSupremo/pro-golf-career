"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowRight, Check, CircleHelp, Flag, HeartPulse, Target, TrendingUp } from "lucide-react";

import { PracticeFacilityHeader } from "@/components/play/practice-facility-nav";
import { Button } from "@/components/ui/button";
import {
  useCareerTraining,
  useCommitCareerPreparation,
  useSetStandingPreparationFocus,
} from "@/lib/api/career-training";
import { cn } from "@/lib/utils";

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
const percent = (value: number) => `${Number((value * 100).toFixed(1))}%`;
const attributeLabel = (attribute: string) => attribute.replaceAll("_", " ").toLowerCase();

/** The same backend purchase options expose each relevant next-growth cost and current growth limits. */
export function nextTrainingGain(focus: Focus, banked: number, options: readonly GrowthOption[]) {
  const attributes = FOCUS_ATTRIBUTES[focus];
  const eligible = options.filter((option) => attributes.includes(option.attribute)
    && option.potentialHeadroom > 0 && option.remainingSeasonal > 0
    && option.nextCost != null && option.nextCost > 0);
  eligible.sort((a, b) => a.rating - b.rating
    || attributes.indexOf(a.attribute) - attributes.indexOf(b.attribute));
  const threshold = eligible[0]?.nextCost;
  return threshold == null ? null : {
    banked,
    threshold,
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

function TrainingDetails({ focus, rules, banked, threshold }: { focus: Focus; rules: Rules; banked?: number; threshold?: number }) {
  return <details className="border-border text-muted-foreground mt-4 border-t pt-3 text-xs">
    <summary className="text-foreground cursor-pointer font-semibold">Training details</summary>
    {technical(focus) ? <div className="mt-2 space-y-1 leading-relaxed">
      <p>Base progress {progressNumber(rules.trainingBaseProgress)} + neutral simulated bonus {progressNumber(rules.simulatedBonusProgress)} (7.5%) = {progressNumber(rules.totalSimulatedProgress)} focus progress.</p>
      {threshold != null ? <p>Banked progress: {progressNumber(banked ?? 0)} of {threshold} needed for the next eligible attribute improvement. Existing potential and seasonal limits apply.</p> : <p>Further attribute growth is currently limited by potential or this season&apos;s allowance. Banked progress is retained.</p>}
      <p>Training adds up to {percent(rules.trainingFatigue)} fatigue. Playable sessions may earn up to 15% extra when they arrive in Milestone 3.</p>
    </div> : focus === "RECOVERY" ? <p className="mt-2">Recovery reduces fatigue by up to {percent(rules.recoveryReduction)} and awards no training progress.</p>
      : <p className="mt-2">Readiness support is +{rules.readinessSupport.toFixed(2)} for the next played tournament. It survives skipped events and does not stack.</p>}
  </details>;
}

export function CareerTrainingConfirmation({ focus, rules, season, week, pending, onBack, onCommit }: {
  focus: Focus; rules: Rules; season: number; week: number; pending: boolean;
  onBack: () => void; onCommit: () => void;
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
        <div className="border-border bg-background rounded-xl border p-4 opacity-65" aria-label="Play Session unavailable">
          <p className="text-foreground text-lg font-semibold">Play Session</p>
          <p className="text-muted-foreground mt-2 text-sm">Eight varied challenges in the existing practice environment, with a possible extra performance bonus.</p>
          <p className="text-muted-foreground mt-4 text-xs font-semibold">Coming in Milestone 3</p>
        </div>
        <div className="border-primary bg-primary/[0.06] rounded-xl border p-4">
          <p className="text-foreground text-lg font-semibold">Simulate Session</p>
          <p className="text-muted-foreground mt-2 text-sm">Complete this week&apos;s technical training instantly. Your golfer gains focus progress and a neutral simulated bonus.</p>
          <p className="text-primary mt-4 text-xs font-semibold">Available now · Normal fatigue applies</p>
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
          <p className="text-muted-foreground mt-1 text-xs">Free Practice remains available afterward.</p></div>
        <div className="flex flex-wrap gap-2"><Button variant="secondary" disabled={pending} onClick={onBack}>Back</Button>
          <Button disabled={pending} onClick={onCommit}>{pending ? "Committing…" : technical(focus) ? "Simulate training" : "Confirm preparation"}<ArrowRight className="size-4" aria-hidden="true" /></Button></div>
      </div>
    </div>
  </section>;
}

export function CareerTrainingReview({ outcome, id, onOverview }: { outcome: Outcome; id: string; onOverview: () => void }) {
  const choice = choiceFor(outcome.focus);
  const isTechnical = technical(choice.key);
  const fatigueRecovered = Math.max(0, outcome.fatigueBefore - outcome.fatigueAfter);
  const headline = isTechnical ? "Good work this week." : choice.key === "RECOVERY" ? "Recovery complete." : "Ready for the next one.";
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
      <div className="grid gap-3 p-4 sm:grid-cols-2 sm:p-6">
        <div className="border-border bg-background rounded-lg border p-4">
          <p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">{isTechnical ? "Permanent development" : "Preparation benefit"}</p>
          <h3 className="text-foreground mt-2 text-lg font-semibold">{choice.label}</h3>
          {isTechnical ? <><p className="text-primary mt-2 text-2xl font-semibold">+{progressNumber(outcome.progressAwarded)} <span className="text-muted-foreground text-sm font-normal">focus progress</span></p>
            {outcome.attributeImprovements.length ? <ul className="text-foreground mt-2 text-sm">{outcome.attributeImprovements.map((change) => <li key={`${change.attribute}:${change.season}`}>{attributeLabel(change.attribute)} +{change.delta}</li>)}</ul>
              : <p className="text-muted-foreground mt-2 text-sm">Progress is banked toward the next eligible skill gain.</p>}</>
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
        <div className="mt-5 flex flex-wrap items-center justify-between gap-3"><p className="text-muted-foreground max-w-lg text-xs">Season {outcome.season}, week {outcome.week} is complete. Free Practice remains unlimited and career-neutral.</p>
          <div className="flex flex-wrap gap-2"><Button variant="secondary" asChild><Link href={`/career/${id}/practice`}>Explore Free Practice</Link></Button>
            <Button onClick={onOverview}>Back to overview <ArrowRight className="size-4" aria-hidden="true" /></Button></div></div>
      </div>
    </div>
  </section>;
}

export function CareerTraining({ id }: { id: string }) {
  const query = useCareerTraining(id);
  const standing = useSetStandingPreparationFocus(id);
  const commit = useCommitCareerPreparation(id);
  const [selectedFocus, setSelectedFocus] = useState<Focus | null>(null);
  const [stage, setStage] = useState<"choose" | "confirm" | "result">("choose");
  const [stageWeek, setStageWeek] = useState<string | null>(null);
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const world = query.data?.world;
  const preparation = query.data?.careerPreparation;
  const fitness = query.data?.playerFitness;
  const retired = query.data?.playerProfile?.retired;
  const focus = selectedFocus ?? (preparation?.standingFocus as Focus | undefined) ?? "DRIVING";
  const choice = choiceFor(focus);
  const weekKey = world ? `${world.season}:${world.week}` : null;
  const activeStage = stageWeek === weekKey ? stage : "choose";
  const resultForWeek = Boolean(world && outcome?.committed && outcome.season === world.season && outcome.week === world.week);
  const used = Boolean(resultForWeek || (world && preparation && preparation.consumedSeason === world.season && preparation.consumedWeek === world.week));
  const available = Boolean(preparation?.available && !retired && !used);
  const usedFocus = resultForWeek ? outcome?.focus : preparation?.committedFocus;
  const banked = preparation?.progress.find((entry) => entry.focus === focus)?.amount ?? 0;
  const gain = query.data?.playerDevelopment ? nextTrainingGain(focus, banked, query.data.playerDevelopment.options) : null;

  async function saveStandingFocus() {
    setActionError(null);
    try {
      const saved = await standing.mutateAsync(focus);
      if (!saved) setActionError("The standing focus could not be changed. Refresh the current career and try again.");
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "The standing focus could not be changed.");
    }
  }

  async function commitPreparation() {
    setActionError(null);
    try {
      const result = await commit.mutateAsync(focus);
      if (!result.committed) {
        setStage("choose");
        setActionError("This week's preparation is no longer available. The current status has been refreshed.");
        await query.refetch();
        return;
      }
      setOutcome(result);
      setStageWeek(`${result.season}:${result.week}`);
      setStage("result");
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Preparation could not be committed. Refresh the weekly status before retrying.");
      await query.refetch();
    }
  }

  function focusCard(item: Choice) {
    const Icon = item.icon;
    return <button key={item.key} type="button" aria-pressed={focus === item.key}
      onClick={() => { setSelectedFocus(item.key); setActionError(null); }}
      className={cn("border-border bg-background hover:border-primary/60 flex min-h-28 items-start gap-3 rounded-lg border p-3 text-left transition-colors focus-visible:outline-2 focus-visible:outline-ring", technical(item.key) ? "flex-col" : "min-h-20", focus === item.key && "border-primary bg-primary/[0.07]")}>
      <span className="bg-surface-3 text-primary grid size-8 shrink-0 place-items-center rounded-md"><Icon className="size-4" aria-hidden="true" /></span>
      <span><span className="text-foreground block text-sm font-semibold">{item.label}</span><span className="text-muted-foreground mt-1 block text-xs">{item.short}</span></span>
    </button>;
  }

  return <div className="practice-facility-page bg-background fixed inset-0 z-[var(--z-modal)] isolate flex h-dvh min-h-0 min-w-0 flex-col overflow-hidden">
    <PracticeFacilityHeader id={id} active="training" title="Career Training" subtitle="Weekly preparation for your golfer" />
    <main className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-3 py-4 sm:px-5">
      <div className="mx-auto max-w-7xl space-y-5 pb-6">
        {query.isLoading ? <p role="status" className="text-muted-foreground">Loading career preparation…</p> : null}
        {query.isError ? <div role="alert" className="border-destructive bg-surface rounded-lg border p-4"><p className="text-foreground">Career preparation could not be loaded.</p><Button className="mt-3" variant="secondary" onClick={() => query.refetch()}>Try again</Button></div> : null}
        {!query.isError && preparation && world ? <>
          <div className="border-border bg-surface-2 grid gap-3 rounded-xl border px-4 py-3 text-sm sm:grid-cols-2 xl:grid-cols-[auto_auto_auto_1fr] xl:items-center xl:gap-5">
            <div><p className="text-subtle-foreground text-[10px] font-bold tracking-widest uppercase">Career calendar</p><p className="text-foreground mt-1 font-semibold">Season {world.season} · Week {world.week}</p></div>
            <div><p className="text-subtle-foreground text-[10px] font-bold tracking-widest uppercase">This week&apos;s preparation</p><p className={cn("mt-1 font-semibold", available ? "text-primary" : "text-foreground")}>{used ? "Completed" : world.hasPendingEvent ? "Tournament play pending" : retired ? "Career complete" : available ? "Available to use" : "Unavailable"}</p></div>
            <div><p className="text-subtle-foreground text-[10px] font-bold tracking-widest uppercase">Golfer fatigue</p><p className="text-foreground mt-1 font-semibold">{fitness ? percent(fitness.fatigue) : "—"}</p>{fitness?.injury ? <p className="text-muted-foreground text-xs">{fitness.injury.severity.toLowerCase()} {fitness.injury.type.toLowerCase()} injury</p> : null}</div>
            <div className="xl:justify-self-end"><p className="text-subtle-foreground text-[10px] font-bold tracking-widest uppercase">Automatic focus</p><p className="text-foreground mt-1 font-semibold">{choiceFor(preparation.standingFocus).label}</p></div>
          </div>

          {activeStage === "result" && outcome ? <CareerTrainingReview outcome={outcome} id={id} onOverview={() => setStage("choose")} />
            : activeStage === "confirm" && available ? <CareerTrainingConfirmation focus={focus} rules={preparation.rules} season={world.season} week={world.week} pending={commit.isPending} onBack={() => setStage("choose")} onCommit={commitPreparation} />
              : <section aria-labelledby="training-choice" className="space-y-4">
                <div className="flex flex-wrap items-end justify-between gap-3">
                  <div><p className="text-subtle-foreground text-xs font-bold tracking-widest uppercase">Career Training / Weekly preparation</p>
                    <h1 id="training-choice" className="text-foreground mt-1 text-2xl font-semibold">{used ? "Preparation complete." : "What would you like your golfer to work on this week?"}</h1>
                    <p className="text-muted-foreground mt-1 text-sm">{used ? "This week is settled. Choose a focus for future automatic preparation." : "Choose a focus now. The week is used only when you confirm."}</p></div>
                  <Steps current={used ? 2 : 0} />
                </div>

                {used ? <div className="border-primary/40 bg-primary/[0.06] flex flex-wrap items-center gap-3 rounded-xl border p-4">
                  <span className="bg-primary/15 text-primary grid size-10 shrink-0 place-items-center rounded-lg"><Check className="size-5" aria-hidden="true" /></span>
                  <div className="min-w-0 flex-1"><p className="text-foreground font-semibold">{choiceFor(usedFocus).label} complete</p><p className="text-muted-foreground text-sm">Your weekly opportunity is used. Another session cannot earn a second reward.</p></div>
                  {outcome && outcome.season === world.season && outcome.week === world.week ? <Button variant="secondary" onClick={() => setStage("result")}>Review this result</Button> : null}
                </div> : null}
                {world.hasPendingEvent && !used ? <p role="status" className="border-border bg-surface rounded-lg border p-3 text-sm">Tournament play is pending. Finish the current event before committing preparation.</p> : null}
                {retired ? <p role="status" className="border-border bg-surface rounded-lg border p-3 text-sm">This golfer has retired from career preparation.</p> : null}
                {!available && !used && !world.hasPendingEvent && !retired ? <p role="status" className="border-border bg-surface rounded-lg border p-3 text-sm">Preparation is unavailable for this career week. You can still change the automatic focus or use Free Practice.</p> : null}

                <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1.85fr)_minmax(300px,0.8fr)]">
                  <div className="border-border bg-surface rounded-xl border p-4 sm:p-5">
                    <div className="flex flex-wrap items-baseline justify-between gap-2"><h2 className="text-foreground font-semibold">{used ? "Set a future focus" : "Choose your preparation"}</h2><span className="text-muted-foreground text-xs">{used ? "For eligible upcoming weeks" : "One choice per career week"}</span></div>
                    <p className="text-subtle-foreground mt-4 text-[10px] font-bold tracking-widest uppercase">Technical development</p>
                    <div className="mt-2 grid grid-cols-2 gap-2 xl:grid-cols-4">{FOCUSES.filter((item) => technical(item.key)).map(focusCard)}</div>
                    <div className="border-border my-4 border-t" />
                    <p className="text-subtle-foreground text-[10px] font-bold tracking-widest uppercase">Preparation & condition</p>
                    <div className="mt-2 grid gap-2 sm:grid-cols-2">{FOCUSES.filter((item) => !technical(item.key)).map(focusCard)}</div>
                    <p className="text-muted-foreground mt-4 text-xs">{used ? "Changing the saved focus is free, even after this week is complete." : "Free Practice is unlimited and has no career consequences."}</p>
                  </div>

                  <div className="space-y-3">
                    <div className="border-primary/30 bg-surface-2 rounded-xl border p-4 sm:p-5">
                      <p className="text-subtle-foreground text-[10px] font-bold tracking-widest uppercase">{used ? "Plan ahead" : "Your selected focus"}</p>
                      <h2 className="text-foreground mt-2 text-xl font-semibold">{choice.label}</h2>
                      <p className="text-muted-foreground mt-2 text-sm">{choice.description}</p>
                      <div className="mt-3 flex flex-wrap gap-2 text-xs font-semibold">
                        <span className="border-border text-foreground rounded-md border px-2 py-1">{technical(focus) ? "Permanent growth" : choice.benefit}</span>
                        {technical(focus) ? <span className="border-border text-foreground rounded-md border px-2 py-1">Small fatigue cost</span> : null}
                      </div>
                      {technical(focus) ? <div className="mt-4">
                        {gain ? <><div className="text-muted-foreground flex justify-between gap-2 text-xs"><span>Progress toward next skill gain</span><span className="text-foreground font-semibold">{Math.round(gain.percentage)}%</span></div>
                          <div role="progressbar" aria-label={`${choice.label} progress toward next skill gain`} aria-valuemin={0} aria-valuemax={gain.threshold} aria-valuenow={Math.min(gain.banked, gain.threshold)} className="bg-surface-3 mt-2 h-2 overflow-hidden rounded-full"><div className="bg-info h-full rounded-full" style={{ width: `${gain.percentage}%` }} /></div>
                          <p className="text-muted-foreground mt-2 text-xs">Progress builds across focused training weeks.</p></>
                          : <p className="text-muted-foreground text-xs">Attribute growth has reached a potential or seasonal limit. Banked progress is retained.</p>}
                      </div> : null}
                      <TrainingDetails focus={focus} rules={preparation.rules} banked={banked} threshold={gain?.threshold} />
                      {used ? <><Button className="mt-4 w-full" disabled={standing.isPending || focus === preparation.standingFocus} onClick={saveStandingFocus}>{focus === preparation.standingFocus ? "Focus already saved" : standing.isPending ? "Saving…" : "Use as future focus"}<ArrowRight className="size-4" aria-hidden="true" /></Button>
                        <p className="text-muted-foreground mt-2 text-center text-xs">Changes future automatic preparation only.</p></>
                        : <><Button className="mt-4 w-full" disabled={!available} onClick={() => { setStageWeek(weekKey); setStage("confirm"); setActionError(null); }}>Continue <ArrowRight className="size-4" aria-hidden="true" /></Button>
                          <p className="text-muted-foreground mt-2 text-center text-xs">No weekly opportunity used until you confirm.</p></>}
                    </div>
                    <div className="border-border bg-surface rounded-xl border p-4 text-sm">
                      <div className="flex items-start gap-3"><CircleHelp className="text-info mt-0.5 size-5 shrink-0" aria-hidden="true" />
                        <div><h3 className="text-foreground font-semibold">When you simulate career weeks</h3><p className="text-muted-foreground mt-1 text-xs">Your saved focus applies automatically to eligible unspent weeks.</p>
                          {focus !== preparation.standingFocus ? <button type="button" disabled={standing.isPending} onClick={saveStandingFocus} className="text-primary mt-2 text-xs font-semibold underline disabled:opacity-50">Save {choice.label} as automatic focus</button> : null}</div></div>
                    </div>
                  </div>
                </div>
              </section>}
          {fitness?.injury ? <p className="text-muted-foreground text-xs">Physical status: {fitness.availability.toLowerCase()} · {fitness.injury.rehabWeeksRemaining} rehab weeks remaining. Recovery is available if the weekly opportunity is unspent.</p> : null}
          {preparation.ready ? <p className="text-muted-foreground text-xs">Tournament readiness is pending for the next event you actually play.</p> : null}
          {actionError ? <p role="alert" className="text-destructive text-sm">{actionError}</p> : null}
          <p className="border-border text-muted-foreground border-t pt-3 text-xs">Free Practice remains unlimited and career-neutral. <Link className="text-primary underline" href={`/career/${id}/practice`}>Driving Range</Link> · <Link className="text-primary underline" href={`/career/${id}/practice/putting`}>Putting Green</Link></p>
        </> : null}
      </div>
    </main>
  </div>;
}
