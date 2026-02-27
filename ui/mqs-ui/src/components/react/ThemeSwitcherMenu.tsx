import type { KeyboardEvent as ReactKeyboardEvent } from "react";
import { Check } from "lucide-react";

import { cn } from "@/lib/utils";
import { THEME_OPTIONS, type ThemePreference } from "@/lib/theme/themeShared";

interface ThemeSwitcherMenuProps {
  menuId: string;
  themePreference: ThemePreference;
  itemRefs: { current: Array<HTMLButtonElement | null> };
  onMenuKeyDown: (event: ReactKeyboardEvent<HTMLDivElement>) => void;
  onSelectTheme: (themePreference: ThemePreference) => void;
}

export function ThemeSwitcherMenu({
  menuId,
  themePreference,
  itemRefs,
  onMenuKeyDown,
  onSelectTheme,
}: ThemeSwitcherMenuProps) {
  return (
    <div
      id={menuId}
      role="menu"
      aria-label="Theme"
      onKeyDown={onMenuKeyDown}
      className="mqs-menu-shadow absolute right-0 top-[calc(100%+0.5rem)] z-50 min-w-32 rounded-lg border border-border bg-popover p-1 text-popover-foreground"
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
            onClick={() => onSelectTheme(option.value)}
          >
            <span>{option.label}</span>
            {selected ? <Check className="size-4" /> : null}
          </button>
        );
      })}
    </div>
  );
}
