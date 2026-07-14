"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { motion, useReducedMotion } from "motion/react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/ui/field";
import { Select } from "@/components/ui/select";
import { ArchetypeSelect } from "@/components/onboarding/archetype-select";
import {
  MAX_START_AGE,
  MIN_START_AGE,
  DEFAULT_START_AGE,
  NATIONALITIES,
  archetypeLabel,
  nationalityLabel,
} from "@/lib/onboarding/options";
import { useCreateCareer, type CreatedGolfer } from "@/lib/api/mutations";
import { isUnauthorized } from "@/lib/api/graphql-client";

const ageRange = `Between ${MIN_START_AGE} and ${MAX_START_AGE}.`;

const schema = z.object({
  firstName: z.string().trim().min(1, "Enter a first name."),
  lastName: z.string().trim().min(1, "Enter a last name."),
  nationality: z.string().min(1, "Choose a nationality."),
  archetype: z.string().min(1, "Choose a playing style."),
  startAge: z
    .number({ message: `Enter an age. ${ageRange}` })
    .int()
    .min(MIN_START_AGE, ageRange)
    .max(MAX_START_AGE, ageRange),
});
type Values = z.infer<typeof schema>;

export function CreateGolferForm() {
  const router = useRouter();
  const create = useCreateCareer();
  const [created, setCreated] = useState<CreatedGolfer | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      firstName: "",
      lastName: "",
      nationality: "",
      archetype: "",
      startAge: DEFAULT_START_AGE,
    },
  });

  async function onSubmit(values: Values) {
    setFormError(null);
    try {
      const golfer = await create.mutateAsync(values);
      setCreated(golfer);
    } catch (error) {
      if (isUnauthorized(error)) {
        router.push("/login");
        router.refresh();
        return;
      }
      setFormError("Couldn't create your golfer. Please try again.");
    }
  }

  if (created) return <CareerCreated golfer={created} />;

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-6">
      <div className="grid gap-4 sm:grid-cols-2">
        <Field id="firstName" label="First name" error={errors.firstName?.message}>
          <Input {...register("firstName")} autoComplete="given-name" autoFocus />
        </Field>
        <Field id="lastName" label="Last name" error={errors.lastName?.message}>
          <Input {...register("lastName")} autoComplete="family-name" />
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
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
        <Field id="startAge" label="Starting age" error={errors.startAge?.message} hint={ageRange}>
          <Input
            type="number"
            min={MIN_START_AGE}
            max={MAX_START_AGE}
            className="max-w-28"
            {...register("startAge", { valueAsNumber: true })}
          />
        </Field>
      </div>

      <Controller
        control={control}
        name="archetype"
        render={({ field, fieldState }) => (
          <div className="flex flex-col gap-2.5">
            <span className="text-foreground text-sm font-medium">Playing style</span>
            <ArchetypeSelect
              value={field.value}
              onValueChange={field.onChange}
              onBlur={field.onBlur}
              invalid={Boolean(fieldState.error)}
              describedBy={fieldState.error ? "archetype-error" : undefined}
            />
            {fieldState.error ? (
              <p id="archetype-error" className="text-destructive text-sm">
                {fieldState.error.message}
              </p>
            ) : null}
          </div>
        )}
      />

      {formError ? (
        <p role="alert" className="text-destructive text-sm">
          {formError}
        </p>
      ) : null}

      <div className="flex items-center gap-3">
        <Button type="submit" size="lg" disabled={create.isPending}>
          {create.isPending ? "Creating…" : "Create golfer"}
        </Button>
        <Button asChild variant="ghost" size="lg">
          <Link href="/saves">Cancel</Link>
        </Button>
      </div>
    </form>
  );
}

function CareerCreated({ golfer }: { golfer: CreatedGolfer }) {
  const router = useRouter();
  const reduce = useReducedMotion();

  return (
    <motion.div
      initial={reduce ? false : { opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
      className="flex flex-col items-center gap-6 text-center"
    >
      <div className="flex flex-col gap-2">
        <p className="text-subtle-foreground font-mono text-xs tracking-[0.18em] uppercase">
          Your golfer is ready
        </p>
        <h2 className="text-foreground font-serif text-4xl font-medium tracking-[-0.02em]">
          {golfer.firstName} {golfer.lastName}
        </h2>
      </div>
      <p className="text-muted-foreground">
        {archetypeLabel(golfer.archetype)} · {nationalityLabel(golfer.nationality)} · age{" "}
        {golfer.startAge}
      </p>
      <p className="text-muted-foreground max-w-sm text-sm">
        Their career begins on the Development tour. Guide the decisions — the rest is theirs to
        earn.
      </p>
      <Button
        size="lg"
        onClick={() => {
          router.push("/saves");
          router.refresh();
        }}
      >
        View your saves
      </Button>
    </motion.div>
  );
}
