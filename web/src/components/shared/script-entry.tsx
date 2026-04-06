import { useId } from 'react';

import { Switch } from '@/components/ui/switch';
import { cn } from '@/lib/utils';

type ScriptEntryProps = {
  name: string;
  version: string | null;
  path: string;
  enabled: boolean;
  disabled?: boolean;
  onEnabledChange: (enabled: boolean) => void;
  className?: string;
};

function formatScriptBreadcrumbs(path: string) {
  const normalizedPath = path.replaceAll('\\', '/');
  const segments = normalizedPath.split('/').filter(Boolean);

  if (segments.length === 0) {
    return path;
  }

  const visibleSegments = segments.slice(-4);
  const prefix = segments.length > visibleSegments.length ? '.../' : '';

  return `${prefix}${visibleSegments.join('/')}`;
}

function ScriptEntry({
  name,
  version,
  path,
  enabled,
  disabled = false,
  onEnabledChange,
  className,
}: ScriptEntryProps) {
  const titleId = useId();
  const pathId = useId();
  const breadcrumbPath = formatScriptBreadcrumbs(path);

  const handleToggle = () => {
    if (disabled) {
      return;
    }

    onEnabledChange(!enabled);
  };

  return (
    <div
      role="switch"
      tabIndex={disabled ? -1 : 0}
      aria-checked={enabled}
      aria-disabled={disabled}
      aria-labelledby={titleId}
      aria-describedby={pathId}
      onClick={handleToggle}
      onKeyDown={(event) => {
        if (event.key !== 'Enter' && event.key !== ' ') {
          return;
        }

        event.preventDefault();
        handleToggle();
      }}
      className={cn(
        'relative z-10 flex items-center justify-between gap-4 rounded-xl border border-border bg-background px-5 py-3.5 shadow-xs outline-none transition-[border-color,box-shadow,opacity] focus-visible:border-border focus-visible:ring-[3px] focus-visible:ring-ring/50 dark:border-input dark:bg-input/30 dark:focus-visible:border-input',
        disabled
          ? 'cursor-not-allowed opacity-60'
          : 'cursor-pointer hover:border-border dark:hover:border-input',
        className
      )}
    >
      <div className="min-w-0 flex-1">
        <div className="flex min-w-0 flex-wrap items-baseline gap-x-2 gap-y-1">
          <h3
            id={titleId}
            className="truncate text-lg font-semibold tracking-tight text-card-foreground"
          >
            {name}
          </h3>
          {version ? (
            <span className="text-sm font-medium text-muted-foreground">v{version}</span>
          ) : null}
        </div>

        <p id={pathId} className="mt-1 truncate text-sm text-muted-foreground" title={path}>
          {breadcrumbPath}
        </p>
      </div>

      <div className="flex shrink-0 items-center justify-end">
        <Switch
          size="lg"
          checked={enabled}
          tabIndex={-1}
          aria-hidden="true"
          className="pointer-events-none focus-visible:border-transparent"
        />
      </div>
    </div>
  );
}

export { ScriptEntry };
