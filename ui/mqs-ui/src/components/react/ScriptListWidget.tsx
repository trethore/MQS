import { useCallback, useEffect, useMemo, useState } from "react";
import { PowerOff, RefreshCcw } from "lucide-react";

import { Button } from "@/components/ui/button";
import { InputGroup, InputGroupInput } from "@/components/ui/input-group";

import { ScriptEntryWidget } from "./ScriptEntryWidget";
import { ScrollbarWidget } from "./ScrollbarWidget";

interface GrapheneBridgeApi {
  request: (channel: string, payload?: unknown) => Promise<unknown>;
  on: (channel: string, listener: (payload: unknown) => void) => () => void;
}

interface ScriptState {
  id: string;
  moduleName: string;
  version: string;
  mainClass: string;
  path: string;
  running: boolean;
}

interface ScriptsSnapshot {
  scripts: ScriptState[];
  runningCount: number;
  totalCount: number;
}

interface ScriptOperation {
  success: boolean;
  message: string;
  snapshot: ScriptsSnapshot | null;
}

declare global {
  interface Window {
    grapheneBridge?: GrapheneBridgeApi;
  }
}

const CHANNEL_LIST = "mqs:scripts:list";
const CHANNEL_TOGGLE = "mqs:scripts:toggle";
const CHANNEL_REFRESH = "mqs:scripts:refresh";
const CHANNEL_REFRESH_AND_REENABLE = "mqs:scripts:refresh-and-reenable";
const EVENT_UPDATED = "mqs:scripts:updated";

const EMPTY_SNAPSHOT: ScriptsSnapshot = {
  scripts: [],
  runningCount: 0,
  totalCount: 0,
};

const BRIDGE_UNAVAILABLE_MESSAGE =
  "Graphene bridge is unavailable. Open this page from MQS in Minecraft.";

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asBoolean(value: unknown): boolean {
  return typeof value === "boolean" ? value : false;
}

function asFiniteNumber(value: unknown, fallback: number): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  return fallback;
}

function parseScriptState(rawScript: unknown): ScriptState | null {
  if (!isObject(rawScript)) {
    return null;
  }

  const id = asString(rawScript.id);
  if (!id) {
    return null;
  }

  const moduleName = asString(rawScript.moduleName);
  return {
    id,
    moduleName: moduleName || id,
    version: asString(rawScript.version),
    mainClass: asString(rawScript.mainClass),
    path: asString(rawScript.path),
    running: asBoolean(rawScript.running),
  };
}

function parseSnapshot(rawSnapshot: unknown): ScriptsSnapshot | null {
  if (!isObject(rawSnapshot) || !Array.isArray(rawSnapshot.scripts)) {
    return null;
  }

  const scripts = rawSnapshot.scripts
    .map((rawScript) => parseScriptState(rawScript))
    .filter((script): script is ScriptState => script !== null);

  const defaultRunningCount = scripts.filter((script) => script.running).length;

  return {
    scripts,
    runningCount: asFiniteNumber(rawSnapshot.runningCount, defaultRunningCount),
    totalCount: asFiniteNumber(rawSnapshot.totalCount, scripts.length),
  };
}

function parseOperation(rawOperation: unknown): ScriptOperation | null {
  if (!isObject(rawOperation)) {
    return null;
  }

  return {
    success: asBoolean(rawOperation.success),
    message: asString(rawOperation.message),
    snapshot: parseSnapshot(rawOperation.snapshot),
  };
}

function getBridge(): GrapheneBridgeApi | null {
  if (typeof window === "undefined") {
    return null;
  }

  const bridge = window.grapheneBridge;
  if (!bridge) {
    return null;
  }

  if (typeof bridge.request !== "function" || typeof bridge.on !== "function") {
    return null;
  }

  return bridge;
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }

  return "Bridge request failed.";
}

