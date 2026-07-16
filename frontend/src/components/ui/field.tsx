import * as React from "react";

import { cn } from "@/lib/utils";
import { Label } from "@/components/ui/label";

/*
 * Field — a labelled form control with optional hint and error message.
 * Wires htmlFor/id and aria-describedby so the error is announced and the
 * control is programmatically associated with its label.
 */
interface FieldProps {
  id: string;
  label: string;
  error?: string;
  hint?: string;
  className?: string;
  children: React.ReactNode;
}

function Field({ id, label, error, hint, className, children }: FieldProps) {
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Label htmlFor={id}>{label}</Label>
      {React.isValidElement(children)
        ? React.cloneElement(children as React.ReactElement<Record<string, unknown>>, {
            id,
            "aria-invalid": error ? true : undefined,
            "aria-describedby": describedBy,
          })
        : children}
      {hint && !error ? (
        <p id={`${id}-hint`} className="text-subtle-foreground text-sm">
          {hint}
        </p>
      ) : null}
      {error ? (
        <p id={`${id}-error`} className="text-destructive text-sm">
          {error}
        </p>
      ) : null}
    </div>
  );
}

export { Field };
