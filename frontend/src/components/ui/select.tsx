import * as React from "react";
import { ChevronDown } from "lucide-react";

import { cn } from "@/lib/utils";

/*
 * Select — a native <select> re-skinned to match Input, with a chevron affordance.
 * Received props (incl. id / aria-* injected by Field) are spread onto the inner
 * <select> so label association and validation state land on the control itself.
 * Native for accessibility and simplicity; the option list is the standard affordance.
 */
const Select = React.forwardRef<HTMLSelectElement, React.ComponentProps<"select">>(
  ({ className, children, ...props }, ref) => {
    return (
      <div className="relative">
        <select
          ref={ref}
          className={cn(
            "border-border bg-surface text-foreground flex h-[var(--control-md)] w-full appearance-none rounded-md border pr-9 pl-3 text-sm transition-colors duration-[var(--duration-base)]",
            "focus:border-ring disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]",
            "aria-[invalid=true]:border-destructive",
            className,
          )}
          {...props}
        >
          {children}
        </select>
        <ChevronDown
          className="text-subtle-foreground pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2"
          aria-hidden="true"
        />
      </div>
    );
  },
);
Select.displayName = "Select";

export { Select };
