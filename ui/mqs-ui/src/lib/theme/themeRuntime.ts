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

function isStorageAccessError(error_: unknown): boolean {
  return error_ instanceof DOMException;
}

function getStoredThemePreference(): ThemePreference {
  try {
    return parseThemePreference(globalThis.window.localStorage.getItem(THEME_STORAGE_KEY));
  } catch (error_) {
    if (isStorageAccessError(error_)) {
      return "system";
    }

    throw error_;
  }
}

function emitThemeChange(themeState: ThemeState): void {
  globalThis.dispatchEvent(
    new CustomEvent(THEME_CHANGE_EVENT, {
      detail: themeState,
    }),
  );
}

export function getThemeApi(): ThemeApi | null {
  if (globalThis.window === undefined) {
    return null;
  }

  return globalThis.window.__mqsTheme ?? null;
}

export function installThemeRuntime(): ThemeApi | null {
  if (globalThis.window === undefined) {
    return null;
  }

  const existingThemeApi = getThemeApi();
  if (existingThemeApi) {
    return existingThemeApi;
  }

  const root = document.documentElement;
  const mediaQuery = globalThis.window.matchMedia("(prefers-color-scheme: dark)");

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
      globalThis.window.localStorage.setItem(THEME_STORAGE_KEY, normalizedThemePreference);
    } catch (error_) {
      if (!isStorageAccessError(error_)) {
        throw error_;
      }
    }

    const themeState = applyTheme(normalizedThemePreference);
    emitThemeChange(themeState);

    return themeState;
  };

  const themeApi: ThemeApi = {
    getThemePreference,
    applyTheme,
    setThemePreference,
  };

  globalThis.window.__mqsTheme = themeApi;
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
