import { useCallback, useEffect, useState } from "react";

import { ensureThemeApi } from "@/lib/theme/themeRuntime";
import {
  isThemeState,
  THEME_CHANGE_EVENT,
  type ThemePreference,
  type ThemeState,
} from "@/lib/theme/themeShared";

interface UseThemePreferenceResult {
  themePreference: ThemePreference;
  darkTheme: boolean | null;
  setThemePreference: (themePreference: ThemePreference) => void;
}

export function useThemePreference(): UseThemePreferenceResult {
  const [themeState, setThemeState] = useState<ThemeState | null>(() => {
    const themeApi = ensureThemeApi();
    return themeApi ? themeApi.applyTheme() : null;
  });

  const themePreference = themeState?.themePreference ?? "system";
  const darkTheme = themeState?.darkTheme ?? null;

  useEffect(() => {
    const onThemeChange = (event: Event) => {
      if (event instanceof CustomEvent && isThemeState(event.detail)) {
        setThemeState(event.detail);
        return;
      }

      const themeApi = ensureThemeApi();
      if (!themeApi) {
        return;
      }

      setThemeState(themeApi.applyTheme());
    };

    window.addEventListener(THEME_CHANGE_EVENT, onThemeChange);

    return () => {
      window.removeEventListener(THEME_CHANGE_EVENT, onThemeChange);
    };
  }, []);

  const setThemePreference = useCallback((nextThemePreference: ThemePreference) => {
    const themeApi = ensureThemeApi();
    if (!themeApi) {
      return;
    }

    setThemeState(themeApi.setThemePreference(nextThemePreference));
  }, []);

  return {
    themePreference,
    darkTheme,
    setThemePreference,
  };
}
