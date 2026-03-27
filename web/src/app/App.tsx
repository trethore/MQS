import { AppShell } from '@/components/layout';
import { APP_ROUTES, type RouteId, useCurrentRoute } from './router';

const PAGE_CONTENT: Record<RouteId, { eyebrow: string; body: string }> = {
  scripts: {
    eyebrow: 'Feature foundation',
    body: 'This page will host the script list, lifecycle actions, and script-specific details.',
  },
  console: {
    eyebrow: 'Feature foundation',
    body: 'This page will host log streaming, filters, and interactive console actions.',
  },
  options: {
    eyebrow: 'Feature foundation',
    body: 'This page will host global MQS settings, host toggles, and future configuration panels.',
  },
};

function App() {
  const currentRoute = useCurrentRoute();
  const currentPage = PAGE_CONTENT[currentRoute.id];
  const navigationItems = APP_ROUTES.map((route) => ({
    href: route.href,
    label: route.label,
    isActive: route.id === currentRoute.id,
  }));

  return (
    <AppShell
      title={currentRoute.title}
      description={currentRoute.description}
      navigationItems={navigationItems}
    >
      <section className="space-y-4">
        <div className="inline-flex rounded-full border border-border bg-muted px-3 py-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {currentPage.eyebrow}
        </div>
        <div className="space-y-2">
          <h3 className="text-xl font-semibold text-card-foreground">{currentRoute.label} page</h3>
          <p className="max-w-2xl text-sm leading-6 text-muted-foreground">{currentPage.body}</p>
        </div>
      </section>
    </AppShell>
  );
}

export default App;
