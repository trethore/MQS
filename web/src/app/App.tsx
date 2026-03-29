import { Button } from '@/components/ui/button';
import { Window } from '@/components/layout/window';

function App() {
  return (
    <main className="dark flex min-h-screen items-center justify-center p-6">
      <Window>
        <Button type="button">Example button</Button>
      </Window>
    </main>
  );
}

export default App;
