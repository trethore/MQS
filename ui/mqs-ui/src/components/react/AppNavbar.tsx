import type { FocusEvent, MouseEvent } from "react";
import { useCallback, useState } from "react";
import { ChevronDown } from "lucide-react";

import { prepareCodeWorkspace } from "@/bridge/services/codeService";
import {
  NAV_ITEMS,
  SETTINGS_MENU_ITEMS,
  UI_NAVIGATION_PAGE_ATTRIBUTE,
  type NavItemId,
  resolveStaticUiPageHref,
} from "@/lib/uiNavigation";
import { cn } from "@/lib/utils";

import { ThemeSwitcher } from "./ThemeSwitcher";

interface AppNavbarProps {
  activeItem: NavItemId;
  pageRootHref: string;
}

const CODE_EDITOR_URL = "https://vscode.dev/";
const SETTINGS_LABEL = "Settings";

export function AppNavbar({ activeItem, pageRootHref }: AppNavbarProps) {
  const [preparingCodeLink, setPreparingCodeLink] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);

  const handleSettingsTriggerClick = useCallback(() => {
    setSettingsOpen((currentOpen) => !currentOpen);
  }, []);

  const handleSettingsBlur = useCallback((event: FocusEvent<HTMLLIElement>) => {
    if (event.currentTarget.contains(event.relatedTarget)) {
      return;
    }

    setSettingsOpen(false);
  }, []);

  const copyWorkspacePathWithBrowserClipboard = useCallback(async (workspacePath: string) => {
    if (!workspacePath) {
      return;
    }

    if (!globalThis.navigator?.clipboard?.writeText) {
      return;
    }

    await globalThis.navigator.clipboard.writeText(workspacePath);
  }, []);

  const handleCodeClick = useCallback(
    async (event: MouseEvent<HTMLAnchorElement>) => {
      event.preventDefault();
      if (preparingCodeLink) {
        return;
      }

      setPreparingCodeLink(true);

      try {
        const response = await prepareCodeWorkspace();
        if (!response.copied) {
          await copyWorkspacePathWithBrowserClipboard(response.modDirPath);
        }
      } catch (error_) {
        globalThis.console.warn("[MQS UI] Failed to prepare code workspace.", error_);
      } finally {
        setPreparingCodeLink(false);
        globalThis.window.location.assign(CODE_EDITOR_URL);
      }
    },
    [copyWorkspacePathWithBrowserClipboard, preparingCodeLink],
  );

  return (
    <header className="p-4 pb-0">
      <div className="mqs-elevated grid grid-cols-[1fr_auto_1fr] items-center gap-3 rounded-2xl border border-border bg-card px-5 py-3">
        <p className="text-sm font-semibold text-foreground">
          My{" "}
          <span className="bg-linear-to-r from-primary to-primary-2 bg-clip-text text-transparent">
            QOL
          </span>{" "}
          Scripts
        </p>

        <nav aria-label="Primary" className="justify-self-center">
          <ul className="flex items-center gap-1">
            {NAV_ITEMS.map((item) => {
              const active = item.id === activeItem;

              return (
                <li key={item.id}>
                  <a
                    href={resolveStaticUiPageHref(pageRootHref, item.href)}
                    {...{ [UI_NAVIGATION_PAGE_ATTRIBUTE]: item.href }}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "mqs-focus-highlight inline-flex h-9 items-center rounded-md px-4 text-sm font-semibold transition-colors",
                      active
                        ? "bg-accent/90 text-foreground"
                        : "text-muted-foreground hover:bg-accent hover:text-foreground",
                    )}
                  >
                    {active ? (
                      <span className="bg-linear-to-r from-primary to-primary-2 bg-clip-text text-transparent">
                        {item.label}
                      </span>
                    ) : (
                      item.label
                    )}
                  </a>
                </li>
              );
            })}

            <li
              className="relative"
              onBlur={handleSettingsBlur}
              onMouseEnter={() => setSettingsOpen(true)}
              onMouseLeave={() => setSettingsOpen(false)}
            >
              <button
                type="button"
                aria-current={activeItem === "settings" ? "page" : undefined}
                aria-expanded={settingsOpen}
                aria-haspopup="menu"
                onClick={handleSettingsTriggerClick}
                onFocus={() => setSettingsOpen(true)}
                className={cn(
                  "mqs-focus-highlight inline-flex h-9 items-center gap-1 rounded-md px-4 text-sm font-semibold transition-colors",
                  activeItem === "settings"
                    ? "bg-accent/90 text-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-foreground",
                )}
              >
                <span
                  className={cn(
                    activeItem === "settings"
                      ? "bg-linear-to-r from-primary to-primary-2 bg-clip-text text-transparent"
                      : null,
                  )}
                >
                  {SETTINGS_LABEL}
                </span>
                <ChevronDown
                  aria-hidden="true"
                  className={cn(
                    "relative top-px ml-1 size-3 transition duration-200",
                    settingsOpen ? "rotate-180" : null,
                    activeItem === "settings" ? "text-primary" : null,
                  )}
                />
              </button>

              <div
                className={cn(
                  "absolute left-1/2 top-full z-20 w-80 -translate-x-1/2 pt-2 transition duration-150",
                  settingsOpen
                    ? "pointer-events-auto translate-y-0 opacity-100"
                    : "pointer-events-none -translate-y-1 opacity-0",
                )}
              >
                <div className="mqs-elevated rounded-2xl border border-border bg-popover p-2 text-popover-foreground">
                  <div className="grid gap-1">
                    {SETTINGS_MENU_ITEMS.map((item) => {
                      const classes =
                        "mqs-focus-highlight flex w-full flex-col rounded-xl px-4 py-3 text-left transition-colors hover:bg-accent hover:text-accent-foreground";

                      return (
                        <a
                          key={item.id}
                          href={resolveStaticUiPageHref(pageRootHref, item.href)}
                          {...{ [UI_NAVIGATION_PAGE_ATTRIBUTE]: item.href }}
                          className={classes}
                          onClick={() => setSettingsOpen(false)}
                          onFocus={() => setSettingsOpen(true)}
                        >
                          <span className="text-sm font-semibold text-foreground">
                            {item.label}
                          </span>
                          <span className="text-xs text-muted-foreground">{item.description}</span>
                        </a>
                      );
                    })}
                  </div>
                </div>
              </div>
            </li>

            <li>
              <a
                href={CODE_EDITOR_URL}
                aria-disabled={preparingCodeLink ? "true" : undefined}
                onClick={handleCodeClick}
                className={cn(
                  "mqs-focus-highlight inline-flex h-9 items-center rounded-md px-4 text-sm font-semibold transition-colors",
                  "text-muted-foreground hover:bg-accent hover:text-foreground",
                  preparingCodeLink ? "opacity-70" : null,
                )}
              >
                Code
              </a>
            </li>
          </ul>
        </nav>

        <div className="flex justify-self-end">
          <ThemeSwitcher />
        </div>
      </div>
    </header>
  );
}
