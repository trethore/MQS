import { useCallback, useEffect, useMemo, useState } from "react";

import { getBridgeErrorMessage } from "@/bridge/core/bridgeError";
import type { ScriptOperation, ScriptsSnapshot } from "@/bridge/contracts/scripts";
import {
  disableAllScripts as requestDisableAllScripts,
  fetchScriptsSnapshot,
  refreshAndReenableScripts as requestRefreshAndReenableScripts,
  refreshScripts as requestRefreshScripts,
  subscribeToScriptsUpdated,
  toggleScript as requestToggleScript,
} from "@/bridge/services/scriptsService";

const EMPTY_SNAPSHOT: ScriptsSnapshot = {
  scripts: [],
  runningCount: 0,
  totalCount: 0,
};

function areSnapshotsEqual(leftSnapshot: ScriptsSnapshot, rightSnapshot: ScriptsSnapshot): boolean {
  if (
    leftSnapshot.runningCount !== rightSnapshot.runningCount ||
    leftSnapshot.totalCount !== rightSnapshot.totalCount ||
    leftSnapshot.scripts.length !== rightSnapshot.scripts.length
  ) {
    return false;
  }

  for (let index = 0; index < leftSnapshot.scripts.length; index += 1) {
    const leftScript = leftSnapshot.scripts[index];
    const rightScript = rightSnapshot.scripts[index];

    if (
      leftScript.id !== rightScript.id ||
      leftScript.moduleName !== rightScript.moduleName ||
      leftScript.version !== rightScript.version ||
      leftScript.mainClass !== rightScript.mainClass ||
      leftScript.path !== rightScript.path ||
      leftScript.running !== rightScript.running
    ) {
      return false;
    }
  }

  return true;
}

export function useScriptsController() {
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
    setSnapshot((previousSnapshot) => {
      return areSnapshotsEqual(previousSnapshot, nextSnapshot) ? previousSnapshot : nextSnapshot;
    });
    setErrorMessage(null);
  }, []);

  const requestSnapshot = useCallback(async () => {
    try {
      const nextSnapshot = await fetchScriptsSnapshot();
      applySnapshot(nextSnapshot);
    } catch (error) {
      setErrorMessage(getBridgeErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [applySnapshot]);

  useEffect(() => {
    let unsubscribe: (() => void) | null = null;

    try {
      unsubscribe = subscribeToScriptsUpdated((nextSnapshot) => {
        applySnapshot(nextSnapshot);
      });
    } catch (error) {
      setErrorMessage(getBridgeErrorMessage(error));
      setLoading(false);
      return;
    }

    void requestSnapshot();

    return () => {
      if (unsubscribe) {
        unsubscribe();
      }
    };
  }, [applySnapshot, requestSnapshot]);

  const runOperation = useCallback(
    async (operationRequest: () => Promise<ScriptOperation>, successNotice: string) => {
      try {
        const operation = await operationRequest();

        if (operation.snapshot) {
          applySnapshot(operation.snapshot);
        }

        if (!operation.success) {
          throw new Error(operation.message || "Script operation failed.");
        }

        setNoticeMessage(operation.message || successNotice);
      } catch (error) {
        setErrorMessage(getBridgeErrorMessage(error));
      }
    },
    [applySnapshot],
  );

  const toggleScript = useCallback(
    async (scriptId: string) => {
      setUpdatingScriptId(scriptId);
      setNoticeMessage(null);
      await runOperation(() => requestToggleScript(scriptId), "Script toggled.");
      setUpdatingScriptId(null);
    },
    [runOperation],
  );

  const refreshScripts = useCallback(async () => {
    setUpdatingAll(true);
    setNoticeMessage(null);
    await runOperation(requestRefreshScripts, "Scripts refreshed.");
    setUpdatingAll(false);
  }, [runOperation]);

  const refreshAndReenableScripts = useCallback(async () => {
    setUpdatingAll(true);
    setNoticeMessage(null);
    await runOperation(requestRefreshAndReenableScripts, "Scripts refreshed and re-enabled.");
    setUpdatingAll(false);
  }, [runOperation]);

  const disableAllScripts = useCallback(async () => {
    setUpdatingAll(true);
    setNoticeMessage(null);
    await runOperation(requestDisableAllScripts, "All running scripts disabled.");
    setUpdatingAll(false);
  }, [runOperation]);

  return {
    disableAllScripts,
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
  };
}
