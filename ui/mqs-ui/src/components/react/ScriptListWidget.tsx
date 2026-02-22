import { ScriptEntryWidget } from "./ScriptEntryWidget";
import { ScrollbarWidget } from "./ScrollbarWidget";

const PLACEHOLDER_SCRIPTS = [
  { title: "Auto Totem", path: "combat/auto-totem.js", enabled: true },
  { title: "HUD Tweaks", path: "ui/hud-tweaks.js", enabled: true },
  { title: "Inventory Sort", path: "inventory/sort.js", enabled: false },
  { title: "Waypoint Tools", path: "travel/waypoint-tools.js", enabled: true },
  { title: "Combat Helpers", path: "combat/helpers.js", enabled: false },
  { title: "Mob Radar", path: "utility/mob-radar.js", enabled: true },
  { title: "Chest Logger", path: "world/chest-logger.js", enabled: false },
  { title: "Tool Saver", path: "inventory/tool-saver.js", enabled: true },
  { title: "Lag Spike Guard", path: "performance/lag-guard.js", enabled: false },
  { title: "Auto Eat", path: "survival/auto-eat.js", enabled: true },
];

export function ScriptListWidget() {
  return (
    <section className="relative flex h-full min-h-0 w-full flex-1 flex-col overflow-hidden">
      <ScrollbarWidget className="pl-0 pr-0 pt-1 pb-0">
        <ul className="flex min-h-full flex-col gap-3 pb-5 pr-2">
          {PLACEHOLDER_SCRIPTS.map((script) => (
            <li key={script.path} className="w-full">
              <ScriptEntryWidget title={script.title} path={script.path} enabled={script.enabled} />
            </li>
          ))}
        </ul>
      </ScrollbarWidget>

      <div
        aria-hidden="true"
        className="pointer-events-none absolute bottom-0 left-0 right-3 h-7 bg-linear-to-b from-card/0 via-card/30 to-card/70"
      />
    </section>
  );
}
