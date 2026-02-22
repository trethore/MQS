import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
} from "@/components/ui/navigation-menu";
import { cn } from "@/lib/utils";

import { ThemeSwitcher } from "./ThemeSwitcher";

const NAV_ITEMS = ["Scripts", "Console", "Options"];

export function AppNavbar() {
  return (
    <header className="p-4 pb-0">
      <div className="mqs-elevated grid grid-cols-[1fr_auto_1fr] items-center gap-3 rounded-2xl border border-border bg-card px-5 py-3">
        <p className="text-sm font-semibold text-foreground">
          My <span className="bg-linear-to-r from-primary to-primary-2 bg-clip-text text-transparent">QOL</span>{" "}
          Scripts
        </p>

        <NavigationMenu className="max-w-none flex-none">
          <NavigationMenuList>
            {NAV_ITEMS.map((item, index) => (
              <NavigationMenuItem key={item}>
                <NavigationMenuLink
                  href="#"
                  aria-current={index === 0 ? "page" : undefined}
                  onClick={(event) => event.preventDefault()}
                  className={cn(
                    "inline-flex h-9 items-center rounded-md px-4 text-sm font-semibold transition-colors",
                    index === 0
                      ? "bg-accent/90 text-foreground"
                      : "text-muted-foreground hover:bg-accent hover:text-foreground"
                  )}
                >
                  {index === 0 ? (
                    <span className="bg-linear-to-r from-primary to-primary-2 bg-clip-text text-transparent">
                      {item}
                    </span>
                  ) : (
                    item
                  )}
                </NavigationMenuLink>
              </NavigationMenuItem>
            ))}
          </NavigationMenuList>
        </NavigationMenu>

        <div className="flex justify-self-end">
          <ThemeSwitcher />
        </div>
      </div>
    </header>
  );
}
