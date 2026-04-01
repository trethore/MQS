import type { ReactNode } from 'react';

import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';

type WindowProps = {
  children: ReactNode;
  navbar?: ReactNode;
  className?: string;
  contentClassName?: string;
};

export function Window({ children, navbar, className, contentClassName }: WindowProps) {
  return (
    <section
      style={{ aspectRatio: '16 / 10' }}
      className={cn(
        'flex w-[min(1120px,96vw,calc(92vh*16/10))] flex-col gap-4 overflow-hidden rounded-2xl border border-border bg-background p-4 text-foreground shadow-lg',
        className
      )}
    >
      {navbar ? <div className="shrink-0">{navbar}</div> : null}

      <Card className="min-h-0 flex-1 gap-0 overflow-hidden rounded-xl py-0 shadow-sm">
        <CardContent className={cn('flex min-h-0 flex-1 py-6', contentClassName)}>{children}</CardContent>
      </Card>
    </section>
  );
}
