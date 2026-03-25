export type NavItemId = "scripts" | "console" | "settings";

export interface NavAnchorItem {
  id: Exclude<NavItemId, "settings">;
  label: string;
  href: string;
}

export interface SettingsMenuItem {
  id: "options" | "keybinds" | "commands";
  label: string;
  description: string;
  href: string;
}

export const UI_PAGES_PATH_MARKER = "/pages/";
export const UI_NAVIGATION_PAGE_ATTRIBUTE = "data-mqs-page-href";
export const ROOT_PAGE_ROOT_HREF = "./";
export const NESTED_PAGE_ROOT_HREF = "../";

export const UI_PAGE_HREFS = {
  scripts: "index.html",
  console: "console.html",
  options: "options.html",
  keybinds: "keybinds.html",
  commands: "commands.html",
} as const;

export const NAV_ITEMS: NavAnchorItem[] = [
  { id: "scripts", label: "Scripts", href: UI_PAGE_HREFS.scripts },
  { id: "console", label: "Console", href: UI_PAGE_HREFS.console },
];

export const SETTINGS_MENU_ITEMS: SettingsMenuItem[] = [
  {
    id: "options",
    label: "Options",
    description: "Open the settings page.",
    href: UI_PAGE_HREFS.options,
  },
  {
    id: "keybinds",
    label: "Keybinds",
    description: "Open the keybinds page.",
    href: UI_PAGE_HREFS.keybinds,
  },
  {
    id: "commands",
    label: "Commands",
    description: "Open the commands page.",
    href: UI_PAGE_HREFS.commands,
  },
];

export function resolveStaticUiPageHref(pageRootHref: string, pageHref: string) {
  return `${pageRootHref}${pageHref}`;
}

export function resolveRuntimeUiRootHref(currentHref: string) {
  try {
    const currentUrl = new URL(currentHref);
    const modsMatch = currentUrl.pathname.match(/^\/mods\/([^/]+)(?:\/.*)?$/);
    if (modsMatch) {
      currentUrl.pathname = `/mods/${modsMatch[1]}/`;
      currentUrl.search = "";
      currentUrl.hash = "";
      return currentUrl.toString();
    }

    const pagesIndex = currentUrl.pathname.indexOf(UI_PAGES_PATH_MARKER);
    if (pagesIndex >= 0) {
      currentUrl.pathname = currentUrl.pathname.slice(0, pagesIndex + UI_PAGES_PATH_MARKER.length);
      currentUrl.search = "";
      currentUrl.hash = "";
      return currentUrl.toString();
    }
  } catch (ignored) {
    return null;
  }

  return null;
}

export function resolveRuntimeUiPageHref(
  currentHref: string,
  pageRootHref: string,
  pageHref: string,
) {
  const runtimeRootHref = resolveRuntimeUiRootHref(currentHref);
  const baseHref = runtimeRootHref ?? pageRootHref;

  try {
    return new URL(pageHref, baseHref).toString();
  } catch (ignored) {
    return resolveStaticUiPageHref(pageRootHref, pageHref);
  }
}

export function createUiNavigationBootstrapScript(pageRootHref: string) {
  return `
(() => {
  const pageRootHref = ${JSON.stringify(pageRootHref)};
  const pageAttribute = ${JSON.stringify(UI_NAVIGATION_PAGE_ATTRIBUTE)};
  const pagesPathMarker = ${JSON.stringify(UI_PAGES_PATH_MARKER)};

  let rootHref = null;

  try {
    const currentUrl = new URL(window.location.href);
    const modsMatch = currentUrl.pathname.match(/^\\/mods\\/([^/]+)(?:\\/.*)?$/);
    if (modsMatch) {
      currentUrl.pathname = "/mods/" + modsMatch[1] + "/";
      currentUrl.search = "";
      currentUrl.hash = "";
      rootHref = currentUrl.toString();
    } else {
      const pagesIndex = currentUrl.pathname.indexOf(pagesPathMarker);
      if (pagesIndex >= 0) {
        currentUrl.pathname = currentUrl.pathname.slice(0, pagesIndex + pagesPathMarker.length);
        currentUrl.search = "";
        currentUrl.hash = "";
        rootHref = currentUrl.toString();
      }
    }
  } catch (ignored) {}

  const baseHref = rootHref ?? pageRootHref;

  for (const element of document.querySelectorAll("a[" + pageAttribute + "]")) {
    const pageHref = element.getAttribute(pageAttribute);
    if (!pageHref) {
      continue;
    }

    try {
      element.setAttribute("href", new URL(pageHref, baseHref).toString());
    } catch (ignored) {
      element.setAttribute("href", pageRootHref + pageHref);
    }
  }
})();`.trim();
}
