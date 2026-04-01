import { PowerOff, RefreshCw } from 'lucide-react';

import { MqsButton } from '@/components/shared/mqs-button';
import { MqsInput } from '@/components/shared/mqs-input';

export function ScriptsPage() {
  return (
    <section className="flex h-full w-full flex-col items-start justify-start gap-4">
      <div className="w-full">
        <h2 className="text-left text-2xl font-semibold tracking-tight text-card-foreground">
          All your <span className="text-primary">QOL</span> Scripts!
        </h2>
      </div>

      <div className="flex w-full items-center gap-3">
        <MqsInput
          type="search"
          placeholder="Search a QOL script..."
          className="h-10 flex-1"
        />

        <MqsButton
          type="button"
          variant="outline"
          size="icon"
          className="h-10 w-10 text-emerald-400 hover:text-emerald-300"
          aria-label="Refresh script list"
        >
          <RefreshCw />
        </MqsButton>

        <MqsButton
          type="button"
          variant="outline"
          size="icon"
          className="h-10 w-10 text-rose-400 hover:text-rose-300"
          aria-label="Turn off all scripts"
        >
          <PowerOff />
        </MqsButton>
      </div>
    </section>
  );
}
