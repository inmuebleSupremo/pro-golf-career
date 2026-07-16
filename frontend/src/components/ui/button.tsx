import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

/*
 * Button — shadcn/Radix pattern (Slot + cva), styled entirely from design tokens.
 * No default shadcn colours: every variant resolves to a semantic role token.
 * Focus is handled by the global :focus-visible ring in globals.css.
 */
const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md font-medium transition-colors ease-[var(--ease-out)] duration-[var(--duration-base)] disabled:pointer-events-none disabled:opacity-[var(--opacity-disabled)] [&_svg]:pointer-events-none [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        primary: "bg-primary text-primary-foreground shadow-sm hover:bg-primary-hover",
        secondary: "border border-border bg-surface text-foreground hover:bg-background",
        ghost: "text-foreground hover:bg-surface",
        destructive: "bg-destructive text-destructive-foreground hover:opacity-90",
        link: "text-accent underline-offset-4 hover:underline",
      },
      size: {
        sm: "h-[var(--control-sm)] px-3 text-sm",
        md: "h-[var(--control-md)] px-4 text-sm",
        lg: "h-[var(--control-lg)] px-6 text-sm",
        icon: "h-[var(--control-md)] w-[var(--control-md)]",
      },
    },
    defaultVariants: {
      variant: "primary",
      size: "md",
    },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return (
      <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />
    );
  },
);
Button.displayName = "Button";

export { Button, buttonVariants };
