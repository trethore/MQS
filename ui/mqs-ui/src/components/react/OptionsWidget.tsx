import { ExternalLink, FolderOpen, Save } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useOptionsController } from "@/hooks/useOptionsController";
import { ScrollbarWidget } from "@/components/react/ScrollbarWidget";
import { cn } from "@/lib/utils";

function OptionsSectionHeading({ title }: { title: string }) {
  return (
    <div className="border-t border-border/70 pt-5">
      <h2 className="bg-linear-to-r from-primary to-primary-2 bg-clip-text text-xl font-semibold text-transparent">
        {title}
      </h2>
    </div>
  );
}

interface ToggleOptionRowProps {
  checked: boolean;
  description: string;
  onCheckedChange: (checked: boolean) => void;
  title: string;
}

function ToggleOptionRow({ checked, description, onCheckedChange, title }: ToggleOptionRowProps) {
  return (
    <button
      type="button"
      className="mqs-focus-highlight flex w-full cursor-pointer items-start justify-between gap-4 rounded-md text-left"
      aria-pressed={checked}
      onClick={() => {
        onCheckedChange(!checked);
      }}
    >
      <div className="pr-4">
        <p className="text-sm font-semibold text-foreground">{title}</p>
        <p className="mt-1 text-sm text-muted-foreground">{description}</p>
      </div>
      <span
        aria-hidden="true"
        className={cn(
          "relative inline-flex h-[1.15rem] w-8 shrink-0 rounded-full border border-transparent transition-all",
          checked ? "bg-primary" : "bg-input dark:bg-input/80",
        )}
      >
        <span
          className={cn(
            "block size-4 rounded-full bg-background shadow-lg ring-0 transition-transform",
            checked ? "dark:bg-primary-foreground" : "dark:bg-foreground",
            checked ? "translate-x-[calc(100%-2px)]" : "translate-x-0",
          )}
        />
      </span>
    </button>
  );
}

export function OptionsWidget() {
  const {
    defaultScriptDirectory,
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
  } = useOptionsController();

  const handleOpenExplorerClick = () => {
    openExplorer();
  };

  const handleOpenProjectClick = () => {
    openProject();
  };

  const handleSaveClick = () => {
    void save();
  };

  const ideActionsDisabled = saving || openingExplorer || openingProject;
  const defaultProjectPathPlaceholder = defaultScriptDirectory
    ? `Leave empty to open ${defaultScriptDirectory}.`
    : "Leave empty to open the default scripts directory.";

  return (
    <div className="relative flex min-h-0 flex-1 flex-col">
      {dirty ? (
        <div className="pointer-events-none absolute top-3 right-4 z-20 flex justify-end">
          <Button
            type="button"
            size="icon"
            variant="default"
            className="mqs-floating-save-button pointer-events-auto size-12 shrink-0 [&_svg]:size-7"
            disabled={ideActionsDisabled}
            onClick={handleSaveClick}
            aria-label={saving ? "Saving changes" : "Save changes"}
            title={saving ? "Saving changes" : "Save changes"}
          >
            <Save />
          </Button>
        </div>
      ) : null}

      <ScrollbarWidget className="-mr-3 pr-3 pb-5">
        <div className="flex flex-col gap-4 pr-16 sm:flex-row sm:items-start sm:justify-between">
          <div className="shrink-0">
            <h1 className="text-left text-3xl font-semibold text-foreground">
              <span className="bg-linear-to-r from-primary to-primary-2 bg-clip-text text-transparent">
                MQS
              </span>{" "}
              Options
            </h1>
            <p className="mt-2 text-sm text-muted-foreground">
              Configure scripting behavior, trusted class access, and editor integration.
            </p>
          </div>
        </div>

        {errorMessage ? (
          <p
            role="alert"
            className="mt-4 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {errorMessage}
          </p>
        ) : null}

        <div className="mt-5">
          {loading ? (
            <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
              Loading options...
            </div>
          ) : (
            <div className="flex flex-col gap-8 pb-2">
              <section>
                <OptionsSectionHeading title="Runtime behavior" />

                <div className="mt-5 flex flex-col gap-5">
                  <ToggleOptionRow
                    title="Log Redirect"
                    description="Forward script logs into the MQS console output."
                    checked={formState.logRedirect}
                    onCheckedChange={(checked) => {
                      setField("logRedirect", checked);
                    }}
                  />

                  <ToggleOptionRow
                    title="Allow all classes"
                    description="Disable package restrictions and let scripts access any JVM class."
                    checked={formState.allowAllClasses}
                    onCheckedChange={(checked) => {
                      setField("allowAllClasses", checked);
                    }}
                  />
                </div>
              </section>

              <section>
                <OptionsSectionHeading title="Scripts" />

                <div className="mt-6 flex flex-col gap-5">
                  <div className="text-sm font-semibold text-foreground">
                    Additional script directories
                  </div>
                  <Input
                    id="additional-script-directories"
                    value={formState.additionalScriptDirectoriesInput}
                    onChange={(event) => {
                      setField("additionalScriptDirectoriesInput", event.target.value);
                    }}
                    placeholder="../../scripts; ../other-scripts/"
                    className="mqs-surface-shadow mqs-surface-shadow-front"
                  />
                </div>
              </section>

              <section>
                <OptionsSectionHeading title="IDE" />

                <div className="mt-6 flex flex-col gap-6">
                  <div className="flex flex-col gap-5">
                    <div className="text-sm font-semibold text-foreground">Default IDE Command</div>
                    <div className="flex flex-col gap-3 sm:flex-row">
                      <Input
                        id="default-ide-command"
                        value={formState.defaultIdeCommand}
                        onChange={(event) => {
                          setField("defaultIdeCommand", event.target.value);
                        }}
                        placeholder="code"
                        className="mqs-surface-shadow mqs-surface-shadow-front flex-1"
                      />
                      <Button
                        type="button"
                        variant="default"
                        className="mqs-surface-shadow mqs-surface-shadow-front w-full shrink-0 justify-center sm:w-40"
                        disabled={ideActionsDisabled}
                        onClick={handleOpenProjectClick}
                      >
                        <ExternalLink />
                        {openingProject ? "Opening..." : "Open Project"}
                      </Button>
                    </div>
                  </div>

                  <div className="flex flex-col gap-5">
                    <div className="text-sm font-semibold text-foreground">
                      Default Project Path
                    </div>
                    <div className="flex flex-col gap-3 sm:flex-row">
                      <Input
                        id="open-path"
                        value={formState.openPath}
                        onChange={(event) => {
                          setField("openPath", event.target.value);
                        }}
                        placeholder={defaultProjectPathPlaceholder}
                        className="mqs-surface-shadow mqs-surface-shadow-front flex-1"
                      />
                      <Button
                        type="button"
                        variant="default"
                        className="mqs-surface-shadow mqs-surface-shadow-front w-full shrink-0 justify-center sm:w-40"
                        disabled={ideActionsDisabled}
                        onClick={handleOpenExplorerClick}
                      >
                        <FolderOpen />
                        {openingExplorer ? "Opening..." : "Open Explorer"}
                      </Button>
                    </div>
                  </div>
                </div>
              </section>
            </div>
          )}
        </div>
      </ScrollbarWidget>
    </div>
  );
}
