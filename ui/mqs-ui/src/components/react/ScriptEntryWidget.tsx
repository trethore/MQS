import { useState } from "react";

import { cn } from "@/lib/utils";

import { ScriptEntrySwitch } from "./ScriptEntrySwitch";

interface ScriptEntryWidgetProps {
  title: string;
  path: string;
  enabled?: boolean;
}

export function ScriptEntryWidget({ title, path, enabled = false }: ScriptEntryWidgetProps) {
  const [checked, setChecked] = useState(enabled);
  const [hovered, setHovered] = useState(false);

  const toggleChecked = () => {
    setChecked((previous) => !previous);
  };

  return (
    <article
      role="button"
      tabIndex={0}
      aria-pressed={checked}
      onClick={toggleChecked}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          toggleChecked();
        }
      }}
      onPointerEnter={() => {
        setHovered(true);
      }}
      onPointerLeave={() => {
        setHovered(false);
      }}
      className={cn(
        "flex cursor-pointer items-center justify-between gap-5 rounded-xl border border-border px-5 py-4 shadow-[0_0.75rem_1.75rem_-1.15rem_rgb(0_0_0_/_0.78)] transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring",
        hovered ? "bg-background/60" : "bg-background/50"
      )}
    >
      <div className="min-w-0 flex-1">
        <h2 className="truncate text-2xl font-semibold leading-tight text-foreground">{title}</h2>
        <p className="mt-1 truncate text-sm text-muted-foreground">{path}</p>
      </div>

      <div className="pointer-events-none shrink-0" aria-hidden="true">
        <ScriptEntrySwitch checked={checked} tabIndex={-1} />
      </div>
    </article>
  );
}
