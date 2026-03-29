import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuList,
} from '@/components/ui/navigation-menu';
import { cn } from '@/lib/utils';

type WindowNavigationItem<TPageId extends string> = {
  id: TPageId;
  label: string;
};

type WindowNavigationMenuProps<TPageId extends string> = {
  items: readonly WindowNavigationItem<TPageId>[];
  activeItem: TPageId;
  onItemChange: (itemId: TPageId) => void;
  className?: string;
};

export function WindowNavigationMenu<TPageId extends string>({
  items,
  activeItem,
  onItemChange,
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
                className={cn(
                  'inline-flex h-9 items-center justify-center rounded-md px-4.5 text-sm font-medium transition-colors outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50',
                  isActive
                    ? 'bg-accent text-primary'
                    : 'text-muted-foreground hover:bg-accent/40 hover:text-foreground'
                )}
                onClick={() => onItemChange(item.id)}
              >
                {item.label}
              </button>
            </NavigationMenuItem>
          );
        })}
      </NavigationMenuList>
    </NavigationMenu>
  );
}
