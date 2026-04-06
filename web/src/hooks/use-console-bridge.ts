import { useCallback, useEffect, useRef, useState } from 'react';

import { formatGrapheneBridgeError } from '@/bridge/core/graphene-bridge';
import {
  executeConsoleCommand,
  getConsoleSnapshot,
  subscribeToConsoleCleared,
  subscribeToConsoleMessage,
  subscribeToConsoleSnapshotUpdated,
  type ConsoleMessageItem,
  type ConsoleSnapshot,
} from '@/bridge/services/console-service';

function createEmptySnapshot(): ConsoleSnapshot {
  return {
    messages: [],
    commandHistory: [],
    messageCount: 0,
  };
}

function appendConsoleMessage(
  currentSnapshot: ConsoleSnapshot,
  message: ConsoleMessageItem
): ConsoleSnapshot {
  return {
    messages: [...currentSnapshot.messages, message],
    commandHistory: currentSnapshot.commandHistory,
    messageCount: currentSnapshot.messageCount + 1,
  };
}

export function useConsoleBridge() {
  const [snapshot, setSnapshot] = useState<ConsoleSnapshot>(() => createEmptySnapshot());
  const [isLoading, setIsLoading] = useState(true);
  const [isExecuting, setIsExecuting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const snapshotRef = useRef<ConsoleSnapshot>(createEmptySnapshot());
  const ignoredMessageEventsRef = useRef(0);

  const applySnapshot = useCallback((nextSnapshot: ConsoleSnapshot) => {
    ignoredMessageEventsRef.current = 0;
    snapshotRef.current = nextSnapshot;
    setSnapshot(nextSnapshot);
    setErrorMessage(null);
    setIsLoading(false);
  }, []);

  const handleConsoleMessage = useCallback((message: ConsoleMessageItem) => {
    if (ignoredMessageEventsRef.current > 0) {
      ignoredMessageEventsRef.current -= 1;
      setErrorMessage(null);
      setIsLoading(false);
      return;
    }

    setSnapshot((currentSnapshot) => {
      const nextSnapshot = appendConsoleMessage(currentSnapshot, message);
      snapshotRef.current = nextSnapshot;
      return nextSnapshot;
    });
    setErrorMessage(null);
    setIsLoading(false);
  }, []);

  const handleConsoleCleared = useCallback(() => {
    ignoredMessageEventsRef.current = 0;
    setSnapshot((currentSnapshot) => {
      const nextSnapshot = {
        messages: [],
        commandHistory: currentSnapshot.commandHistory,
        messageCount: 0,
      };
      snapshotRef.current = nextSnapshot;
      return nextSnapshot;
    });
    setErrorMessage(null);
    setIsLoading(false);
  }, []);

  useEffect(() => {
    let isActive = true;
    const abortController = new AbortController();
    const unsubscribers: Array<() => void> = [];
    const options = { signal: abortController.signal };

    const registerUnsubscribe = (unsubscribe: () => void) => {
      if (!isActive) {
        unsubscribe();
        return;
      }

      unsubscribers.push(unsubscribe);
    };

    const handleActiveConsoleMessage = (message: ConsoleMessageItem) => {
      if (!isActive) {
        return;
      }

      handleConsoleMessage(message);
    };

    const handleActiveConsoleCleared = () => {
      if (!isActive) {
        return;
      }

      handleConsoleCleared();
    };

    const handleActiveSnapshotUpdated = (nextSnapshot: ConsoleSnapshot) => {
      if (!isActive) {
        return;
      }

      applySnapshot(nextSnapshot);
    };

    const connectToBridge = async () => {
      try {
        registerUnsubscribe(await subscribeToConsoleMessage(handleActiveConsoleMessage, options));
        registerUnsubscribe(await subscribeToConsoleCleared(handleActiveConsoleCleared, options));
        registerUnsubscribe(
          await subscribeToConsoleSnapshotUpdated(handleActiveSnapshotUpdated, options)
        );

        const nextSnapshot = await getConsoleSnapshot(options);
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
      for (const unsubscribe of unsubscribers) {
        unsubscribe();
      }
    };
  }, [applySnapshot, handleConsoleCleared, handleConsoleMessage]);

  const handleExecuteCommand = useCallback(async (input: string) => {
    const normalizedInput = input.trim();
    if (normalizedInput.length === 0) {
      return false;
    }

    setIsExecuting(true);

    try {
      const result = await executeConsoleCommand(normalizedInput);
      const pendingDuplicateCount = Math.max(
        0,
        result.snapshot.messageCount - snapshotRef.current.messageCount
      );
      ignoredMessageEventsRef.current += pendingDuplicateCount;
      snapshotRef.current = result.snapshot;
      setSnapshot(result.snapshot);
      setErrorMessage(result.success ? null : result.message);
      return result.success;
    } catch (error) {
      setErrorMessage(formatGrapheneBridgeError(error));
      return false;
    } finally {
      setIsExecuting(false);
    }
  }, []);

  return {
    messages: snapshot.messages,
    commandHistory: snapshot.commandHistory,
    messageCount: snapshot.messageCount,
    isLoading,
    isExecuting,
    errorMessage,
    executeCommand: handleExecuteCommand,
  };
}
