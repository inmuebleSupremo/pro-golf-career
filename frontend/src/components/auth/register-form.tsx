"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/ui/field";

const schema = z.object({
  username: z.string().min(3, "Username must be at least 3 characters."),
  password: z.string().min(8, "Password must be at least 8 characters."),
});
type Values = z.infer<typeof schema>;

export function RegisterForm() {
  const [formError, setFormError] = useState<string | null>(null);
  const [created, setCreated] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Values>({ resolver: zodResolver(schema) });

  async function onSubmit(values: Values) {
    setFormError(null);
    const res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(values),
    });
    if (!res.ok) {
      const data = (await res.json().catch(() => null)) as { error?: string } | null;
      setFormError(data?.error ?? "Couldn't create your account. Try again.");
      return;
    }
    setCreated(true);
  }

  if (created) {
    return (
      <div className="flex flex-col items-center gap-4 text-center">
        <p className="text-foreground font-serif text-xl">Account created</p>
        <p className="text-muted-foreground text-sm">You can sign in now.</p>
        <Button asChild size="lg" className="w-full">
          <Link href="/login">Sign in</Link>
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
      <Field id="username" label="Username" error={errors.username?.message}>
        <Input {...register("username")} autoComplete="username" autoFocus />
      </Field>
      <Field
        id="password"
        label="Password"
        error={errors.password?.message}
        hint="At least 8 characters."
      >
        <Input type="password" {...register("password")} autoComplete="new-password" />
      </Field>
      {formError ? (
        <p role="alert" className="text-destructive text-sm">
          {formError}
        </p>
      ) : null}
      <Button type="submit" size="lg" disabled={isSubmitting} className="mt-1 w-full">
        {isSubmitting ? "Creating account…" : "Create account"}
      </Button>
    </form>
  );
}
