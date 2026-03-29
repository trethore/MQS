import { useState } from 'react';

import { useThemePreference } from '@/hooks/use-theme-preference';
import { WindowNavbar, type WindowPageId } from '@/components/layout/window-navbar';
import { Button } from '@/components/ui/button';
import { Window } from '@/components/layout/window';

function App() {
  const [activePage, setActivePage] = useState<WindowPageId>('scripts');
  const { theme, resolvedTheme, setTheme } = useThemePreference();

  return (
    <main className="flex min-h-screen items-center justify-center p-6">
      <Window
        navbar={
          <WindowNavbar
            activePage={activePage}
            onPageChange={setActivePage}
            theme={theme}
            resolvedTheme={resolvedTheme}
            onThemeChange={setTheme}
          />
        }
      >
        <Button type="button">Example button</Button>
      </Window>
    </main>
  );
}

export default App;
