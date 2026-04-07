import { useState } from 'react';

import { useThemePreference } from '@/hooks/use-theme-preference';
import { WindowNavbar, type WindowPageId } from '@/components/layout/window-navbar';
import { Window } from '@/components/layout/window';
import { ConsolePage } from '@/pages/console/console';
import { OptionsPage } from '@/pages/options/options';
import { ScriptsPage } from '@/pages/scripts/scripts';

const DEFAULT_PAGE_ID: WindowPageId = 'scripts';

function App() {
  const [activePage, setActivePage] = useState<WindowPageId>(DEFAULT_PAGE_ID);
  const [scriptsSearchValue, setScriptsSearchValue] = useState('');
  const [consoleCommandValue, setConsoleCommandValue] = useState('');
  const { theme, resolvedTheme, setTheme } = useThemePreference();

  const handlePageChange = (pageId: WindowPageId) => {
    setActivePage(pageId);
  };

  return (
    <main className="flex min-h-screen items-center justify-center p-6">
      <Window
        contentClassName="items-start justify-start"
        navbar={
          <WindowNavbar
            activePage={activePage}
            onPageChange={handlePageChange}
            theme={theme}
            resolvedTheme={resolvedTheme}
            onThemeChange={setTheme}
          />
        }
      >
        {activePage === 'scripts' && (
          <ScriptsPage
            searchValue={scriptsSearchValue}
            onSearchValueChange={setScriptsSearchValue}
          />
        )}
        {activePage === 'console' && (
          <ConsolePage
            commandValue={consoleCommandValue}
            onCommandValueChange={setConsoleCommandValue}
          />
        )}
        {activePage === 'options' && <OptionsPage />}
      </Window>
    </main>
  );
}

export default App;
