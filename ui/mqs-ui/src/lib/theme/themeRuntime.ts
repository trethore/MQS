import {
  THEME_CHANGE_EVENT,
  THEME_STORAGE_KEY,
  parseThemePreference,
  resolveDarkTheme,
  type ThemeApi,
  type ThemePreference,
  type ThemeState,
} from "@/lib/theme/themeShared";

declare global {
  interface Window {
    __mqsTheme?: ThemeApi;
  }
}

function getStoredThemePreference(): ThemePreference {
  try {
    return parseThemePreference(window.localStorage.getItem(THEME_STORAGE_KEY));
  } catch (ignored) {
    return "system";
  }
}

function emitThemeChange(themeState: ThemeState): void {
  window.dispatchEvent(
    new CustomEvent(THEME_CHANGE_EVENT, {
      detail: themeState,
    }),
  );
}

export function getThemeApi(): ThemeApi | null {
  if (typeof window === "undefined") {
    return null;
  }

  return window.__mqsTheme ?? null;
}

export function installThemeRuntime(): ThemeApi | null {
  if (typeof window === "undefined") {
    return null;
  }

  const existingThemeApi = getThemeApi();
  if (existingThemeApi) {
    return existingThemeApi;
  }

  const root = document.documentElement;
  const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

  const getThemePreference = (): ThemePreference => {
    return getStoredThemePreference();
  };

  const applyTheme = (themePreference: ThemePreference = getThemePreference()): ThemeState => {
    const normalizedThemePreference = parseThemePreference(themePreference);
    const darkTheme = resolveDarkTheme(normalizedThemePreference, mediaQuery.matches);

    root.classList.toggle("dark", darkTheme);

    return {
      themePreference: normalizedThemePreference,
      darkTheme,
    };
  };

  const setThemePreference = (themePreference: ThemePreference): ThemeState => {
    const normalizedThemePreference = parseThemePreference(themePreference);

    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, normalizedThemePreference);
    } catch (ignored) {}

    const themeState = applyTheme(normalizedThemePreference);
    emitThemeChange(themeState);

    return themeState;
  };

  const themeApi: ThemeApi = {
    getThemePreference,
    applyTheme,
    setThemePreference,
  };

  window.__mqsTheme = themeApi;
  themeApi.applyTheme();

  mediaQuery.addEventListener("change", () => {
    if (getThemePreference() !== "system") {
      return;
    }

    const themeState = applyTheme("system");
    emitThemeChange(themeState);
  });

  return themeApi;
}

export function ensureThemeApi(): ThemeApi | null {
  return getThemeApi() ?? installThemeRuntime();
}
