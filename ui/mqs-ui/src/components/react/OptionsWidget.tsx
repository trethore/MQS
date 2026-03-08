import { FolderOpen, Save } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { useOptionsController } from "@/hooks/useOptionsController";
import { ScrollbarWidget } from "@/components/react/ScrollbarWidget";

export function OptionsWidget() {
  const {
    dirty,
    errorMessage,
    formState,
    loading,
    openPath,
    openingPath,
    save,
    saving,
    setField,
    successMessage,
  } = useOptionsController();

  const handleOpenPathClick = () => {
    openPath().then();
  };

  const handleSaveClick = () => {
    save().then();
  };

  return (
    <div className="relative flex min-h-0 flex-1 flex-col">
      <ScrollbarWidget className="-mr-3 pr-3 pb-5">
        {dirty ? (
          <div className="sticky top-3 z-20 -mb-12 flex justify-end pr-1 pb-3">
            <Button
              type="button"
              size="icon"
              variant="default"
              className="mqs-surface-shadow mqs-surface-shadow-front size-12 shrink-0 shadow-[0_1rem_2.5rem_-0.75rem_rgb(0_0_0_/_0.55)]"
              disabled={saving || openingPath}
              onClick={handleSaveClick}
              aria-label={saving ? "Saving changes" : "Save changes"}
              title={saving ? "Saving changes" : "Save changes"}
            >
              <Save className="size-5" />
            </Button>
          </div>
        ) : null}

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

        {!errorMessage && successMessage ? (
          <output
            className="mt-4 rounded-md border border-success/40 bg-success/10 px-3 py-2 text-sm text-foreground"
          >
            {successMessage}
          </output>
        ) : null}

        <div className="mt-5">
          {loading ? (
            <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
              Loading options...
            </div>
          ) : (
            <div className="flex flex-col gap-8 pb-2">
              <section>
                <h2 className="text-xl font-semibold text-foreground">Runtime behavior</h2>

                <div className="mt-5 flex flex-col gap-5">
                  <div className="flex items-start justify-between gap-4">
                    <div className="pr-4">
                      <label className="text-sm font-semibold text-foreground" htmlFor="log-redirect-switch">
                        Log Redirect
                      </label>
                      <p className="mt-1 text-sm text-muted-foreground">
                        Forward script logs into the MQS console output.
                      </p>
                    </div>
                    <Switch
                      id="log-redirect-switch"
                      checked={formState.logRedirect}
                      onCheckedChange={(checked) => {
                        setField("logRedirect", checked);
                      }}
                      aria-label="Toggle log redirect"
                    />
                  </div>

                  <div className="flex items-start justify-between gap-4">
                    <div className="pr-4">
                      <label className="text-sm font-semibold text-foreground" htmlFor="allow-all-classes-switch">
                        Allow all classes
                      </label>
                      <p className="mt-1 text-sm text-muted-foreground">
                        Disable package restrictions and let scripts access any JVM class.
                      </p>
                    </div>
                    <Switch
                      id="allow-all-classes-switch"
                      checked={formState.allowAllClasses}
                      onCheckedChange={(checked) => {
                        setField("allowAllClasses", checked);
                      }}
                      aria-label="Toggle allow all classes"
                    />
                  </div>
                </div>
              </section>

              <section>
                <h2 className="text-xl font-semibold text-foreground">Scripts</h2>

                <div className="mt-6 flex flex-col gap-5">
                  <label htmlFor="additional-script-directories" className="text-sm font-semibold text-foreground">
                    Additional script directories
                  </label>
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
                <h2 className="text-xl font-semibold text-foreground">IDE</h2>

                <div className="mt-6 flex flex-col gap-6">
                  <div className="flex flex-col gap-5">
                    <label htmlFor="default-ide-command" className="text-sm font-semibold text-foreground">
                      Default IDE Command
                    </label>
                    <Input
                      id="default-ide-command"
                      value={formState.defaultIdeCommand}
                      onChange={(event) => {
                        setField("defaultIdeCommand", event.target.value);
                      }}
                      placeholder="code"
                      className="mqs-surface-shadow mqs-surface-shadow-front"
                    />
                  </div>

                  <div className="flex flex-col gap-5">
                    <label htmlFor="open-path" className="text-sm font-semibold text-foreground">
                      Default Project Path
                    </label>
                    <div className="flex flex-col gap-3 sm:flex-row">
                      <Input
                        id="open-path"
                        value={formState.openPath}
                        onChange={(event) => {
                          setField("openPath", event.target.value);
                        }}
                        placeholder="Leave empty to open ../myqolscripts/scripts/."
                        className="mqs-surface-shadow mqs-surface-shadow-front flex-1"
                      />
                      <Button
                        type="button"
                        variant="default"
                        className="mqs-surface-shadow mqs-surface-shadow-front shrink-0"
                        disabled={openingPath || saving}
                        onClick={handleOpenPathClick}
                      >
                        <FolderOpen />
                        {openingPath ? "Opening..." : "Open path"}
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
