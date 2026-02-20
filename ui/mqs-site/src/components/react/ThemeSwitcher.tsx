import * as React from "react"
import { Moon, Sun } from "lucide-react"

import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

type ThemeChoice = "light" | "dark" | "system"
type ResolvedTheme = "light" | "dark"

const STORAGE_KEY = "mqs-theme"

function isThemeChoice(value: string | null): value is ThemeChoice {
  return value === "light" || value === "dark" || value === "system"
}

function getSystemTheme(): ResolvedTheme {
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"
}

function resolveTheme(choice: ThemeChoice): ResolvedTheme {
  return choice === "system" ? getSystemTheme() : choice
}

function applyTheme(theme: ResolvedTheme): void {
  document.documentElement.classList.toggle("dark", theme === "dark")
}

function readStoredTheme(): ThemeChoice {
  try {
    const storedTheme = localStorage.getItem(STORAGE_KEY)
    return isThemeChoice(storedTheme) ? storedTheme : "system"
  } catch (ignored) {
    return "system"
  }
}

function writeStoredTheme(choice: ThemeChoice): void {
  try {
    localStorage.setItem(STORAGE_KEY, choice)
  } catch (ignored) {}
}

function ThemeSwitcher() {
  const [open, setOpen] = React.useState(false)
  const [choice, setChoice] = React.useState<ThemeChoice>("system")
  const [resolvedTheme, setResolvedTheme] = React.useState<ResolvedTheme>("dark")
  const rootRef = React.useRef<HTMLDivElement>(null)

  React.useEffect(() => {
    const initialChoice = readStoredTheme()
    const initialResolvedTheme = resolveTheme(initialChoice)

    setChoice(initialChoice)
    setResolvedTheme(initialResolvedTheme)
    applyTheme(initialResolvedTheme)
  }, [])

  React.useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)")

    const handleSystemThemeChange = () => {
      if (choice !== "system") {
        return
      }

      const nextTheme = getSystemTheme()
      setResolvedTheme(nextTheme)
      applyTheme(nextTheme)
    }

    mediaQuery.addEventListener("change", handleSystemThemeChange)
    return () => mediaQuery.removeEventListener("change", handleSystemThemeChange)
  }, [choice])

  React.useEffect(() => {
    if (!open) {
      return
    }

    const handleClickOutside = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false)
      }
    }

    document.addEventListener("mousedown", handleClickOutside)
    document.addEventListener("keydown", handleEscape)

    return () => {
      document.removeEventListener("mousedown", handleClickOutside)
      document.removeEventListener("keydown", handleEscape)
    }
  }, [open])

  const selectTheme = (nextChoice: ThemeChoice) => {
    const nextResolvedTheme = resolveTheme(nextChoice)

    setChoice(nextChoice)
    setResolvedTheme(nextResolvedTheme)
    applyTheme(nextResolvedTheme)
    writeStoredTheme(nextChoice)
    setOpen(false)
  }

  return (
    <div ref={rootRef} className="relative shrink-0">
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        className="mqs-nav-link"
        aria-label="Theme options"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((isOpen) => !isOpen)}
      >
        <Sun className={cn("size-4", resolvedTheme === "dark" ? "hidden" : "block")} />
        <Moon className={cn("size-4", resolvedTheme === "light" ? "hidden" : "block")} />
      </Button>

      {open && (
        <div className="mqs-theme-menu absolute right-0 top-[calc(100%+0.25rem)] z-30 min-w-36 rounded-xl p-1" role="menu" aria-label="Theme">
          <button
            type="button"
            className={cn("mqs-theme-item", choice === "light" && "mqs-theme-item-active")}
            onClick={() => selectTheme("light")}
            role="menuitemradio"
            aria-checked={choice === "light"}
          >
            Light
          </button>
          <button
            type="button"
            className={cn("mqs-theme-item", choice === "dark" && "mqs-theme-item-active")}
            onClick={() => selectTheme("dark")}
            role="menuitemradio"
            aria-checked={choice === "dark"}
          >
            Dark
          </button>
          <button
            type="button"
            className={cn("mqs-theme-item", choice === "system" && "mqs-theme-item-active")}
            onClick={() => selectTheme("system")}
            role="menuitemradio"
            aria-checked={choice === "system"}
          >
            System
          </button>
        </div>
      )}
    </div>
  )
}

export { ThemeSwitcher }
