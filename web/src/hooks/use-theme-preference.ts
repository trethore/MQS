import { useEffect, useRef, useState } from 'react';

const THEME_STORAGE_KEY = 'mqs-web-theme';
const THEME_TRANSITION_CLASS = 'theme-changing';

export type ThemePreference = 'light' | 'dark' | 'system';
export type ResolvedTheme = Exclude<ThemePreference, 'system'>;

function isThemePreference(value: string | null): value is ThemePreference {
  return value === 'light' || value === 'dark' || value === 'system';
}

function hasBrowserGlobals() {
  return typeof globalThis.matchMedia === 'function' && globalThis.localStorage !== undefined;
}

function getSystemTheme(): ResolvedTheme {
  if (!hasBrowserGlobals()) {
    return 'dark';
  }

  return globalThis.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function useThemePreference() {
  const [theme, setTheme] = useState<ThemePreference>(() => {
    if (!hasBrowserGlobals()) {
      return 'system';
    }

    const storedTheme = globalThis.localStorage.getItem(THEME_STORAGE_KEY);
    return isThemePreference(storedTheme) ? storedTheme : 'system';
  });
  const [systemTheme, setSystemTheme] = useState<ResolvedTheme>(getSystemTheme);
  const hasAppliedThemeRef = useRef(false);

  useEffect(() => {
    if (typeof globalThis.matchMedia !== 'function') {
      return;
    }

    const mediaQuery = globalThis.matchMedia('(prefers-color-scheme: dark)');

    const handleChange = () => {
      setSystemTheme(mediaQuery.matches ? 'dark' : 'light');
    };

    handleChange();
    mediaQuery.addEventListener('change', handleChange);

    return () => {
      mediaQuery.removeEventListener('change', handleChange);
    };
  }, []);

  const resolvedTheme = theme === 'system' ? systemTheme : theme;

  useEffect(() => {
    if (globalThis.document === undefined) {
      return;
    }

    const rootElement = globalThis.document.documentElement;
    rootElement.classList.toggle('dark', resolvedTheme === 'dark');
    rootElement.style.colorScheme = resolvedTheme;

    if (!hasAppliedThemeRef.current) {
      hasAppliedThemeRef.current = true;
      return;
    }

    rootElement.classList.add(THEME_TRANSITION_CLASS);
    const timeoutId = globalThis.setTimeout(() => {
      rootElement.classList.remove(THEME_TRANSITION_CLASS);
    }, 180);

    return () => {
      globalThis.clearTimeout(timeoutId);
      rootElement.classList.remove(THEME_TRANSITION_CLASS);
    };
  }, [resolvedTheme]);

  useEffect(() => {
    if (globalThis.localStorage === undefined) {
      return;
    }

    globalThis.localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  return {
    theme,
    resolvedTheme,
    setTheme,
  };
}
