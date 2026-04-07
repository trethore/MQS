import { MqsButton } from '@/components/shared/mqs-button';
import { MqsInput } from '@/components/shared/mqs-input';
import { OptionSection } from '@/pages/options/components/option-section';

type TextActionFieldProps = {
  readonly title: string;
  readonly description: string;
  readonly value: string;
  readonly placeholder: string;
  readonly buttonText: string;
  readonly disabled: boolean;
  readonly onValueChange: (value: string) => void;
  readonly onBlur: () => void;
  readonly onEnter: () => void;
  readonly onButtonClick: () => void;
};

type IdeSectionProps = {
  readonly defaultIdeCommandValue: string;
  readonly defaultProjectPathValue: string;
  readonly isDefaultIdeCommandDisabled: boolean;
  readonly isDefaultProjectPathDisabled: boolean;
  readonly onDefaultIdeCommandChange: (value: string) => void;
  readonly onDefaultIdeCommandBlur: () => void;
  readonly onDefaultIdeCommandEnter: () => void;
  readonly onOpenIde: () => void;
  readonly onDefaultProjectPathChange: (value: string) => void;
  readonly onDefaultProjectPathBlur: () => void;
  readonly onDefaultProjectPathEnter: () => void;
  readonly onBrowseProjectPath: () => void;
};

function TextActionField({
  title,
  description,
  value,
  placeholder,
  buttonText,
  disabled,
  onValueChange,
  onBlur,
  onEnter,
  onButtonClick,
}: TextActionFieldProps) {
  return (
    <div className="flex w-full flex-col gap-3">
      <div className="space-y-1">
        <div className="text-sm font-semibold text-card-foreground">{title}</div>
        <p className="text-sm leading-6 text-muted-foreground">{description}</p>
      </div>

      <div className="flex w-full items-center gap-3">
        <MqsInput
          type="text"
          placeholder={placeholder}
          className="flex-1"
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

        <MqsButton type="button" className="shrink-0" disabled={disabled} onClick={onButtonClick}>
          {buttonText}
        </MqsButton>
      </div>
    </div>
  );
}

export function IdeSection({
  defaultIdeCommandValue,
  defaultProjectPathValue,
  isDefaultIdeCommandDisabled,
  isDefaultProjectPathDisabled,
  onDefaultIdeCommandChange,
  onDefaultIdeCommandBlur,
  onDefaultIdeCommandEnter,
  onOpenIde,
  onDefaultProjectPathChange,
  onDefaultProjectPathBlur,
  onDefaultProjectPathEnter,
  onBrowseProjectPath,
}: IdeSectionProps) {
  return (
    <OptionSection title="IDE">
      <div className="flex w-full flex-col gap-6">
        <TextActionField
          title="Default IDE Command"
          description="Configure the command ran for opening the IDE."
          value={defaultIdeCommandValue}
          placeholder="Command to open ide"
          buttonText="Open IDE"
          disabled={isDefaultIdeCommandDisabled}
          onValueChange={onDefaultIdeCommandChange}
          onBlur={onDefaultIdeCommandBlur}
          onEnter={onDefaultIdeCommandEnter}
          onButtonClick={onOpenIde}
        />

        <TextActionField
          title="Default Project Path"
          description="Configure the project path opened by the IDE command."
          value={defaultProjectPathValue}
          placeholder="Path to open in ide"
          buttonText="Browse"
          disabled={isDefaultProjectPathDisabled}
          onValueChange={onDefaultProjectPathChange}
          onBlur={onDefaultProjectPathBlur}
          onEnter={onDefaultProjectPathEnter}
          onButtonClick={onBrowseProjectPath}
        />
      </div>
    </OptionSection>
  );
}
