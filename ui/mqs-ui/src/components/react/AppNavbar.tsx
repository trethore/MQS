import type { MouseEvent } from "react";
import { useCallback, useState } from "react";

import { prepareCodeWorkspace } from "@/bridge/services/codeService";
import { cn } from "@/lib/utils";

import { ThemeSwitcher } from "./ThemeSwitcher";

type NavItemId = "scripts" | "console" | "options";
type NavLinkId = NavItemId | "code";

interface AppNavbarProps {
  activeItem: NavItemId;
}

const CODE_EDITOR_URL = "https://vscode.dev/";

const NAV_ITEMS: Array<{ id: NavLinkId; label: string; href: string }> = [
  { id: "scripts", label: "Scripts", href: "../scripts/index.html" },
  { id: "console", label: "Console", href: "../console/index.html" },
  { id: "options", label: "Options", href: "../options/index.html" },
  { id: "code", label: "Code", href: CODE_EDITOR_URL },
];

export function AppNavbar({ activeItem }: AppNavbarProps) {
  const [preparingCodeLink, setPreparingCodeLink] = useState(false);

  const handleActiveNavigationClick = useCallback((event: MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
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
              const codeLink = item.id === "code";
              let onClick:
                | ((event: MouseEvent<HTMLAnchorElement>) => void | Promise<void>)
                | undefined;

              if (codeLink) {
                onClick = handleCodeClick;
              } else if (active) {
                onClick = handleActiveNavigationClick;
              }

              return (
                <li key={item.id}>
                  <a
                    href={item.href}
                    aria-current={active ? "page" : undefined}
                    aria-disabled={codeLink && preparingCodeLink ? "true" : undefined}
                    onClick={onClick}
                    className={cn(
                      "mqs-focus-highlight inline-flex h-9 items-center rounded-md px-4 text-sm font-semibold transition-colors",
                      active
                        ? "bg-accent/90 text-foreground"
                        : "text-muted-foreground hover:bg-accent hover:text-foreground",
                      codeLink && preparingCodeLink ? "opacity-70" : null,
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
          </ul>
        </nav>

        <div className="flex justify-self-end">
          <ThemeSwitcher />
        </div>
      </div>
    </header>
  );
}
