import { ThemeSwitcher } from '@/components/shared/theme-switcher';
import type { ResolvedTheme, ThemePreference } from '@/hooks/use-theme-preference';
import { Card } from '@/components/ui/card';
import { WindowNavigationMenu } from '@/components/layout/window-navigation-menu';
import { cn } from '@/lib/utils';

const WINDOW_NAV_ITEMS = [
  { id: 'scripts', label: 'Scripts' },
  { id: 'console', label: 'Console' },
  { id: 'options', label: 'Options' },
] as const;

export type WindowPageId = (typeof WINDOW_NAV_ITEMS)[number]['id'];

type WindowNavbarProps = {
  activePage: WindowPageId;
  onPageChange: (pageId: WindowPageId) => void;
  theme: ThemePreference;
  resolvedTheme: ResolvedTheme;
  onThemeChange: (theme: ThemePreference) => void;
  className?: string;
};

export function WindowNavbar({
  activePage,
  onPageChange,
  theme,
  resolvedTheme,
  onThemeChange,
  className,
}: WindowNavbarProps) {
  return (
    <Card
      className={cn(
        'gap-0 rounded-xl border-(--window-navbar-border) bg-card py-2 shadow-sm',
        className
      )}
    >
      <div className="grid min-h-12 grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-4 px-5">
        <div className="min-w-0">
          <h1 className="text-base font-semibold text-card-foreground">
            My <span className="text-primary">QOL</span> Scripts
          </h1>
        </div>

        <WindowNavigationMenu
          items={WINDOW_NAV_ITEMS}
          activeItem={activePage}
          onItemChange={onPageChange}
        />

        <div className="flex justify-end">
          <ThemeSwitcher
            theme={theme}
            resolvedTheme={resolvedTheme}
            onThemeChange={onThemeChange}
          />
        </div>
      </div>
    </Card>
  );
}
