import { PowerOff, RefreshCcw } from "lucide-react";

import { Button } from "@/components/ui/button";
import { InputGroup, InputGroupInput } from "@/components/ui/input-group";
import { useScriptsController } from "@/hooks/useScriptsController";

import { ScriptEntryWidget } from "./ScriptEntryWidget";
import { ScrollbarWidget } from "./ScrollbarWidget";

export function ScriptListWidget() {
  const {
    disableAllScripts,
    errorMessage,
    filteredScripts,
    loading,
    refreshAndReenableScripts,
    searchQuery,
    setSearchQuery,
    toggleScript,
    updatingAll,
    updatingScriptId,
  } = useScriptsController();

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <h1 className="text-left text-3xl font-semibold text-foreground">
        All your{" "}
        <span className="bg-linear-to-r from-primary to-primary-2 bg-clip-text text-transparent">
          QOL
        </span>{" "}
        Scripts!
      </h1>

      <div className="relative z-10 mt-5 flex items-center gap-3">
        <InputGroup className="mqs-surface-shadow mqs-surface-shadow-front flex-1">
          <InputGroupInput
            value={searchQuery}
            onChange={(event) => {
              setSearchQuery(event.target.value);
            }}
            placeholder="Search a QOL script..."
            aria-label="Search scripts"
          />
        </InputGroup>

        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Refresh and re-enable scripts"
          className="mqs-surface-shadow mqs-surface-shadow-front shrink-0 text-success hover:bg-success/10 hover:text-success"
          onClick={() => {
            void refreshAndReenableScripts();
          }}
          disabled={loading || updatingAll}
        >
          <RefreshCcw />
        </Button>
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Disable all scripts"
          className="mqs-surface-shadow mqs-surface-shadow-front shrink-0 text-destructive hover:bg-destructive/10 hover:text-destructive"
          onClick={() => {
            void disableAllScripts();
          }}
          disabled={loading || updatingAll}
        >
          <PowerOff />
        </Button>
      </div>

      {errorMessage ? (
        <p
          role="alert"
          className="mt-3 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
        >
          {errorMessage}
        </p>
      ) : null}

      <section className="mt-3 flex min-h-0 w-full flex-1 flex-col pb-3">
        {loading ? (
          <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
            Loading scripts...
          </div>
        ) : (
          <ScrollbarWidget className="-ml-3 pl-3 pt-1 pb-0">
            <ul className="flex min-h-full flex-col gap-3 pb-5 pr-2">
              {filteredScripts.map((script) => (
                <li key={script.id} className="w-full">
                  <ScriptEntryWidget
                    title={script.scriptName}
                    path={script.path}
                    version={script.version}
                    running={script.running}
                    disabled={updatingAll || updatingScriptId === script.id}
                    onToggle={() => {
                      void toggleScript(script.id);
                    }}
                  />
                </li>
              ))}

              {filteredScripts.length === 0 ? (
                <li className="rounded-xl border border-border bg-background/40 px-5 py-4 text-sm text-muted-foreground">
                  No scripts match your search.
                </li>
              ) : null}
            </ul>
          </ScrollbarWidget>
        )}

      </section>
    </div>
  );
}
