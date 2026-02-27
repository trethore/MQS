import {
  useEffect,
  useId,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
} from "react";
import { Moon, Sun } from "lucide-react";

import { useThemePreference } from "@/hooks/useThemePreference";
import { THEME_OPTIONS, type ThemePreference } from "@/lib/theme/themeShared";

import { ThemeSwitcherMenu } from "./ThemeSwitcherMenu";

const MENU_NAVIGATION_KEYS = new Set(["ArrowDown", "ArrowUp", "Home", "End"]);

export function ThemeSwitcher() {
  const [menuOpen, setMenuOpen] = useState(false);
  const { darkTheme, setThemePreference, themePreference } = useThemePreference();
  const rootRef = useRef<HTMLDivElement>(null);
  const toggleButtonRef = useRef<HTMLButtonElement>(null);
  const itemRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const menuId = useId();
  const selectedOptionIndex = Math.max(
    0,
    THEME_OPTIONS.findIndex((option) => option.value === themePreference),
  );
  const themeIcon = (() => {
    if (darkTheme === true) {
      return <Moon className="size-4" />;
    }

    if (darkTheme === false) {
      return <Sun className="size-4" />;
    }

    return null;
  })();

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

    const focusSelectedOption = globalThis.requestAnimationFrame(() => {
      itemRefs.current[selectedOptionIndex]?.focus();
    });

    return () => {
      globalThis.cancelAnimationFrame(focusSelectedOption);
    };
  }, [menuOpen, selectedOptionIndex]);

  const selectTheme = (nextThemePreference: ThemePreference) => {
    setThemePreference(nextThemePreference);
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

    const focusedIndex =
      document.activeElement instanceof HTMLButtonElement
        ? itemRefs.current.indexOf(document.activeElement)
        : -1;
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
        <div className="flex h-4 w-4 items-center justify-center">{themeIcon}</div>
      </button>

      {menuOpen ? (
        <ThemeSwitcherMenu
          menuId={menuId}
          themePreference={themePreference}
          itemRefs={itemRefs}
          onMenuKeyDown={onMenuKeyDown}
          onSelectTheme={selectTheme}
        />
      ) : null}
    </div>
  );
}
