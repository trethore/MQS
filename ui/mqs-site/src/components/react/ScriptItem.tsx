import * as React from "react"

interface ScriptItemProps {
  id: string
  title: string
  path: string
  enabled: boolean
  onToggle: (id: string, enabled: boolean) => void
}

function ScriptItem({ id, title, path, enabled, onToggle }: ScriptItemProps) {
  const skipNextClickRef = React.useRef(false)

  const toggleScript = () => {
    onToggle(id, !enabled)
  }

  const handleFallbackMouseDown = (event: React.MouseEvent<HTMLElement>) => {
    if (event.button !== 0) {
      return
    }

    event.preventDefault()
    event.stopPropagation()
    skipNextClickRef.current = true
    toggleScript()
  }

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    event.stopPropagation()

    if (skipNextClickRef.current) {
      skipNextClickRef.current = false
      return
    }

    toggleScript()
  }

  return (
    <article
      role="button"
      tabIndex={0}
      className="mqs-script-item flex cursor-pointer items-center justify-between gap-4 rounded-2xl px-4 py-3 sm:px-5 sm:py-4"
      onMouseDown={handleFallbackMouseDown}
      onClick={handleClick}
      onKeyDown={(event) => {
        if (event.currentTarget !== event.target) {
          return
        }

        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault()
          toggleScript()
        }
      }}
    >
      <div className="min-w-0">
        <h2 className="truncate text-base font-semibold text-foreground sm:text-lg">{title}</h2>
        <p className="truncate text-xs text-muted-foreground sm:text-sm">{path}</p>
      </div>

      <div className="flex shrink-0 items-center gap-3">
        <button
          type="button"
          role="switch"
          aria-checked={enabled}
          aria-label={`Toggle ${title}`}
          data-state={enabled ? "checked" : "unchecked"}
          className="mqs-script-toggle"
          onMouseDown={handleFallbackMouseDown}
          onClick={handleClick}
        >
          <span className="mqs-script-toggle-thumb" />
        </button>
      </div>
    </article>
  )
}

export { ScriptItem }
