import { useState } from 'react';

import { useThemePreference } from '@/hooks/use-theme-preference';
import { WindowNavbar, type WindowPageId } from '@/components/layout/window-navbar';
import { Window } from '@/components/layout/window';
import { ScriptsPage } from '@/pages/scripts-page';

const DEFAULT_PAGE_ID: WindowPageId = 'scripts';

function renderPage(activePage: WindowPageId) {
  switch (activePage) {
    case 'scripts':
      return <ScriptsPage />;
    case 'console':
      return <div className="text-sm text-muted-foreground">Console page coming soon.</div>;
    case 'options':
      return <div className="text-sm text-muted-foreground">Options page coming soon.</div>;
  }
}

function App() {
  const [activePage, setActivePage] = useState<WindowPageId>(DEFAULT_PAGE_ID);
  const { theme, resolvedTheme, setTheme } = useThemePreference();

  return (
    <main className="flex min-h-screen items-center justify-center p-6">
      <Window
        contentClassName="items-start justify-start"
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
        {renderPage(activePage)}
      </Window>
    </main>
  );
}

export default App;
