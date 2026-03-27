import type { ReactNode } from 'react';

type NavigationItem = {
  href: string;
  label: string;
  isActive: boolean;
};

type AppShellProps = {
  title: string;
  description: string;
  navigationItems: NavigationItem[];
  children: ReactNode;
};

export function AppShell({ title, description, navigationItems, children }: AppShellProps) {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col gap-6 px-6 py-8 lg:flex-row lg:px-8">
        <aside className="w-full shrink-0 lg:w-64">
          <div className="sticky top-8 rounded-2xl border border-border bg-card p-4 shadow-sm">
            <div className="mb-6 space-y-1">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                My QOL Scripts
              </p>
              <h1 className="text-2xl font-semibold text-card-foreground">MQS Web</h1>
            </div>

            <nav className="flex flex-col gap-2">
              {navigationItems.map((item) => {
                const className = item.isActive
                  ? 'rounded-lg border border-primary bg-primary px-3 py-2 text-sm font-medium text-primary-foreground'
                  : 'rounded-lg border border-transparent px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:border-border hover:bg-accent hover:text-accent-foreground';

                return (
                  <a key={item.href} className={className} href={item.href}>
                    {item.label}
                  </a>
                );
              })}
            </nav>
          </div>
        </aside>

        <main className="flex-1">
          <div className="rounded-3xl border border-border bg-card p-6 shadow-sm">
            <header className="mb-6 space-y-2 border-b border-border pb-6">
              <h2 className="text-3xl font-semibold text-card-foreground">{title}</h2>
              <p className="max-w-2xl text-sm text-muted-foreground">{description}</p>
            </header>

            <div>{children}</div>
          </div>
        </main>
      </div>
    </div>
  );
}