export function ScriptListWidget() {
  const [snapshot, setSnapshot] = useState<ScriptsSnapshot>(EMPTY_SNAPSHOT);
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [updatingAll, setUpdatingAll] = useState(false);
  const [updatingScriptId, setUpdatingScriptId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [noticeMessage, setNoticeMessage] = useState<string | null>(null);

  const filteredScripts = useMemo(() => {
    const trimmedQuery = searchQuery.trim().toLowerCase();
    if (!trimmedQuery) {
      return snapshot.scripts;
    }

    return snapshot.scripts.filter((script) => {
      return (
        script.moduleName.toLowerCase().includes(trimmedQuery) ||
        script.id.toLowerCase().includes(trimmedQuery) ||
        script.path.toLowerCase().includes(trimmedQuery) ||
        script.mainClass.toLowerCase().includes(trimmedQuery)
      );
    });
  }, [searchQuery, snapshot.scripts]);

  const applySnapshot = useCallback((nextSnapshot: ScriptsSnapshot) => {
    setSnapshot(nextSnapshot);
    setErrorMessage(null);
  }, []);

  const requestSnapshot = useCallback(async () => {
    const bridge = getBridge();
    if (!bridge) {
      setErrorMessage(BRIDGE_UNAVAILABLE_MESSAGE);
      setLoading(false);
      return;
    }

    try {
      const rawSnapshot = await bridge.request(CHANNEL_LIST, null);
      const parsedSnapshot = parseSnapshot(rawSnapshot);
      if (!parsedSnapshot) {
        throw new Error("Invalid scripts snapshot payload.");
      }

      applySnapshot(parsedSnapshot);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [applySnapshot]);

  useEffect(() => {
    const bridge = getBridge();
    if (!bridge) {
      setErrorMessage(BRIDGE_UNAVAILABLE_MESSAGE);
      setLoading(false);
      return;
    }

    const unsubscribe = bridge.on(EVENT_UPDATED, (rawSnapshot) => {
      const parsedSnapshot = parseSnapshot(rawSnapshot);
      if (!parsedSnapshot) {
        return;
      }

      applySnapshot(parsedSnapshot);
    });

    void requestSnapshot();

    return () => {
      unsubscribe();
    };
  }, [applySnapshot, requestSnapshot]);

  const runOperation = useCallback(
    async (channel: string, payload: unknown, successNotice: string) => {
      const bridge = getBridge();
      if (!bridge) {
        setErrorMessage(BRIDGE_UNAVAILABLE_MESSAGE);
        return;
      }

      try {
        const rawOperation = await bridge.request(channel, payload);
        const operation = parseOperation(rawOperation);
        if (!operation) {
          throw new Error("Invalid scripts operation payload.");
        }

        if (operation.snapshot) {
          applySnapshot(operation.snapshot);
        }

        if (!operation.success) {
          throw new Error(operation.message || "Script operation failed.");
        }

        setNoticeMessage(operation.message || successNotice);
      } catch (error) {
        setErrorMessage(getErrorMessage(error));
      }
    },
    [applySnapshot],
  );

  const toggleScript = useCallback(
    async (scriptId: string) => {
      setUpdatingScriptId(scriptId);
      setNoticeMessage(null);
      await runOperation(CHANNEL_TOGGLE, { scriptId }, "Script toggled.");
      setUpdatingScriptId(null);
    },
    [runOperation],
  );

  const refreshScripts = useCallback(async () => {
    setUpdatingAll(true);
    setNoticeMessage(null);
    await runOperation(CHANNEL_REFRESH, null, "Scripts refreshed.");
    setUpdatingAll(false);
  }, [runOperation]);

  const refreshAndReenableScripts = useCallback(async () => {
    setUpdatingAll(true);
    setNoticeMessage(null);
    await runOperation(CHANNEL_REFRESH_AND_REENABLE, null, "Scripts refreshed and re-enabled.");
    setUpdatingAll(false);
  }, [runOperation]);

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
