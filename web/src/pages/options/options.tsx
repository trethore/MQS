import { useState } from 'react';

import { MqsList } from '@/components/shared/mqs-list';
import { useOptionsBridge } from '@/hooks/use-options-bridge';
import { cn } from '@/lib/utils';
import { IdeSection } from '@/pages/options/components/ide-section';
import { PermissionsSection } from '@/pages/options/components/permissions-section';
import { ScriptsSection } from '@/pages/options/components/scripts-section';
import { Separator } from '@/components/ui/separator';
import {
  areStringArraysEqual,
  formatPathListValue,
  normalizePathListValue,
} from '@/pages/options/path-list';

export function OptionsPage() {
  const {
    options,
    hasLoadedSnapshot,
    isLoading,
    errorMessage,
    isSavingDefaultIdeCommand,
    isSavingDefaultProjectPath,
    isSavingAdditionalScriptDirectories,
    isPickingDefaultProjectPath,
    isPickingAdditionalScriptDirectory,
    isOpeningIde,
    isOptionPending,
    setBooleanOption,
    setDefaultIdeCommand,
    setDefaultProjectPath,
    setAdditionalScriptDirectories,
    pickDefaultProjectPath,
    pickAdditionalScriptDirectory,
    openIde,
  } = useOptionsBridge();
  const [defaultIdeCommandDraft, setDefaultIdeCommandDraft] = useState<string>();
  const [defaultProjectPathDraft, setDefaultProjectPathDraft] = useState<string>();
  const [additionalScriptDirectoriesDraft, setAdditionalScriptDirectoriesDraft] =
    useState<string>();
  const defaultIdeCommandValue = defaultIdeCommandDraft ?? options.defaultIdeCommand;
  const defaultProjectPathValue = defaultProjectPathDraft ?? options.defaultProjectPath;
  const additionalScriptDirectoriesValue =
    additionalScriptDirectoriesDraft ?? formatPathListValue(options.additionalScriptDirectories);

  const saveDefaultIdeCommandIfNeeded = async () => {
    if (defaultIdeCommandDraft == undefined) {
      return true;
    }

    if (defaultIdeCommandDraft === options.defaultIdeCommand) {
      setDefaultIdeCommandDraft(undefined);
      return true;
    }

    const didSave = await setDefaultIdeCommand(defaultIdeCommandDraft);
    if (didSave) {
      setDefaultIdeCommandDraft(undefined);
    }

    return didSave;
  };

  const saveDefaultProjectPathIfNeeded = async () => {
    if (defaultProjectPathDraft == undefined) {
      return true;
    }

    if (defaultProjectPathDraft === options.defaultProjectPath) {
      setDefaultProjectPathDraft(undefined);
      return true;
    }

    const didSave = await setDefaultProjectPath(defaultProjectPathDraft);
    if (didSave) {
      setDefaultProjectPathDraft(undefined);
    }

    return didSave;
  };

  const saveAdditionalScriptDirectoriesIfNeeded = async () => {
    if (additionalScriptDirectoriesDraft == undefined) {
      return true;
    }

    const nextDirectories = normalizePathListValue(additionalScriptDirectoriesDraft);
    if (areStringArraysEqual(nextDirectories, options.additionalScriptDirectories)) {
      setAdditionalScriptDirectoriesDraft(undefined);
      return true;
    }

    const didSave = await setAdditionalScriptDirectories(nextDirectories);
    if (didSave) {
      setAdditionalScriptDirectoriesDraft(undefined);
    }

    return didSave;
  };

  const statusMessageClassName = errorMessage
    ? 'text-destructive dark:text-rose-300'
    : 'text-muted-foreground';

  return (
    <section className="flex h-full min-h-0 w-full flex-col gap-4">
      <div className="w-full">
        <h2 className="text-left text-2xl font-semibold tracking-tight text-card-foreground">
          Options
        </h2>
        {errorMessage && (
          <p className={cn('mt-1 text-sm', statusMessageClassName)}>{errorMessage}</p>
        )}
      </div>

      <MqsList className="w-full" contentClassName="gap-5 pr-2 pb-5">
        <PermissionsSection
          hasLoadedSnapshot={hasLoadedSnapshot}
          isLoading={isLoading}
          logRedirect={options.logRedirect}
          allowAllClasses={options.allowAllClasses}
          logRedirectPending={isOptionPending('logRedirect')}
          allowAllClassesPending={isOptionPending('allowAllClasses')}
          setBooleanOption={setBooleanOption}
        />

        <Separator className="bg-border/80 dark:bg-input" />

        <ScriptsSection
          value={additionalScriptDirectoriesValue}
          disabled={
            isLoading || isSavingAdditionalScriptDirectories || isPickingAdditionalScriptDirectory
          }
          onValueChange={setAdditionalScriptDirectoriesDraft}
          onBlur={() => {
            void saveAdditionalScriptDirectoriesIfNeeded();
          }}
          onEnter={() => {
            void saveAdditionalScriptDirectoriesIfNeeded();
          }}
          onPickDirectory={() => {
            void (async () => {
              const currentDirectories = normalizePathListValue(additionalScriptDirectoriesValue);
              const pickedPath = await pickAdditionalScriptDirectory(
                currentDirectories.at(-1) ?? options.defaultScriptDirectory
              );
              if (!pickedPath) {
                return;
              }

              const nextDirectories = [...currentDirectories, pickedPath];
              const nextValue = formatPathListValue(nextDirectories);
              setAdditionalScriptDirectoriesDraft(nextValue);
              const didSave = await setAdditionalScriptDirectories(nextDirectories);
              if (didSave) {
                setAdditionalScriptDirectoriesDraft(undefined);
              }
            })();
          }}
        />

        <Separator className="bg-border/80 dark:bg-input" />

        <IdeSection
          defaultIdeCommandValue={defaultIdeCommandValue}
          defaultProjectPathValue={defaultProjectPathValue}
          isDefaultIdeCommandDisabled={isLoading || isSavingDefaultIdeCommand || isOpeningIde}
          isDefaultProjectPathDisabled={
            isLoading || isSavingDefaultProjectPath || isPickingDefaultProjectPath || isOpeningIde
          }
          onDefaultIdeCommandChange={setDefaultIdeCommandDraft}
          onDefaultIdeCommandBlur={() => {
            void saveDefaultIdeCommandIfNeeded();
          }}
          onDefaultIdeCommandEnter={() => {
            void saveDefaultIdeCommandIfNeeded();
          }}
          onOpenIde={() => {
            void (async () => {
              const ideCommandToOpen = defaultIdeCommandValue;
              const didSave = await saveDefaultIdeCommandIfNeeded();
              if (!didSave) {
                return;
              }

              await openIde(ideCommandToOpen);
            })();
          }}
          onDefaultProjectPathChange={setDefaultProjectPathDraft}
          onDefaultProjectPathBlur={() => {
            void saveDefaultProjectPathIfNeeded();
          }}
          onDefaultProjectPathEnter={() => {
            void saveDefaultProjectPathIfNeeded();
          }}
          onBrowseProjectPath={() => {
            void (async () => {
              const didSave = await saveDefaultProjectPathIfNeeded();
              if (!didSave) {
                return;
              }

              const didPick = await pickDefaultProjectPath();
              if (didPick) {
                setDefaultProjectPathDraft(undefined);
              }
            })();
          }}
        />
      </MqsList>
    </section>
  );
}
