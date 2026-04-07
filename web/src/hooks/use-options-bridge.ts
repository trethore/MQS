import { useCallback, useEffect, useRef, useState } from 'react';

import { formatGrapheneBridgeError } from '@/bridge/core/graphene-bridge';
import {
  getOptionsSnapshot,
  openOptionsPath,
  subscribeToOptionsUpdated,
  updateOptions,
  type OptionsSnapshot,
  type OptionsUpdateResult,
} from '@/bridge/services/options-service';

type BooleanOptionKey = 'logRedirect' | 'allowAllClasses';

function createEmptySnapshot(): OptionsSnapshot {
  return {
    logRedirect: false,
    allowAllClasses: false,
    defaultIdeCommand: '',
    defaultProjectPath: '',
    additionalScriptDirectories: [],
    defaultScriptDirectory: '',
  };
}

function addPendingKey(
  currentKeys: ReadonlySet<BooleanOptionKey>,
  optionKey: BooleanOptionKey
): ReadonlySet<BooleanOptionKey> {
  const nextKeys = new Set(currentKeys);
  nextKeys.add(optionKey);
  return nextKeys;
}

function removePendingKey(
  currentKeys: ReadonlySet<BooleanOptionKey>,
  optionKey: BooleanOptionKey
): ReadonlySet<BooleanOptionKey> {
  const nextKeys = new Set(currentKeys);
  nextKeys.delete(optionKey);
  return nextKeys;
}

