import * as React from "react"
import { PowerOff, RefreshCcw, Search } from "lucide-react"

import { ScriptItem } from "@/components/react/ScriptItem"
import { Button } from "@/components/ui/button"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@/components/ui/input-group"

interface ScriptEntry {
  id: string
  title: string
  path: string
  enabled: boolean
}

const placeholderScripts: ScriptEntry[] = [
  {
    id: "auto-fly",
    title: "Auto Fly",
    path: "movement/auto-fly.ts",
    enabled: true,
  },
  {
    id: "ore-scanner",
    title: "Ore Scanner",
    path: "world/ore-scanner.ts",
    enabled: false,
  },
  {
    id: "quick-totem",
    title: "Quick Totem",
    path: "combat/quick-totem.ts",
    enabled: true,
  },
]

function ScriptsPanel() {
  const [searchTerm, setSearchTerm] = React.useState("")
  const [scripts, setScripts] = React.useState<ScriptEntry[]>(placeholderScripts)

  const visibleScripts = React.useMemo(() => {
    const normalizedTerm = searchTerm.trim().toLowerCase()

    if (!normalizedTerm) {
      return scripts
    }

    return scripts.filter((script) => {
      return (
        script.title.toLowerCase().includes(normalizedTerm) ||
        script.path.toLowerCase().includes(normalizedTerm)
      )
    })
  }, [scripts, searchTerm])

  const handleToggleScript = (id: string, enabled: boolean) => {
    setScripts((currentScripts) => {
      return currentScripts.map((script) => {
        if (script.id !== id) {
          return script
        }

        return {
          ...script,
          enabled,
        }
      })
    })
  }

  return (
    <div className="mt-6">
      <div className="flex items-center gap-2">
        <InputGroup className="mqs-script-search h-10 flex-1 rounded-xl">
          <InputGroupAddon align="inline-start" className="pl-3">
            <Search className="size-4" />
          </InputGroupAddon>
          <InputGroupInput
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
            placeholder="Search scripts by name or path"
            aria-label="Search scripts"
          />
        </InputGroup>

        <Button
          type="button"
          variant="ghost"
          size="icon-lg"
          className="mqs-script-action mqs-nav-link rounded-xl"
          aria-label="Refresh scripts"
        >
          <RefreshCcw className="size-4" />
        </Button>

        <Button
          type="button"
          variant="ghost"
          size="icon-lg"
          className="mqs-script-action mqs-nav-link rounded-xl"
          aria-label="Disable all scripts"
        >
          <PowerOff className="size-4" />
        </Button>
      </div>

      <div className="mt-4 space-y-3">
        {visibleScripts.map((script) => {
          return (
            <ScriptItem
              key={script.id}
              id={script.id}
              title={script.title}
              path={script.path}
              enabled={script.enabled}
              onToggle={handleToggleScript}
            />
          )
        })}
      </div>
    </div>
  )
}

export { ScriptsPanel }
