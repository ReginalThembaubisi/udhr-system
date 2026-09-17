import * as React from 'react';
import { cva } from 'class-variance-authority';
import { CheckCircle2, AlertTriangle, XCircle } from 'lucide-react';

import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
  {
    variants: {
      variant: {
        default: 'border-transparent bg-primary text-primary-foreground shadow hover:bg-primary/80',
        secondary: 'border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80',
        destructive: 'border-transparent bg-destructive text-destructive-foreground shadow hover:bg-destructive/80',
        outline: 'text-foreground',
        success: 'border-transparent bg-emerald-100 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-400',
        warning: 'border-transparent bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-400',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

// Status colors (success/warning/destructive) are never the only cue — each
// pairs with a distinct icon SHAPE (circle-check / triangle / circle-x) so
// the meaning still reads for colorblind users, per WCAG 1.4.1. Pass
// icon={false} where the content already carries its own leading icon or
// emoji (avoids a redundant second glyph) or where the variant is used
// purely for neutral categorization rather than a pass/fail/caution state.
const STATUS_ICON = {
  success: CheckCircle2,
  warning: AlertTriangle,
  destructive: XCircle,
};

function Badge({ className, variant, icon = true, children, ...props }) {
  const Icon = icon && STATUS_ICON[variant];
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props}>
      {Icon && <Icon className="h-3 w-3 shrink-0" aria-hidden="true" />}
      {children}
    </div>
  );
}

export { Badge, badgeVariants };
