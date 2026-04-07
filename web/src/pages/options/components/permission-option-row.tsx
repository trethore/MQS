import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { cn } from '@/lib/utils';

type PermissionOptionRowProps = {
  readonly id: string;
  readonly title: string;
  readonly description: string;
  readonly checked: boolean;
  readonly disabled?: boolean;
  readonly onCheckedChange: (checked: boolean) => void;
};

export function PermissionOptionRow({
  id,
  title,
  description,
  checked,
  disabled,
  onCheckedChange,
}: PermissionOptionRowProps) {
  return (
    <Label
      htmlFor={id}
      className={cn(
        'flex w-full items-start gap-4 rounded-md cursor-pointer',
        disabled && 'cursor-not-allowed'
      )}
    >
      <div className="min-w-0 flex-1 space-y-1">
        <div className="text-sm font-semibold text-card-foreground">{title}</div>
        <p className="text-sm leading-6 text-muted-foreground">{description}</p>
      </div>

      <Switch
        id={id}
        size="lg"
        className="disabled:opacity-100"
        checked={checked}
        disabled={disabled}
        onCheckedChange={onCheckedChange}
        aria-label={title}
      />
    </Label>
  );
}
