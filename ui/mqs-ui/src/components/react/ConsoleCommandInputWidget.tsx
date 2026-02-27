import type { KeyboardEvent } from "react";

import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
  InputGroupText,
} from "@/components/ui/input-group";

interface ConsoleCommandInputWidgetProps {
  disabled: boolean;
  inputValue: string;
  onInputKeyDown: (event: KeyboardEvent<HTMLInputElement>) => void;
  onInputValueChange: (nextValue: string) => void;
  onSubmit: () => Promise<void>;
}

export function ConsoleCommandInputWidget({
  disabled,
  inputValue,
  onInputKeyDown,
  onInputValueChange,
  onSubmit,
}: ConsoleCommandInputWidgetProps) {
  return (
    <form
      className="mt-3 pb-3"
      onSubmit={(event) => {
        event.preventDefault();
        void onSubmit();
      }}
    >
      <InputGroup className="mqs-surface-shadow mqs-surface-shadow-front h-10">
        <InputGroupAddon align="inline-start">
          <InputGroupText className="font-mono text-foreground">{">"}</InputGroupText>
        </InputGroupAddon>
        <InputGroupInput
          value={inputValue}
          onChange={(event) => {
            onInputValueChange(event.target.value);
          }}
          onKeyDown={onInputKeyDown}
          placeholder="Enter a command and press Enter"
          aria-label="Console command input"
          autoComplete="off"
          spellCheck={false}
          disabled={disabled}
          className="font-mono"
        />
      </InputGroup>
    </form>
  );
}
