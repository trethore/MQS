import { useEffect, useState } from 'react';

export type RouteId = 'scripts' | 'console' | 'options';

export type AppRoute = {
  id: RouteId;
  href: `#/${RouteId}`;
  label: string;
  title: string;
  description: string;
};

const DEFAULT_ROUTE_ID: RouteId = 'scripts';

export const APP_ROUTES: AppRoute[] = [
  {
    id: 'scripts',
    href: '#/scripts',
    label: 'Scripts',
    title: 'Scripts',
    description: 'Manage discovered scripts and their lifecycle.',
  },
  {
    id: 'console',
    href: '#/console',
    label: 'Console',
    title: 'Console',
    description: 'Inspect script logs and interactive output.',
  },
  {
    id: 'options',
    href: '#/options',
    label: 'Options',
    title: 'Options',
    description: 'Configure host settings and script defaults.',
  },
];

export function getRouteFromHash(hash: string): AppRoute {
  const normalizedHash = hash.replace(/^#\/?/, '').trim().toLowerCase();
  const route = APP_ROUTES.find((candidate) => candidate.id === normalizedHash);

  if (route !== undefined) {
    return route;
  }

  return APP_ROUTES.find((candidate) => candidate.id === DEFAULT_ROUTE_ID) ?? APP_ROUTES[0];
}

export function useCurrentRoute(): AppRoute {
  const [route, setRoute] = useState<AppRoute>(() => getRouteFromHash(globalThis.location.hash));

  useEffect(() => {
    const handleHashChange = (): void => {
      setRoute(getRouteFromHash(globalThis.location.hash));
    };

    globalThis.addEventListener('hashchange', handleHashChange);

    return () => {
      globalThis.removeEventListener('hashchange', handleHashChange);
    };
  }, []);

  return route;
}
