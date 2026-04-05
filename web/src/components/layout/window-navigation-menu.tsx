import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuList,
} from '@/components/ui/navigation-menu';
import { cn } from '@/lib/utils';

type WindowNavigationItem<TPageId extends string> = {
  readonly id: TPageId;
  readonly label: string;
};

type WindowNavigationAction = {
  readonly id: string;
  readonly label: string;
  readonly onAction: () => void | Promise<void>;
  readonly disabled?: boolean;
  readonly title?: string;
};

type WindowNavigationMenuProps<TPageId extends string> = {
  readonly items: readonly WindowNavigationItem<TPageId>[];
  readonly activeItem: TPageId;
  readonly onItemChange: (itemId: TPageId) => void;
  readonly actions?: readonly WindowNavigationAction[];
  readonly className?: string;
};

function getItemClassName(isActive: boolean): string {
  return cn(
    'inline-flex h-9 items-center justify-center rounded-md px-4.5 text-sm font-medium transition-colors outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50',
    isActive
      ? 'bg-accent text-primary'
      : 'text-muted-foreground hover:bg-accent/40 hover:text-foreground'
  );
}

export function WindowNavigationMenu<TPageId extends string>({
  items,
  activeItem,
  onItemChange,
  actions,
  className,
}: WindowNavigationMenuProps<TPageId>) {
  return (
    <NavigationMenu className={cn('justify-self-center', className)} viewport={false}>
      <NavigationMenuList className="gap-2 bg-transparent p-0">
        {items.map((item) => {
          const isActive = item.id === activeItem;

          return (
            <NavigationMenuItem key={item.id}>
              <button
                type="button"
                className={getItemClassName(isActive)}
                onClick={() => onItemChange(item.id)}
              >
                {item.label}
              </button>
            </NavigationMenuItem>
          );
        })}

        {actions?.map((action) => {
          return (
            <NavigationMenuItem key={action.id}>
              <button
                type="button"
                className={getItemClassName(false)}
                disabled={action.disabled}
                title={action.title}
                onClick={() => {
                  void action.onAction();
                }}
              >
                {action.label}
              </button>
            </NavigationMenuItem>
          );
        })}
      </NavigationMenuList>
    </NavigationMenu>
  );
}
