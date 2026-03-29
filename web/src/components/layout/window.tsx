import type { ReactNode } from 'react';

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
      style={{ aspectRatio: '16 / 9' }}
      className={cn(
        'flex w-[min(1120px,96vw,calc(92vh*16/9))] flex-col overflow-hidden rounded-2xl border border-border bg-background text-foreground shadow-lg',
        className
      )}
    >
      {navbar ? <div className="shrink-0 border-b border-border px-5 py-4">{navbar}</div> : null}

      <div className={cn('flex min-h-0 flex-1 items-center justify-center p-6', contentClassName)}>
        {children}
      </div>
    </section>
  );
}