export function useOptionsBridge() {
  const [snapshot, setSnapshot] = useState<OptionsSnapshot>(() => createEmptySnapshot());
  const [hasLoadedSnapshot, setHasLoadedSnapshot] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string>();
  const [pendingOptionKeys, setPendingOptionKeys] = useState<ReadonlySet<BooleanOptionKey>>(
    new Set<BooleanOptionKey>()
  );
  const [isSavingDefaultIdeCommand, setIsSavingDefaultIdeCommand] = useState(false);
  const [isSavingDefaultProjectPath, setIsSavingDefaultProjectPath] = useState(false);
  const [isSavingAdditionalScriptDirectories, setIsSavingAdditionalScriptDirectories] =
    useState(false);
  const [isPickingDefaultProjectPath, setIsPickingDefaultProjectPath] = useState(false);
  const [isPickingAdditionalScriptDirectory, setIsPickingAdditionalScriptDirectory] =
    useState(false);
  const [isOpeningIde, setIsOpeningIde] = useState(false);
  const snapshotRef = useRef<OptionsSnapshot>(createEmptySnapshot());

  const applySnapshot = useCallback((nextSnapshot: OptionsSnapshot) => {
    snapshotRef.current = nextSnapshot;
    setSnapshot(nextSnapshot);
    setHasLoadedSnapshot(true);
    setErrorMessage(undefined);
    setIsLoading(false);
  }, []);

  const applyUpdateResult = useCallback((result: OptionsUpdateResult) => {
    snapshotRef.current = result.options;
    setSnapshot(result.options);
    setHasLoadedSnapshot(true);
    setErrorMessage(result.success ? undefined : result.message);
  }, []);

  useEffect(() => {
    let isActive = true;
    let unsubscribe = () => {};
    const abortController = new AbortController();

    const connectToBridge = async () => {
      try {
        unsubscribe = await subscribeToOptionsUpdated(
          (nextSnapshot) => {
            if (!isActive) {
              return;
            }

            applySnapshot(nextSnapshot);
          },
          { signal: abortController.signal }
        );

        const nextSnapshot = await getOptionsSnapshot({ signal: abortController.signal });
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

  const handleSetBooleanOption = useCallback(
    async (optionKey: BooleanOptionKey, enabled: boolean) => {
      const previousSnapshot = snapshotRef.current;
      const nextSnapshot = {
        ...previousSnapshot,
        [optionKey]: enabled,
      } satisfies OptionsSnapshot;

      snapshotRef.current = nextSnapshot;
      setSnapshot(nextSnapshot);
      setHasLoadedSnapshot(true);
      setErrorMessage(undefined);
      setPendingOptionKeys((currentKeys) => addPendingKey(currentKeys, optionKey));

      try {
        const result = await updateOptions(
          optionKey === 'logRedirect' ? { logRedirect: enabled } : { allowAllClasses: enabled }
        );
        applyUpdateResult(result);
        return result.success;
      } catch (error) {
        snapshotRef.current = previousSnapshot;
        setSnapshot(previousSnapshot);
        setHasLoadedSnapshot(true);
        setErrorMessage(formatGrapheneBridgeError(error));
        return false;
      } finally {
        setPendingOptionKeys((currentKeys) => removePendingKey(currentKeys, optionKey));
      }
    },
    [applyUpdateResult]
  );

  const handleSetDefaultIdeCommand = useCallback(
    async (command: string) => {
      const previousSnapshot = snapshotRef.current;
      const nextSnapshot = {
        ...previousSnapshot,
        defaultIdeCommand: command.trim(),
      } satisfies OptionsSnapshot;

      snapshotRef.current = nextSnapshot;
      setSnapshot(nextSnapshot);
      setHasLoadedSnapshot(true);
      setErrorMessage(undefined);
      setIsSavingDefaultIdeCommand(true);

      try {
        const result = await updateOptions({ defaultIdeCommand: command });
        applyUpdateResult(result);
        return result.success;
      } catch (error) {
        snapshotRef.current = previousSnapshot;
        setSnapshot(previousSnapshot);
        setHasLoadedSnapshot(true);
        setErrorMessage(formatGrapheneBridgeError(error));
        return false;
      } finally {
        setIsSavingDefaultIdeCommand(false);
      }
    },
    [applyUpdateResult]
  );

  const handleSetDefaultProjectPath = useCallback(
    async (path: string) => {
      const previousSnapshot = snapshotRef.current;
      const nextSnapshot = {
        ...previousSnapshot,
        defaultProjectPath: path.trim(),
      } satisfies OptionsSnapshot;

      snapshotRef.current = nextSnapshot;
      setSnapshot(nextSnapshot);
      setHasLoadedSnapshot(true);
      setErrorMessage(undefined);
      setIsSavingDefaultProjectPath(true);

      try {
        const result = await updateOptions({ defaultProjectPath: path });
        applyUpdateResult(result);
        return result.success;
      } catch (error) {
        snapshotRef.current = previousSnapshot;
        setSnapshot(previousSnapshot);
        setHasLoadedSnapshot(true);
        setErrorMessage(formatGrapheneBridgeError(error));
        return false;
      } finally {
        setIsSavingDefaultProjectPath(false);
      }
    },
    [applyUpdateResult]
  );

  const handleSetAdditionalScriptDirectories = useCallback(
    async (directories: Array<string>) => {
      const previousSnapshot = snapshotRef.current;
      const nextSnapshot = {
        ...previousSnapshot,
        additionalScriptDirectories: [...directories],
      } satisfies OptionsSnapshot;

      snapshotRef.current = nextSnapshot;
      setSnapshot(nextSnapshot);
      setHasLoadedSnapshot(true);
      setErrorMessage(undefined);
      setIsSavingAdditionalScriptDirectories(true);

      try {
        const result = await updateOptions({ additionalScriptDirectories: directories });
        applyUpdateResult(result);
        return result.success;
      } catch (error) {
        snapshotRef.current = previousSnapshot;
        setSnapshot(previousSnapshot);
        setHasLoadedSnapshot(true);
        setErrorMessage(formatGrapheneBridgeError(error));
        return false;
      } finally {
        setIsSavingAdditionalScriptDirectories(false);
      }
    },
    [applyUpdateResult]
  );

  const handlePickDefaultProjectPath = useCallback(async () => {
    setErrorMessage(undefined);
    setIsPickingDefaultProjectPath(true);

    try {
      const pickResult = await openOptionsPath({
        path: snapshotRef.current.defaultProjectPath,
        target: 'picker',
      });

      if (!pickResult.success) {
        setErrorMessage(pickResult.message);
        return false;
      }

      if (pickResult.openedPath.trim().length === 0) {
        return true;
      }

      return await handleSetDefaultProjectPath(pickResult.openedPath);
    } catch (error) {
      setErrorMessage(formatGrapheneBridgeError(error));
      return false;
    } finally {
      setIsPickingDefaultProjectPath(false);
    }
  }, [handleSetDefaultProjectPath]);

  const handlePickAdditionalScriptDirectory = useCallback(async (path?: string) => {
    setErrorMessage(undefined);
    setIsPickingAdditionalScriptDirectory(true);

    try {
      const pickResult = await openOptionsPath({
        path: path && path.trim().length > 0 ? path : snapshotRef.current.defaultScriptDirectory,
        target: 'picker',
      });

      if (!pickResult.success) {
        setErrorMessage(pickResult.message);
        return;
      }

      return pickResult.openedPath.trim().length > 0 ? pickResult.openedPath : undefined;
    } catch (error) {
      setErrorMessage(formatGrapheneBridgeError(error));
      return;
    } finally {
      setIsPickingAdditionalScriptDirectory(false);
    }
  }, []);

  const handleOpenIde = useCallback(async (command: string) => {
    setErrorMessage(undefined);
    setIsOpeningIde(true);

    try {
      const openResult = await openOptionsPath({
        path: snapshotRef.current.defaultProjectPath,
        defaultIdeCommand: command,
      });
      setErrorMessage(openResult.success ? undefined : openResult.message);
      return openResult.success;
    } catch (error) {
      setErrorMessage(formatGrapheneBridgeError(error));
      return false;
    } finally {
      setIsOpeningIde(false);
    }
  }, []);

  const isOptionPending = useCallback(
    (optionKey: BooleanOptionKey) => {
      return pendingOptionKeys.has(optionKey);
    },
    [pendingOptionKeys]
  );

  return {
    options: snapshot,
    hasLoadedSnapshot,
    isLoading,
    errorMessage,
    pendingOptionKeys,
    isSavingDefaultIdeCommand,
    isSavingDefaultProjectPath,
    isSavingAdditionalScriptDirectories,
    isPickingDefaultProjectPath,
    isPickingAdditionalScriptDirectory,
    isOpeningIde,
    isOptionPending,
    setBooleanOption: handleSetBooleanOption,
    setDefaultIdeCommand: handleSetDefaultIdeCommand,
    setDefaultProjectPath: handleSetDefaultProjectPath,
    setAdditionalScriptDirectories: handleSetAdditionalScriptDirectories,
    pickDefaultProjectPath: handlePickDefaultProjectPath,
    pickAdditionalScriptDirectory: handlePickAdditionalScriptDirectory,
    openIde: handleOpenIde,
  };
}
