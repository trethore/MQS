import { useEffect, useRef, useState } from "react";
import { Check, Moon, Sun } from "lucide-react";

import { cn } from "@/lib/utils";

type ThemePreference = "dark" | "light" | "system";

const THEME_OPTIONS: Array<{ label: string; value: ThemePreference }> = [
  { label: "Dark", value: "dark" },
  { label: "Light", value: "light" },
  { label: "System", value: "system" },
];

function parseThemePreference(value: string | null): ThemePreference {
  if (value === "dark" || value === "light" || value === "system") {
    return value;
  }

  return "system";
}

function isDarkTheme(themePreference: ThemePreference, mediaQuery: MediaQueryList): boolean {
  return themePreference === "dark" || (themePreference === "system" && mediaQuery.matches);
}

function getStoredThemePreference(): ThemePreference {
  try {
    return parseThemePreference(window.localStorage.getItem("theme"));
  } catch (ignored) {
    return "system";
  }
}

function storeThemePreference(themePreference: ThemePreference): void {
  try {
    window.localStorage.setItem("theme", themePreference);
  } catch (ignored) {
  }
}

export function ThemeSwitcher() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [themePreference, setThemePreference] = useState<ThemePreference>("system");
  const [darkTheme, setDarkTheme] = useState<boolean | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

    const syncTheme = () => {
      const nextThemePreference = getStoredThemePreference();
      const nextDarkTheme = isDarkTheme(nextThemePreference, mediaQuery);

      document.documentElement.classList.toggle("dark", nextDarkTheme);
      setThemePreference(nextThemePreference);
      setDarkTheme(nextDarkTheme);
    };

    const onSystemThemeChange = () => {
      if (getStoredThemePreference() === "system") {
        syncTheme();
      }
    };

    syncTheme();

    mediaQuery.addEventListener("change", onSystemThemeChange);

    return () => {
      mediaQuery.removeEventListener("change", onSystemThemeChange);
    };
  }, []);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }

    const onDocumentClick = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };

    const onEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", onDocumentClick);
    document.addEventListener("keydown", onEscape);

    return () => {
      document.removeEventListener("mousedown", onDocumentClick);
      document.removeEventListener("keydown", onEscape);
    };
  }, [menuOpen]);

  const setTheme = (nextThemePreference: ThemePreference) => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const nextDarkTheme = isDarkTheme(nextThemePreference, mediaQuery);

    storeThemePreference(nextThemePreference);
    document.documentElement.classList.toggle("dark", nextDarkTheme);
    setThemePreference(nextThemePreference);
    setDarkTheme(nextDarkTheme);
    setMenuOpen(false);
  };

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        aria-label="Toggle theme"
        aria-haspopup="menu"
        aria-expanded={menuOpen}
        onClick={() => setMenuOpen((previous) => !previous)}
        className="inline-flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        <span className="sr-only">Toggle theme</span>
        <div className="flex h-4 w-4 items-center justify-center">
          {darkTheme === null ? null : darkTheme ? <Moon className="size-4" /> : <Sun className="size-4" />}
        </div>
      </button>

      {menuOpen ? (
        <div
          role="menu"
          aria-label="Theme"
          className="absolute right-0 top-[calc(100%+0.5rem)] z-50 min-w-32 rounded-lg border border-border bg-popover p-1 text-popover-foreground shadow-[rgb(0_0_0_/_0.24)_0_0.1875rem_0.5rem]"
        >
          {THEME_OPTIONS.map((option) => {
            const selected = option.value === themePreference;

            return (
              <button
                key={option.value}
                type="button"
                role="menuitemradio"
                aria-checked={selected}
                className={cn(
                  "flex w-full items-center justify-between rounded-md px-2 py-1.5 text-sm transition-colors focus-visible:outline-none focus-visible:bg-accent focus-visible:text-accent-foreground",
                  selected
                    ? "bg-accent text-accent-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-foreground"
                )}
                onClick={() => setTheme(option.value)}
              >
                <span>{option.label}</span>
                {selected ? <Check className="size-4" /> : null}
              </button>
            );
          })}
        </div>
      ) : null}
    </div>
  );
}
