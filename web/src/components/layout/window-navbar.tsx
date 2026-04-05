import { useCallback, useState } from 'react';

import { prepareCodeWorkspace } from '@/bridge/services/code-service';
import { VSCODE_WEB_URL } from '@/bridge/contracts/code';
import { isGrapheneBridgeInstalled } from '@/bridge/core/graphene-bridge';
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
  readonly activePage: WindowPageId;
  readonly onPageChange: (pageId: WindowPageId) => void;
  readonly theme: ThemePreference;
  readonly resolvedTheme: ResolvedTheme;
  readonly onThemeChange: (theme: ThemePreference) => void;
  readonly className?: string;
};

async function copyTextToClipboard(text: string): Promise<boolean> {
  if (text.trim().length === 0 || typeof globalThis.navigator?.clipboard?.writeText !== 'function') {
    return false;
  }

  try {
    await globalThis.navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}

export function WindowNavbar({
  activePage,
  onPageChange,
  theme,
  resolvedTheme,
  onThemeChange,
  className,
}: WindowNavbarProps) {
  const [isOpeningCode, setIsOpeningCode] = useState(false);

  const handleOpenCode = useCallback(async () => {
    setIsOpeningCode(true);

    try {
      if (isGrapheneBridgeInstalled()) {
        const preparation = await prepareCodeWorkspace();
        if (!preparation.copied) {
          await copyTextToClipboard(preparation.modDirPath);
        }
      }
    } catch (_error) {
      console.warn('Failed to prepare code workspace', _error);
    } finally {
      setIsOpeningCode(false);
      globalThis.location.assign(VSCODE_WEB_URL);
    }
  }, []);

  const navigationActions = [
    {
      id: 'code',
      label: 'Code',
      disabled: isOpeningCode,
      title: 'Open vscode.dev and copy the MQS mod directory path.',
      onAction: handleOpenCode,
    },
  ] as const;

  return (
    <Card
      className={cn('gap-0 rounded-xl bg-card py-2 shadow-sm', className)}
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
          actions={navigationActions}
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
