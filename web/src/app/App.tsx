import { useState } from 'react';

import { WindowNavbar, type WindowPageId } from '@/components/layout/window-navbar';
import { Button } from '@/components/ui/button';
import { Window } from '@/components/layout/window';

function App() {
  const [activePage, setActivePage] = useState<WindowPageId>('scripts');

  return (
    <main className="dark flex min-h-screen items-center justify-center p-6">
      <Window navbar={<WindowNavbar activePage={activePage} onPageChange={setActivePage} />}>
        <Button type="button">Example button</Button>
      </Window>
    </main>
  );
}

export default App;
