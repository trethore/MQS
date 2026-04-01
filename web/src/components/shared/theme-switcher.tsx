import { MonitorIcon, MoonIcon, SunIcon } from 'lucide-react';

import type { ResolvedTheme, ThemePreference } from '@/hooks/use-theme-preference';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

type ThemeSwitcherProps = {
  theme: ThemePreference;
  resolvedTheme: ResolvedTheme;
  onThemeChange: (theme: ThemePreference) => void;
};

const THEME_OPTIONS = [
  { id: 'light', label: 'Light' },
  { id: 'dark', label: 'Dark' },
  { id: 'system', label: 'System' },
] as const satisfies ReadonlyArray<{
  id: ThemePreference;
  label: string;
}>;

type ThemeTriggerIconProps = {
  theme: ThemePreference;
  resolvedTheme: ResolvedTheme;
};

function ThemeTriggerIcon({ theme, resolvedTheme }: ThemeTriggerIconProps) {
  if (theme === 'system') {
    return <MonitorIcon className="size-4" />;
  }

  return resolvedTheme === 'dark' ? (
    <MoonIcon className="size-4" />
  ) : (
    <SunIcon className="size-4" />
  );
}

export function ThemeSwitcher({ theme, resolvedTheme, onThemeChange }: ThemeSwitcherProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          className="rounded-md text-muted-foreground hover:bg-accent/40 hover:text-foreground data-[state=open]:text-primary"
        >
          <ThemeTriggerIcon theme={theme} resolvedTheme={resolvedTheme} />
          <span className="sr-only">Open theme menu</span>
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        align="end"
        className="w-40 rounded-xl border-border bg-card text-card-foreground shadow-sm dark:border-input"
        onCloseAutoFocus={(event) => {
          event.preventDefault();

          const activeElement = document.activeElement;
          if (activeElement instanceof HTMLElement) {
            activeElement.blur();
          }
        }}
      >
        <DropdownMenuLabel>Theme</DropdownMenuLabel>
        <DropdownMenuSeparator className="bg-border dark:bg-input" />

        <DropdownMenuRadioGroup
          value={theme}
          onValueChange={(value) => onThemeChange(value as ThemePreference)}
        >
          {THEME_OPTIONS.map((option) => {
            return (
              <DropdownMenuRadioItem key={option.id} value={option.id} className="rounded-md">
                <span>{option.label}</span>
              </DropdownMenuRadioItem>
            );
          })}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
