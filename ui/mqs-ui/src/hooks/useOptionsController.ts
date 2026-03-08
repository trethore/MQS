import { useCallback, useEffect, useMemo, useState } from "react";

import { getBridgeErrorMessage } from "@/bridge/core/bridgeError";
import type { OptionsSnapshot } from "@/bridge/contracts/options";
import {
  fetchOptionsSnapshot,
  openOptionsPath,
  subscribeToOptionsUpdated,
  updateOptions,
} from "@/bridge/services/optionsService";

interface OptionsFormState {
  logRedirect: boolean;
  allowAllClasses: boolean;
  defaultIdeCommand: string;
  additionalScriptDirectoriesInput: string;
  openPath: string;
}

const EMPTY_SNAPSHOT: OptionsSnapshot = {
  logRedirect: false,
  allowAllClasses: false,
  defaultIdeCommand: "code",
  defaultProjectPath: "",
  additionalScriptDirectories: [],
  defaultScriptDirectory: "",
};

function formatDirectories(directories: string[]): string {
  return directories.join("; ");
}

function parseDirectories(input: string): string[] {
  return input
    .split(";")
    .map((entry) => entry.trim())
    .filter((entry, index, entries) => entry.length > 0 && entries.indexOf(entry) === index);
}

function createFormState(snapshot: OptionsSnapshot): OptionsFormState {
  return {
    logRedirect: snapshot.logRedirect,
    allowAllClasses: snapshot.allowAllClasses,
    defaultIdeCommand: snapshot.defaultIdeCommand,
    additionalScriptDirectoriesInput: formatDirectories(snapshot.additionalScriptDirectories),
    openPath: snapshot.defaultProjectPath,
  };
}

function areSnapshotsEqual(leftSnapshot: OptionsSnapshot, rightSnapshot: OptionsSnapshot): boolean {
  if (
    leftSnapshot.logRedirect !== rightSnapshot.logRedirect ||
    leftSnapshot.allowAllClasses !== rightSnapshot.allowAllClasses ||
    leftSnapshot.defaultIdeCommand !== rightSnapshot.defaultIdeCommand ||
    leftSnapshot.defaultProjectPath !== rightSnapshot.defaultProjectPath ||
    leftSnapshot.defaultScriptDirectory !== rightSnapshot.defaultScriptDirectory ||
    leftSnapshot.additionalScriptDirectories.length !==
      rightSnapshot.additionalScriptDirectories.length
  ) {
    return false;
  }

  return leftSnapshot.additionalScriptDirectories.every((directory, index) => {
    return directory === rightSnapshot.additionalScriptDirectories[index];
  });
}

export function useOptionsController() {
  const [snapshot, setSnapshot] = useState<OptionsSnapshot>(EMPTY_SNAPSHOT);
  const [formState, setFormState] = useState<OptionsFormState>(() =>
    createFormState(EMPTY_SNAPSHOT),
  );
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [openingExplorer, setOpeningExplorer] = useState(false);
  const [openingProject, setOpeningProject] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const applySnapshot = useCallback((nextSnapshot: OptionsSnapshot) => {
    setSnapshot((previousSnapshot) => {
      return areSnapshotsEqual(previousSnapshot, nextSnapshot) ? previousSnapshot : nextSnapshot;
    });
    setFormState(createFormState(nextSnapshot));
    setErrorMessage(null);
  }, []);

  const requestSnapshot = useCallback(async () => {
    try {
      const nextSnapshot = await fetchOptionsSnapshot();
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
      unsubscribe = subscribeToOptionsUpdated((nextSnapshot) => {
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

  const dirty = useMemo(() => {
    return (
      formState.logRedirect !== snapshot.logRedirect ||
      formState.allowAllClasses !== snapshot.allowAllClasses ||
      formState.defaultIdeCommand.trim() !== snapshot.defaultIdeCommand ||
      formState.openPath.trim() !== snapshot.defaultProjectPath ||
      formatDirectories(parseDirectories(formState.additionalScriptDirectoriesInput)) !==
        formatDirectories(snapshot.additionalScriptDirectories)
    );
  }, [formState, snapshot]);

  const setField = useCallback(
    <Key extends keyof OptionsFormState>(field: Key, value: OptionsFormState[Key]) => {
      setErrorMessage(null);
      setFormState((currentState) => ({
        ...currentState,
        [field]: value,
      }));
    },
    [],
  );

  const save = useCallback(async () => {
    setSaving(true);
    setErrorMessage(null);

    try {
      const response = await updateOptions({
        logRedirect: formState.logRedirect,
        allowAllClasses: formState.allowAllClasses,
        defaultIdeCommand: formState.defaultIdeCommand.trim(),
        defaultProjectPath: formState.openPath.trim(),
        additionalScriptDirectories: parseDirectories(formState.additionalScriptDirectoriesInput),
      });

      if (response.options) {
        applySnapshot(response.options);
      }

      if (!response.success) {
        setErrorMessage(response.message || "Failed to save MQS options.");
        return;
      }
    } catch (error) {
      setErrorMessage(getBridgeErrorMessage(error, "Failed to save MQS options."));
    } finally {
      setSaving(false);
    }
  }, [applySnapshot, formState]);

  const openProject = useCallback(async () => {
    setOpeningProject(true);
    setErrorMessage(null);

    try {
      const response = await openOptionsPath({
        path: formState.openPath.trim(),
        defaultIdeCommand: formState.defaultIdeCommand.trim(),
        target: "ide",
      });

      if (!response.success) {
        setErrorMessage(response.message || "Failed to open project.");
        return;
      }
    } catch (error) {
      setErrorMessage(getBridgeErrorMessage(error, "Failed to open project."));
    } finally {
      setOpeningProject(false);
    }
  }, [formState.defaultIdeCommand, formState.openPath]);

  const openExplorer = useCallback(async () => {
    setOpeningExplorer(true);
    setErrorMessage(null);

    try {
      const response = await openOptionsPath({
        path: formState.openPath.trim(),
        target: "picker",
      });

      if (!response.success) {
        setErrorMessage(response.message || "Failed to open explorer.");
        return;
      }

      if (response.openedPath) {
        setFormState((currentState) => ({
          ...currentState,
          openPath: response.openedPath,
        }));
      }
    } catch (error) {
      setErrorMessage(getBridgeErrorMessage(error, "Failed to open explorer."));
    } finally {
      setOpeningExplorer(false);
    }
  }, [formState.openPath]);

  return {
    defaultScriptDirectory: snapshot.defaultScriptDirectory,
    dirty,
    errorMessage,
    formState,
    loading,
    openExplorer,
    openProject,
    openingExplorer,
    openingProject,
    save,
    saving,
    setField,
  };
}
