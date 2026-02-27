export type ThemePreference = "dark" | "light" | "system";

export interface ThemeState {
  themePreference: ThemePreference;
  darkTheme: boolean;
}

export interface ThemeApi {
  getThemePreference: () => ThemePreference;
  applyTheme: (themePreference?: ThemePreference) => ThemeState;
  setThemePreference: (themePreference: ThemePreference) => ThemeState;
}

export const THEME_STORAGE_KEY = "theme";
export const THEME_CHANGE_EVENT = "mqs-themechange";

export const THEME_OPTIONS: ReadonlyArray<{ label: string; value: ThemePreference }> = [
  { label: "Dark", value: "dark" },
  { label: "Light", value: "light" },
  { label: "System", value: "system" },
];

export function parseThemePreference(value: unknown): ThemePreference {
  if (value === "dark" || value === "light" || value === "system") {
    return value;
  }

  return "system";
}

export function resolveDarkTheme(themePreference: ThemePreference, prefersDark: boolean): boolean {
  return themePreference === "dark" || (themePreference === "system" && prefersDark);
}

export function isThemeState(value: unknown): value is ThemeState {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const maybeThemeState = value as Partial<ThemeState>;
  return (
    typeof maybeThemeState.darkTheme === "boolean" &&
    (maybeThemeState.themePreference === "dark" ||
      maybeThemeState.themePreference === "light" ||
      maybeThemeState.themePreference === "system")
  );
}
