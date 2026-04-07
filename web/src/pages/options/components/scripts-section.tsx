import { FolderOpen } from 'lucide-react';

import { MqsButton } from '@/components/shared/mqs-button';
import { MqsInput } from '@/components/shared/mqs-input';
import { OptionSection } from '@/pages/options/components/option-section';

type ScriptsSectionProps = {
  readonly value: string;
  readonly disabled: boolean;
  readonly onValueChange: (value: string) => void;
  readonly onBlur: () => void;
  readonly onEnter: () => void;
  readonly onPickDirectory: () => void;
};

export function ScriptsSection({
  value,
  disabled,
  onValueChange,
  onBlur,
  onEnter,
  onPickDirectory,
}: ScriptsSectionProps) {
  return (
    <OptionSection title="Scripts">
      <div className="flex w-full flex-col gap-3">
        <div className="space-y-1">
          <div className="text-sm font-semibold text-card-foreground">
            Additional Script Directories
          </div>
          <p className="text-sm leading-6 text-muted-foreground">
            Configure extra script folders separated by ;.
          </p>
        </div>

        <div className="relative w-full">
          <MqsInput
            type="text"
            placeholder="Directories separated by ;"
            className="w-full pr-12"
            value={value}
            disabled={disabled}
            onChange={(event) => {
              onValueChange(event.target.value);
            }}
            onBlur={onBlur}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault();
                onEnter();
              }
            }}
          />

          <MqsButton
            type="button"
            variant="ghost"
            size="icon-sm"
            className="absolute top-1/2 right-1.5 -translate-y-1/2 bg-transparent text-muted-foreground shadow-none hover:bg-transparent hover:text-foreground dark:hover:bg-transparent"
            aria-label="Pick additional script directory"
            disabled={disabled}
            onClick={onPickDirectory}
          >
            <FolderOpen />
          </MqsButton>
        </div>
      </div>
    </OptionSection>
  );
}
