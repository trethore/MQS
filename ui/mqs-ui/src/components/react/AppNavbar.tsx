import { cn } from "@/lib/utils";

import { ThemeSwitcher } from "./ThemeSwitcher";

type NavItemId = "scripts" | "console" | "options";

interface AppNavbarProps {
  activeItem: NavItemId;
}

const NAV_ITEMS: Array<{ id: NavItemId; label: string; href: string }> = [
  { id: "scripts", label: "Scripts", href: "/scripts/index.html" },
  { id: "console", label: "Console", href: "/console/index.html" },
  { id: "options", label: "Options", href: "/options/index.html" },
];

export function AppNavbar({ activeItem }: AppNavbarProps) {
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
            {NAV_ITEMS.map((item) => (
              <li key={item.id}>
                <a
                  href={item.href}
                  aria-current={item.id === activeItem ? "page" : undefined}
                  className={cn(
                    "mqs-focus-highlight inline-flex h-9 items-center rounded-md px-4 text-sm font-semibold transition-colors",
                    item.id === activeItem
                      ? "bg-accent/90 text-foreground"
                      : "text-muted-foreground hover:bg-accent hover:text-foreground",
                  )}
                >
                  {item.id === activeItem ? (
                    <span className="bg-linear-to-r from-primary to-primary-2 bg-clip-text text-transparent">
                      {item.label}
                    </span>
                  ) : (
                    item.label
                  )}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="flex justify-self-end">
          <ThemeSwitcher />
        </div>
      </div>
    </header>
  );
}
