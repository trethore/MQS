import type { KeyboardEvent } from "react";
import { useCallback, useEffect, useState } from "react";

import { getBridgeErrorMessage } from "@/bridge/core/bridgeError";
import type { ConsoleSnapshot } from "@/bridge/contracts/console";
import {
  executeConsoleCommand,
  fetchConsoleSnapshot,
  subscribeToConsoleCleared,
  subscribeToConsoleMessage,
  subscribeToConsoleSnapshotUpdated,
} from "@/bridge/services/consoleService";

const EMPTY_SNAPSHOT: ConsoleSnapshot = {
  messages: [],
  commandHistory: [],
  messageCount: 0,
};

export function useConsoleController() {
  const [snapshot, setSnapshot] = useState<ConsoleSnapshot>(EMPTY_SNAPSHOT);
  const [inputValue, setInputValue] = useState("");
  const [historyCursor, setHistoryCursor] = useState(-1);
  const [draftInput, setDraftInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [executing, setExecuting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const applySnapshot = useCallback((nextSnapshot: ConsoleSnapshot) => {
    setSnapshot(nextSnapshot);
    setErrorMessage(null);
  }, []);

  useEffect(() => {
    let active = true;
    let unsubscribeMessage: (() => void) | null = null;
    let unsubscribeCleared: (() => void) | null = null;
    let unsubscribeSnapshotUpdated: (() => void) | null = null;

    try {
      unsubscribeMessage = subscribeToConsoleMessage((message) => {
        if (!active) {
          return;
        }

        setSnapshot((previousSnapshot) => {
          const nextMessages = [...previousSnapshot.messages, message];
          return {
            ...previousSnapshot,
            messages: nextMessages,
            messageCount: nextMessages.length,
          };
        });
      });

      unsubscribeCleared = subscribeToConsoleCleared((event) => {
        if (!active || !event.cleared) {
          return;
        }

        setSnapshot((previousSnapshot) => {
          return {
            ...previousSnapshot,
            messages: [],
            messageCount: 0,
          };
        });
      });

      unsubscribeSnapshotUpdated = subscribeToConsoleSnapshotUpdated((nextSnapshot) => {
        if (!active) {
          return;
        }

        applySnapshot(nextSnapshot);
      });
    } catch (error) {
      if (unsubscribeMessage) {
        unsubscribeMessage();
      }

      if (unsubscribeCleared) {
        unsubscribeCleared();
      }

      if (unsubscribeSnapshotUpdated) {
        unsubscribeSnapshotUpdated();
      }

      setErrorMessage(getBridgeErrorMessage(error));
      setLoading(false);

      return () => {
        active = false;
      };
    }

    void (async () => {
      try {
        const nextSnapshot = await fetchConsoleSnapshot();
        if (!active) {
          return;
        }

        applySnapshot(nextSnapshot);
      } catch (error) {
        if (!active) {
          return;
        }

        setErrorMessage(getBridgeErrorMessage(error));
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    })();

    return () => {
      active = false;

      if (unsubscribeMessage) {
        unsubscribeMessage();
      }

      if (unsubscribeCleared) {
        unsubscribeCleared();
      }

      if (unsubscribeSnapshotUpdated) {
        unsubscribeSnapshotUpdated();
      }
    };
  }, [applySnapshot]);

  const setCommandInput = useCallback(
    (nextValue: string) => {
      setInputValue(nextValue);
      setDraftInput(nextValue);

      if (historyCursor !== -1) {
        setHistoryCursor(-1);
      }
    },
    [historyCursor],
  );

  const navigateHistory = useCallback(
    (direction: "up" | "down") => {
      const history = snapshot.commandHistory;
      if (history.length === 0) {
        return;
      }

      if (direction === "up") {
        if (historyCursor === -1) {
          setDraftInput(inputValue);
          const latestIndex = history.length - 1;
          setHistoryCursor(latestIndex);
          setInputValue(history[latestIndex]);
          return;
        }

        const previousIndex = Math.max(historyCursor - 1, 0);
        setHistoryCursor(previousIndex);
        setInputValue(history[previousIndex]);
        return;
      }

      if (historyCursor === -1) {
        return;
      }

      if (historyCursor < history.length - 1) {
        const nextIndex = historyCursor + 1;
        setHistoryCursor(nextIndex);
        setInputValue(history[nextIndex]);
        return;
      }

      setHistoryCursor(-1);
      setInputValue(draftInput);
    },
    [draftInput, historyCursor, inputValue, snapshot.commandHistory],
  );

  const handleInputKeyDown = useCallback(
    (event: KeyboardEvent<HTMLInputElement>) => {
      if (event.key === "ArrowUp") {
        event.preventDefault();
        navigateHistory("up");
        return;
      }

      if (event.key === "ArrowDown") {
        event.preventDefault();
        navigateHistory("down");
      }
    },
    [navigateHistory],
  );

  const executeInput = useCallback(async () => {
    const trimmedInput = inputValue.trim();
    if (!trimmedInput || executing) {
      return;
    }

    setExecuting(true);
    setErrorMessage(null);

    try {
      const response = await executeConsoleCommand(trimmedInput);
      if (!response.success) {
        if (response.snapshot) {
          applySnapshot(response.snapshot);
        }

        setErrorMessage(response.message || "Console command failed.");
        return;
      }

      setInputValue("");
      setDraftInput("");
      setHistoryCursor(-1);
    } catch (error) {
      setErrorMessage(getBridgeErrorMessage(error));
    } finally {
      setExecuting(false);
    }
  }, [applySnapshot, executing, inputValue]);

  return {
    errorMessage,
    executeInput,
    executing,
    handleInputKeyDown,
    inputValue,
    loading,
    setCommandInput,
    snapshot,
  };
}
