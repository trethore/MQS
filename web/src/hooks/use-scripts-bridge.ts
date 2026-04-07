import { useCallback, useEffect, useState } from 'react';

import { formatGrapheneBridgeError } from '@/bridge/core/graphene-bridge';
import {
  disableAllScripts,
  listScripts,
  refreshAndReenableScripts,
  subscribeToScriptsUpdated,
  toggleScript,
  type ScriptOperationResult,
  type ScriptsSnapshot,
} from '@/bridge/services/scripts-service';

function createEmptySnapshot(): ScriptsSnapshot {
  return {
    scripts: [],
    runningCount: 0,
    totalCount: 0,
  };
}

function addPendingId(currentIds: ReadonlySet<string>, scriptId: string): ReadonlySet<string> {
  const nextIds = new Set(currentIds);
  nextIds.add(scriptId);
  return nextIds;
}

function removePendingId(currentIds: ReadonlySet<string>, scriptId: string): ReadonlySet<string> {
  const nextIds = new Set(currentIds);
  nextIds.delete(scriptId);
  return nextIds;
}

export function useScriptsBridge() {
  const [snapshot, setSnapshot] = useState<ScriptsSnapshot>(() => createEmptySnapshot());
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string>();
  const [pendingScriptIds, setPendingScriptIds] = useState<ReadonlySet<string>>(new Set<string>());
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isDisablingAll, setIsDisablingAll] = useState(false);

  const applySnapshot = useCallback((nextSnapshot: ScriptsSnapshot) => {
    setSnapshot(nextSnapshot);
    setErrorMessage(undefined);
    setIsLoading(false);
  }, []);

  const applyOperationResult = useCallback((result: ScriptOperationResult) => {
    setSnapshot(result.snapshot);
    setErrorMessage(result.success ? undefined : result.message);
  }, []);

  useEffect(() => {
    let isActive = true;
    let unsubscribe = () => {};
    const abortController = new AbortController();

    const connectToBridge = async () => {
      try {
        unsubscribe = await subscribeToScriptsUpdated(
          (nextSnapshot) => {
            if (!isActive) {
              return;
            }

            applySnapshot(nextSnapshot);
          },
          { signal: abortController.signal }
        );

        const nextSnapshot = await listScripts({ signal: abortController.signal });
        if (!isActive) {
          return;
        }

        applySnapshot(nextSnapshot);
      } catch (error) {
        if (!isActive || (error instanceof Error && error.name === 'AbortError')) {
          return;
        }

        setErrorMessage(formatGrapheneBridgeError(error));
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    };

    void connectToBridge();

    return () => {
      isActive = false;
      abortController.abort();
      unsubscribe();
    };
  }, [applySnapshot]);

  const handleToggleScript = useCallback(
    async (scriptId: string) => {
      setPendingScriptIds((currentIds) => addPendingId(currentIds, scriptId));

      try {
        const result = await toggleScript(scriptId);
        applyOperationResult(result);
      } catch (error) {
        setErrorMessage(formatGrapheneBridgeError(error));
      } finally {
        setPendingScriptIds((currentIds) => removePendingId(currentIds, scriptId));
      }
    },
    [applyOperationResult]
  );

  const handleRefreshScripts = useCallback(async () => {
    setIsRefreshing(true);

    try {
      const result = await refreshAndReenableScripts();
      applyOperationResult(result);
    } catch (error) {
      setErrorMessage(formatGrapheneBridgeError(error));
    } finally {
      setIsRefreshing(false);
    }
  }, [applyOperationResult]);

  const handleDisableAllScripts = useCallback(async () => {
    setIsDisablingAll(true);

    try {
      const result = await disableAllScripts();
      applyOperationResult(result);
    } catch (error) {
      setErrorMessage(formatGrapheneBridgeError(error));
    } finally {
      setIsDisablingAll(false);
    }
  }, [applyOperationResult]);

  return {
    scripts: snapshot.scripts,
    runningCount: snapshot.runningCount,
    totalCount: snapshot.totalCount,
    isLoading,
    errorMessage,
    pendingScriptIds,
    isRefreshing,
    isDisablingAll,
    toggleScript: handleToggleScript,
    refreshScripts: handleRefreshScripts,
    disableAllScripts: handleDisableAllScripts,
  };
}
