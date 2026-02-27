import {
  useEffect,
  useId,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
} from "react";
import { Check, Moon, Sun } from "lucide-react";

import { cn } from "@/lib/utils";

type ThemePreference = "dark" | "light" | "system";

interface ThemeState {
  themePreference: ThemePreference;
  darkTheme: boolean;
}

interface ThemeApi {
  getThemePreference: () => ThemePreference;
  applyTheme: (themePreference?: ThemePreference) => ThemeState;
  setThemePreference: (themePreference: ThemePreference) => ThemeState;
}

declare global {
  interface Window {
    __mqsTheme?: ThemeApi;
  }
}

const THEME_OPTIONS: Array<{ label: string; value: ThemePreference }> = [
  { label: "Dark", value: "dark" },
  { label: "Light", value: "light" },
  { label: "System", value: "system" },
];

const MENU_NAVIGATION_KEYS = new Set(["ArrowDown", "ArrowUp", "Home", "End"]);

function parseThemePreference(value: string | null): ThemePreference {
  if (value === "dark" || value === "light" || value === "system") {
    return value;
  }

  return "system";
}

function isThemeState(value: unknown): value is ThemeState {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const maybeThemeState = value as Partial<ThemeState>;
  return (
    typeof maybeThemeState.darkTheme === "boolean" &&
    (maybeThemeState.themePreference === "dark" ||
      maybeThemeState.themePreference === "light" ||
      maybeThemeState.themePreference === "system")
  );
}

function getStoredThemePreferenceFallback(): ThemePreference {
  try {
    return parseThemePreference(window.localStorage.getItem("theme"));
  } catch (ignored) {
    return "system";
  }
}

function getThemeApi(): ThemeApi | null {
  return window.__mqsTheme ?? null;
}

function applyThemeFallback(
  themePreference: ThemePreference = getStoredThemePreferenceFallback(),
): ThemeState {
  const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
  const darkTheme =
    themePreference === "dark" || (themePreference === "system" && mediaQuery.matches);

  document.documentElement.classList.toggle("dark", darkTheme);

  return {
    themePreference,
    darkTheme,
  };
}

function setThemePreferenceFallback(themePreference: ThemePreference): ThemeState {
  try {
    window.localStorage.setItem("theme", themePreference);
  } catch (ignored) {}

  return applyThemeFallback(themePreference);
}

export function ThemeSwitcher() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [themePreference, setThemePreferenceState] = useState<ThemePreference>("system");
  const [darkTheme, setDarkTheme] = useState<boolean | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const toggleButtonRef = useRef<HTMLButtonElement>(null);
  const itemRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const menuId = useId();
  const selectedOptionIndex = Math.max(
    0,
    THEME_OPTIONS.findIndex((option) => option.value === themePreference),
  );

  useEffect(() => {
    const syncTheme = () => {
      const themeApi = getThemeApi();
      const nextThemeState = themeApi ? themeApi.applyTheme() : applyThemeFallback();

      setThemePreferenceState(nextThemeState.themePreference);
      setDarkTheme(nextThemeState.darkTheme);
    };

    const onThemeChange = (event: Event) => {
      if (!(event instanceof CustomEvent) || !isThemeState(event.detail)) {
        syncTheme();
        return;
      }

      setThemePreferenceState(event.detail.themePreference);
      setDarkTheme(event.detail.darkTheme);
    };

    syncTheme();

    window.addEventListener("mqs-themechange", onThemeChange);

    return () => {
      window.removeEventListener("mqs-themechange", onThemeChange);
    };
  }, []);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }

    const onDocumentPointerDown = (event: PointerEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };

    const onEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMenuOpen(false);
        toggleButtonRef.current?.focus();
      }
    };

    document.addEventListener("pointerdown", onDocumentPointerDown);
    document.addEventListener("keydown", onEscape);

    return () => {
      document.removeEventListener("pointerdown", onDocumentPointerDown);
      document.removeEventListener("keydown", onEscape);
    };
  }, [menuOpen]);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }

    const focusSelectedOption = window.requestAnimationFrame(() => {
      itemRefs.current[selectedOptionIndex]?.focus();
    });

    return () => {
      window.cancelAnimationFrame(focusSelectedOption);
    };
  }, [menuOpen, selectedOptionIndex]);

  const selectTheme = (nextThemePreference: ThemePreference) => {
    const themeApi = getThemeApi();
    const nextThemeState = themeApi
      ? themeApi.setThemePreference(nextThemePreference)
      : setThemePreferenceFallback(nextThemePreference);

    setThemePreferenceState(nextThemeState.themePreference);
    setDarkTheme(nextThemeState.darkTheme);
    setMenuOpen(false);
    toggleButtonRef.current?.focus();
  };

  const onMenuKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    if (event.key === "Tab") {
      setMenuOpen(false);
      return;
    }

    if (!MENU_NAVIGATION_KEYS.has(event.key)) {
      return;
    }

    event.preventDefault();

    const focusedIndex = itemRefs.current.findIndex((item) => item === document.activeElement);
    const currentIndex = focusedIndex === -1 ? selectedOptionIndex : focusedIndex;
    const lastIndex = THEME_OPTIONS.length - 1;

    if (event.key === "Home") {
      itemRefs.current[0]?.focus();
      return;
    }

    if (event.key === "End") {
      itemRefs.current[lastIndex]?.focus();
      return;
    }

    if (event.key === "ArrowDown") {
      const nextIndex = currentIndex >= lastIndex ? 0 : currentIndex + 1;
      itemRefs.current[nextIndex]?.focus();
      return;
    }

    if (event.key === "ArrowUp") {
      const previousIndex = currentIndex <= 0 ? lastIndex : currentIndex - 1;
      itemRefs.current[previousIndex]?.focus();
    }
  };

  return (
    <div ref={rootRef} className="relative">
      <button
        ref={toggleButtonRef}
        type="button"
        aria-label="Toggle theme"
        aria-haspopup="menu"
        aria-expanded={menuOpen}
        aria-controls={menuOpen ? menuId : undefined}
        onClick={() => setMenuOpen((previous) => !previous)}
        className="mqs-focus-highlight inline-flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
      >
        <span className="sr-only">Toggle theme</span>
        <div className="flex h-4 w-4 items-center justify-center">
          {darkTheme === null ? null : darkTheme ? (
            <Moon className="size-4" />
          ) : (
            <Sun className="size-4" />
          )}
        </div>
      </button>

      {menuOpen ? (
        <div
          id={menuId}
          role="menu"
          aria-label="Theme"
          onKeyDown={onMenuKeyDown}
          className="absolute right-0 top-[calc(100%+0.5rem)] z-50 min-w-32 rounded-lg border border-border bg-popover p-1 text-popover-foreground shadow-[rgb(0_0_0_/_0.24)_0_0.1875rem_0.5rem]"
        >
          {THEME_OPTIONS.map((option, index) => {
            const selected = option.value === themePreference;

            return (
              <button
                key={option.value}
                ref={(element) => {
                  itemRefs.current[index] = element;
                }}
                type="button"
                role="menuitemradio"
                aria-checked={selected}
                className={cn(
                  "mqs-focus-highlight flex w-full items-center justify-between rounded-md px-2 py-1.5 text-sm transition-colors focus-visible:bg-accent focus-visible:text-accent-foreground",
                  selected
                    ? "bg-accent text-accent-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-foreground",
                )}
                onClick={() => selectTheme(option.value)}
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
