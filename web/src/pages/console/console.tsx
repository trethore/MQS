import { useRef, useState, type ChangeEvent, type ComponentProps, type KeyboardEvent } from 'react';

import { ConsoleOutput } from '@/components/shared/console-output';
import { MqsInput } from '@/components/shared/mqs-input';
import { useAutoFocusInput } from '@/hooks/use-auto-focus-input';
import { useConsoleBridge } from '@/hooks/use-console-bridge';
import { cn } from '@/lib/utils';

type ConsolePageProps = {
  readonly commandValue: string;
  readonly onCommandValueChange: (value: string) => void;
};

function getStatusMessage(errorMessage: string | null): string | null {
  return errorMessage;
}

export function ConsolePage({ commandValue, onCommandValueChange }: ConsolePageProps) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [historyIndex, setHistoryIndex] = useState<number | null>(null);
  const [historyDraft, setHistoryDraft] = useState('');
  const { messages, commandHistory, isLoading, isExecuting, errorMessage, executeCommand } =
    useConsoleBridge();

  useAutoFocusInput(inputRef);

  const statusMessage = getStatusMessage(errorMessage);
  const statusMessageClassName = errorMessage
    ? 'text-destructive dark:text-rose-300'
    : 'text-muted-foreground';

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const nextValue = event.target.value;
    onCommandValueChange(nextValue);
    setHistoryDraft(nextValue);
    setHistoryIndex(null);
  };

  const navigateHistory = (direction: 'up' | 'down') => {
    if (commandHistory.length === 0) {
      return;
    }

    const activeHistoryIndex =
      historyIndex == null ? null : Math.min(historyIndex, commandHistory.length - 1);

    if (direction === 'up') {
      if (activeHistoryIndex == null) {
        const nextIndex = commandHistory.length - 1;
        setHistoryDraft(commandValue);
        setHistoryIndex(nextIndex);
        onCommandValueChange(commandHistory[nextIndex]);
        return;
      }

      const nextIndex = Math.max(0, activeHistoryIndex - 1);
      setHistoryIndex(nextIndex);
      onCommandValueChange(commandHistory[nextIndex]);
      return;
    }

    if (activeHistoryIndex == null) {
      return;
    }

    if (activeHistoryIndex < commandHistory.length - 1) {
      const nextIndex = activeHistoryIndex + 1;
      setHistoryIndex(nextIndex);
      onCommandValueChange(commandHistory[nextIndex]);
      return;
    }

    setHistoryIndex(null);
    onCommandValueChange(historyDraft);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      navigateHistory('up');
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      navigateHistory('down');
    }
  };

  const handleSubmit: NonNullable<ComponentProps<'form'>['onSubmit']> = (event) => {
    event.preventDefault();

    const nextCommand = commandValue.trim();
    if (nextCommand.length === 0 || isExecuting) {
      return;
    }

    onCommandValueChange('');
    setHistoryDraft('');
    setHistoryIndex(null);
    executeCommand(nextCommand);
  };

  return (
    <section className="flex h-full min-h-0 w-full flex-col gap-3">
      <div className="w-full">
        <h2 className="text-left text-2xl font-semibold tracking-tight text-card-foreground">
          Console
        </h2>
        {statusMessage ? (
          <p className={cn('mt-1 text-sm', statusMessageClassName)}>{statusMessage}</p>
        ) : null}
      </div>

      <ConsoleOutput messages={messages} isLoading={isLoading} />

      <form className="shrink-0" onSubmit={handleSubmit}>
        <label htmlFor="console-command-input" className="sr-only">
          Console command
        </label>
        <div className="relative">
          <span className="pointer-events-none absolute top-1/2 left-3.5 -translate-y-1/2 font-mono text-sm font-semibold text-primary">
            &gt;
          </span>
          <MqsInput
            id="console-command-input"
            ref={inputRef}
            type="text"
            autoComplete="off"
            spellCheck={false}
            value={commandValue}
            placeholder="Enter a command and press Enter"
            className="h-11 pl-8 font-mono text-sm"
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
          />
        </div>
      </form>
    </section>
  );
}
