import * as React from "react";

import { cn } from "@/lib/utils";

/*
 * Input — token-styled text field. Mouse focus is shown via the border-ring
 * change; keyboard focus additionally gets the global :focus-visible ring.
 * `aria-invalid` drives the error border so validation state is visible.
 */
const Input = React.forwardRef<HTMLInputElement, React.ComponentProps<"input">>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        type={type}
        ref={ref}
        className={cn(
          "border-border bg-surface text-foreground flex h-[var(--control-md)] w-full rounded-md border px-3 text-sm transition-colors duration-[var(--duration-base)]",
          "placeholder:text-subtle-foreground focus:border-ring",
          "disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]",
          "aria-[invalid=true]:border-destructive",
          className,
        )}
        {...props}
      />
    );
  },
);
Input.displayName = "Input";

export { Input };
