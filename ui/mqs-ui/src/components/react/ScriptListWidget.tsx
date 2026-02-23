import { PowerOff, RefreshCcw } from "lucide-react";

import { Button } from "@/components/ui/button";
import { InputGroup, InputGroupInput } from "@/components/ui/input-group";
import { useScriptsController } from "@/hooks/useScriptsController";

import { ScriptEntryWidget } from "./ScriptEntryWidget";
import { ScrollbarWidget } from "./ScrollbarWidget";

export function ScriptListWidget() {
  const {
    errorMessage,
    filteredScripts,
    loading,
    noticeMessage,
    refreshAndReenableScripts,
    refreshScripts,
    searchQuery,
    setSearchQuery,
    snapshot,
    toggleScript,
    updatingAll,
    updatingScriptId,
  } = useScriptsController();

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <h1 className="text-left text-3xl font-semibold text-foreground">
        All your <span className="bg-linear-to-r from-primary to-primary-2 bg-clip-text text-transparent">QOL</span>{" "}
        Scripts!
      </h1>

      <div className="mt-5 flex items-center gap-3">
        <InputGroup className="flex-1">
          <InputGroupInput
            value={searchQuery}
            onChange={(event) => {
              setSearchQuery(event.target.value);
            }}
            placeholder="Search scripts..."
            aria-label="Search scripts"
          />
        </InputGroup>

        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Refresh scripts"
          className="shrink-0"
          onClick={() => {
            void refreshScripts();
          }}
          disabled={loading || updatingAll}
        >
          <RefreshCcw />
        </Button>
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Refresh and re-enable scripts"
          className="shrink-0"
          onClick={() => {
            void refreshAndReenableScripts();
          }}
          disabled={loading || updatingAll}
        >
          <PowerOff />
        </Button>
      </div>

      <p className="mt-2 min-h-5 text-sm text-muted-foreground" role="status">
        {errorMessage
          ? errorMessage
          : noticeMessage || `${snapshot.runningCount} / ${snapshot.totalCount} scripts running`}
      </p>

      <section className="relative mt-1 flex min-h-0 w-full flex-1 flex-col overflow-hidden pb-3">
        {loading ? (
          <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
            Loading scripts...
          </div>
        ) : (
          <ScrollbarWidget className="pl-0 pr-0 pt-1 pb-0">
            <ul className="flex min-h-full flex-col gap-3 pb-5 pr-2">
              {filteredScripts.map((script) => (
                <li key={script.id} className="w-full">
                  <ScriptEntryWidget
                    title={script.moduleName}
                    path={script.path}
                    version={script.version}
                    scriptId={script.id}
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

        <div
          aria-hidden="true"
          className="pointer-events-none absolute bottom-0 left-0 right-3 h-7 bg-linear-to-b from-card/0 via-card/30 to-card/70"
        />
      </section>
    </div>
  );
}
