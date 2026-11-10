"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ArrowLeft, ArrowRight, Check, Flag, Shield, UserRound, Goal } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/ui/field";
import { Select } from "@/components/ui/select";
import {
  ArchetypeSelect,
  StartingBackgroundSelect,
} from "@/components/onboarding/archetype-select";
import {
  MAX_START_AGE,
  MIN_START_AGE,
  DEFAULT_START_AGE,
  NATIONALITIES,
  STARTING_BACKGROUNDS,
  archetypeLabel,
  backgroundLabel,
  nationalityLabel,
} from "@/lib/onboarding/options";
import { useCreateCareer } from "@/lib/api/mutations";
import { isUnauthorized } from "@/lib/api/graphql-client";
import "./create-golfer.css";

const ageRange = `Between ${MIN_START_AGE} and ${MAX_START_AGE}.`;
const schema = z.object({
  firstName: z.string().trim().min(1, "Enter a first name."),
  lastName: z.string().trim().min(1, "Enter a last name."),
  nationality: z.string().min(1, "Choose a nationality."),
  archetype: z.string().min(1, "Choose a playing style."),
  background: z.enum(STARTING_BACKGROUNDS.map((option) => option.value)),
  startAge: z
    .number({ message: `Enter an age. ${ageRange}` })
    .int()
    .min(MIN_START_AGE, ageRange)
    .max(MAX_START_AGE, ageRange),
  handedness: z.enum(["RIGHT", "LEFT"]),
});
type Values = z.infer<typeof schema>;
type Step = 1 | 2 | 3;

const countryFlags: Record<string, string> = {
  USA: "🇺🇸",
  GBR: "🇬🇧",
  IRL: "🇮🇪",
  ESP: "🇪🇸",
  ZAF: "🇿🇦",
  AUS: "🇦🇺",
  JPN: "🇯🇵",
  KOR: "🇰🇷",
  SWE: "🇸🇪",
  GER: "🇩🇪",
  FRA: "🇫🇷",
  CAN: "🇨🇦",
  ARG: "🇦🇷",
  NZL: "🇳🇿",
  ITA: "🇮🇹",
  MEX: "🇲🇽",
};

