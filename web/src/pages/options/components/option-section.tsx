import type { ReactNode } from 'react';

type OptionSectionProps = {
  readonly title: string;
  readonly children?: ReactNode;
};

export function OptionSection({ title, children }: OptionSectionProps) {
  return (
    <div className="flex flex-col gap-4">
      <h3 className="text-base font-semibold tracking-tight text-primary">{title}</h3>
      {children}
    </div>
  );
}
