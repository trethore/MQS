import { useMemo, useRef, type ReactNode } from 'react';
import { PowerOff, RefreshCw } from 'lucide-react';

import type { ScriptListItem } from '@/bridge/services/scripts-service';
import { MqsButton } from '@/components/shared/mqs-button';
import { MqsInput } from '@/components/shared/mqs-input';
import { MqsList } from '@/components/shared/mqs-list';
import { ScriptEntry } from '@/components/shared/script-entry';
import { Card } from '@/components/ui/card';
import { useAutoFocusInput } from '@/hooks/use-auto-focus-input';
import { useScriptsBridge } from '@/hooks/use-scripts-bridge';
import { cn } from '@/lib/utils';

type ScriptsPageProps = {
  readonly searchValue: string;
  readonly onSearchValueChange: (value: string) => void;
};

function getStatusMessage(errorMessage: string | null): string | null {
  return errorMessage;
}

function renderScriptEntries(
  scripts: ReadonlyArray<ScriptListItem>,
  pendingScriptIds: ReadonlySet<string>,
  isRefreshing: boolean,
  isDisablingAll: boolean,
  toggleScript: (scriptId: string) => void
): Array<ReactNode> {
  return scripts.map((script) => {
    const isPending = pendingScriptIds.has(script.id) || isRefreshing || isDisablingAll;
    return (
      <ScriptEntry
        key={script.id}
        name={script.name}
        version={script.version}
        path={script.path}
        enabled={script.enabled}
        disabled={isPending}
        onEnabledChange={() => {
          toggleScript(script.id);
        }}
      />
    );
  });
}

function renderListContent(options: {
  isLoading: boolean;
  showBridgeErrorState: boolean;
  showSearchEmptyState: boolean;
  showScriptsEmptyState: boolean;
  errorMessage: string | null;
  filteredScripts: ReadonlyArray<ScriptListItem>;
  pendingScriptIds: ReadonlySet<string>;
  isRefreshing: boolean;
  isDisablingAll: boolean;
  toggleScript: (scriptId: string) => void;
}): ReactNode {
  if (options.isLoading) {
    return null;
  }

  if (options.showBridgeErrorState) {
    return (
      <div className="rounded-xl border border-dashed border-destructive/40 bg-destructive/5 px-5 py-8 text-center text-sm text-destructive dark:border-destructive/30 dark:bg-destructive/10 dark:text-rose-300">
        {options.errorMessage}
      </div>
    );
  }

  if (options.filteredScripts.length > 0) {
    return renderScriptEntries(
      options.filteredScripts,
      options.pendingScriptIds,
      options.isRefreshing,
      options.isDisablingAll,
      options.toggleScript
    );
  }

  if (options.showSearchEmptyState) {
    return (
      <Card className="w-full max-w-md gap-0 rounded-2xl px-6 py-10 text-center text-sm text-muted-foreground shadow-xs">
        No scripts match your search.
      </Card>
    );
  }

  if (options.showScriptsEmptyState) {
    return (
      <div className="rounded-xl border border-dashed border-border px-5 py-8 text-center text-sm text-muted-foreground dark:border-input">
        No scripts were discovered by MQS.
      </div>
    );
  }

  return null;
}

export function ScriptsPage({ searchValue, onSearchValueChange }: ScriptsPageProps) {
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const {
    scripts,
    runningCount,
    totalCount,
    isLoading,
    errorMessage,
    pendingScriptIds,
    isRefreshing,
    isDisablingAll,
    toggleScript,
    refreshScripts,
    disableAllScripts,
  } = useScriptsBridge();

  useAutoFocusInput(searchInputRef);

  const filteredScripts = useMemo(() => {
    const normalizedSearch = searchValue.trim().toLowerCase();

    if (!normalizedSearch) {
      return scripts;
    }

    return scripts.filter((script) => {
      return (
        script.name.toLowerCase().includes(normalizedSearch) ||
        (script.version ?? '').toLowerCase().includes(normalizedSearch) ||
        script.path.toLowerCase().includes(normalizedSearch)
      );
    });
  }, [scripts, searchValue]);

  const statusMessage = getStatusMessage(errorMessage);

  const showBridgeErrorState = !isLoading && errorMessage !== null && totalCount === 0;
  const showSearchEmptyState =
    !isLoading &&
    !showBridgeErrorState &&
    filteredScripts.length === 0 &&
    searchValue.trim().length > 0;
  const showScriptsEmptyState =
    !isLoading && !showBridgeErrorState && filteredScripts.length === 0 && totalCount === 0;
  const listContentClassName = showSearchEmptyState
    ? 'min-h-full items-center justify-center'
    : undefined;
  const statusMessageClassName = errorMessage
    ? 'text-destructive dark:text-rose-300'
    : 'text-muted-foreground';
  const listContent = renderListContent({
    isLoading,
    showBridgeErrorState,
    showSearchEmptyState,
    showScriptsEmptyState,
    errorMessage,
    filteredScripts,
    pendingScriptIds,
    isRefreshing,
    isDisablingAll,
    toggleScript,
  });

  return (
    <section className="flex h-full min-h-0 w-full flex-col items-start justify-start gap-4">
      <div className="w-full">
        <h2 className="text-left text-2xl font-semibold tracking-tight text-card-foreground">
          All your <span className="text-primary">QOL</span> Scripts!
        </h2>
        {statusMessage ? (
          <p className={cn('mt-1 text-sm', statusMessageClassName)}>{statusMessage}</p>
        ) : null}
      </div>

      <div className="flex w-full items-center gap-3">
        <MqsInput
          ref={searchInputRef}
          type="text"
          placeholder="Search a QOL script..."
          className="h-10 flex-1"
          value={searchValue}
          onChange={(event) => onSearchValueChange(event.target.value)}
        />

        <MqsButton
          type="button"
          variant="outline"
          size="icon"
          className="h-10 w-10 text-emerald-400 hover:text-emerald-300"
          aria-label="Refresh script list"
          disabled={isLoading || isRefreshing}
          onClick={() => {
            refreshScripts();
          }}
        >
          <RefreshCw className={cn(isRefreshing && 'animate-spin')} />
        </MqsButton>

        <MqsButton
          type="button"
          variant="outline"
          size="icon"
          className="h-10 w-10 text-rose-400 hover:text-rose-300"
          aria-label="Turn off all scripts"
          disabled={isLoading || isDisablingAll || runningCount === 0}
          onClick={() => {
            disableAllScripts();
          }}
        >
          <PowerOff />
        </MqsButton>
      </div>

      <MqsList className="w-full" contentClassName={listContentClassName}>
        {listContent}
      </MqsList>
    </section>
  );
}