export function CreateGolferForm() {
  const router = useRouter();
  const create = useCreateCareer();
  const creating = useRef(false);
  const [step, setStep] = useState<Step>(1);
  const [confirmed, setConfirmed] = useState<Values | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const {
    register,
    handleSubmit,
    trigger,
    control,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      firstName: "",
      lastName: "",
      nationality: "",
      archetype: "",
      background: "ELITE_AMATEUR",
      startAge: DEFAULT_START_AGE,
      handedness: "RIGHT",
    },
  });

  async function nextStep() {
    if (await trigger(["firstName", "lastName", "nationality", "startAge", "handedness"]))
      setStep(2);
  }

  function showConfirmation(values: Values) {
    setConfirmed(values);
    setFormError(null);
    setStep(3);
  }

  async function startCareer() {
    if (!confirmed || creating.current) return;
    creating.current = true;
    setSubmitting(true);
    setFormError(null);
    try {
      const golfer = await create.mutateAsync(confirmed);
      router.push(`/career/${golfer.sessionId}`);
      router.refresh();
    } catch (error) {
      creating.current = false;
      setSubmitting(false);
      if (isUnauthorized(error)) {
        router.push("/login");
        router.refresh();
        return;
      }
      setFormError("Couldn't create your golfer. Please try again.");
    }
  }

  return (
    <div className={`golfer-creation golfer-creation--step-${step}`}>
      {step === 2 ? (
        <div className="golfer-creation__step-heading">
          <h1>Create your golfer</h1>
          <div className="golfer-creation__step-two-progress" aria-label="Step 2 of 2">
            <span>Step 2 of 2</span>
            <span className="golfer-creation__step-circle golfer-creation__step-circle--past">
              1
            </span>
            <span className="golfer-creation__step-line" />
            <span className="golfer-creation__step-circle golfer-creation__step-circle--current">
              2
            </span>
          </div>
        </div>
      ) : (
        <div className="golfer-creation__heading">
          <div>
            <h1>Create your golfer</h1>
            <p>
              This is the career you&apos;ll guide across decades. Choose who they are and how they
              play.
            </p>
          </div>
          {step === 1 ? (
            <span className="golfer-creation__step-pill">
              <strong>Step 1</strong> of 2
            </span>
          ) : null}
        </div>
      )}

      {step === 1 ? (
        <div className="golfer-creation__details">
          <div className="golfer-creation__details-heading">
            <h2>Your details</h2>
            <p>Set your golfer&apos;s identity and key characteristics.</p>
          </div>
          <div className="golfer-creation__details-grid">
            <Field id="firstName" label="First name" error={errors.firstName?.message}>
              <Input {...register("firstName")} autoComplete="given-name" autoFocus />
            </Field>
            <Field id="lastName" label="Last name" error={errors.lastName?.message}>
              <Input {...register("lastName")} autoComplete="family-name" />
            </Field>
            <Field id="handedness" label="Playing hand">
              <Select {...register("handedness")}>
                <option value="RIGHT">Right-handed</option>
                <option value="LEFT">Left-handed</option>
              </Select>
            </Field>
            <Field id="nationality" label="Nationality" error={errors.nationality?.message}>
              <Select {...register("nationality")}>
                <option value="" disabled>
                  Select a country
                </option>
                {NATIONALITIES.map((n) => (
                  <option key={n.value} value={n.value}>
                    {n.label}
                  </option>
                ))}
              </Select>
            </Field>
            <Field
              id="startAge"
              label="Starting age"
              error={errors.startAge?.message}
              hint={ageRange}
              className="golfer-creation__age"
            >
              <Input
                type="number"
                min={MIN_START_AGE}
                max={MAX_START_AGE}
                {...register("startAge", { valueAsNumber: true })}
              />
            </Field>
          </div>
        </div>
      ) : null}

      {step === 2 ? (
        <form onSubmit={handleSubmit(showConfirmation)} noValidate>
          <div className="golfer-creation__profile-heading">
            <h2>Your Starting Profile</h2>
            <p>
              Choose your starting background and the playing style that will define your strengths
              and approach to the game.
            </p>
          </div>
          <div className="golfer-creation__profile-grid">
            <section className="golfer-creation__choice-panel">
              <h3>Starting Background</h3>
              <Controller
                control={control}
                name="background"
                render={({ field, fieldState }) => (
                  <>
                    <StartingBackgroundSelect
                      value={field.value}
                      onValueChange={field.onChange}
                      onBlur={field.onBlur}
                      invalid={Boolean(fieldState.error)}
                      describedBy={fieldState.error ? "background-error" : undefined}
                    />
                    {fieldState.error ? (
                      <p id="background-error" className="golfer-creation__error">
                        {fieldState.error.message}
                      </p>
                    ) : null}
                  </>
                )}
              />
            </section>
            <section className="golfer-creation__choice-panel">
              <h3>Playing Style</h3>
              <Controller
                control={control}
                name="archetype"
                render={({ field, fieldState }) => (
                  <>
                    <ArchetypeSelect
                      value={field.value}
                      onValueChange={field.onChange}
                      onBlur={field.onBlur}
                      invalid={Boolean(fieldState.error)}
                      describedBy={fieldState.error ? "archetype-error" : undefined}
                    />
                    {fieldState.error ? (
                      <p id="archetype-error" className="golfer-creation__error">
                        {fieldState.error.message}
                      </p>
                    ) : null}
                  </>
                )}
              />
            </section>
          </div>
          <div className="golfer-creation__actions">
            <Button type="button" variant="ghost" onClick={() => setStep(1)}>
              <ArrowLeft aria-hidden="true" />
              Back
            </Button>
            <Button type="submit" className="golfer-creation__primary">
              Create golfer
            </Button>
          </div>
        </form>
      ) : null}

      {step === 1 ? (
        <div className="golfer-creation__actions">
          <Button asChild variant="ghost">
            <Link href="/saves">
              <ArrowLeft aria-hidden="true" />
              Back to careers
            </Link>
          </Button>
          <Button type="button" className="golfer-creation__primary" onClick={nextStep}>
            Next: Career setup <ArrowRight aria-hidden="true" />
          </Button>
        </div>
      ) : null}

      {step === 3 && confirmed ? (
        <div className="golfer-creation__confirmation-wrap">
          <div className="golfer-creation__complete-progress" aria-label="Step 2 complete">
            <span className="golfer-creation__complete-check">
              <Check aria-hidden="true" />
            </span>
            <span>Create golfer</span>
            <span className="golfer-creation__step-line" />
            <span className="golfer-creation__step-circle golfer-creation__step-circle--current">
              2
            </span>
            <strong>Step 2 complete</strong>
          </div>
          <section className="golfer-creation__confirmation">
            <p className="golfer-creation__eyebrow">Your golfer is ready</p>
            <h2>
              {confirmed.firstName} {confirmed.lastName}
            </h2>
            <p className="golfer-creation__summary-line">
              {backgroundLabel(confirmed.background)} · {archetypeLabel(confirmed.archetype)} ·{" "}
              {nationalityLabel(confirmed.nationality)} · Age {confirmed.startAge}
            </p>
            <div className="golfer-creation__tour-row">
              <span className="golfer-creation__tour-line" />
              <span className="golfer-creation__tour-pill">
                <Flag aria-hidden="true" />
                Begins on the Development tour
              </span>
              <span className="golfer-creation__tour-line" />
            </div>
            <p className="golfer-creation__confirmation-copy">
              Your career begins on the Development tour. Guide the decisions — the rest is theirs
              to earn.
            </p>
            <div className="golfer-creation__summary-cards">
              <div className="golfer-creation__summary-card">
                <Shield aria-hidden="true" />
                <span>
                  <small>Background</small>
                  <strong>{backgroundLabel(confirmed.background)}</strong>
                </span>
              </div>
              <div className="golfer-creation__summary-card">
                <Goal aria-hidden="true" />
                <span>
                  <small>Playing Style</small>
                  <strong>{archetypeLabel(confirmed.archetype)}</strong>
                </span>
              </div>
              <div className="golfer-creation__summary-card">
                <span className="golfer-creation__flag" aria-hidden="true">
                  {countryFlags[confirmed.nationality]}
                </span>
                <span>
                  <small>Nationality</small>
                  <strong>{nationalityLabel(confirmed.nationality)}</strong>
                </span>
              </div>
              <div className="golfer-creation__summary-card">
                <UserRound aria-hidden="true" />
                <span>
                  <small>Age</small>
                  <strong>{confirmed.startAge}</strong>
                </span>
              </div>
            </div>
            {formError ? (
              <p role="alert" className="golfer-creation__error">
                {formError}
              </p>
            ) : null}
            <div className="golfer-creation__confirmation-actions">
              <Button
                type="button"
                className="golfer-creation__primary"
                disabled={submitting}
                onClick={startCareer}
              >
                {submitting ? "Creating…" : "Start your career"}
              </Button>
              <Button
                type="button"
                variant="ghost"
                disabled={submitting}
                onClick={() => {
                  setFormError(null);
                  setStep(2);
                }}
              >
                Back to setup
              </Button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}
