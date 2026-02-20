import {
  NavigationMenu,
  NavigationMenuList,
  NavigationMenuItem,
  NavigationMenuLink,
  navigationMenuTriggerStyle,
} from "@/components/ui/navigation-menu"
import { cn } from "@/lib/utils"

const links = [
  { href: "/scripts/", label: "Scripts" },
  { href: "/console/", label: "Console" },
  { href: "/settings/", label: "Settings" },
] as const

interface NavLinksProps {
  currentPath?: string
}

function NavLinks({ currentPath = "/scripts/" }: NavLinksProps) {
  return (
    <NavigationMenu viewport={false}>
      <NavigationMenuList className="gap-0.5">
        {links.map((link) => {
          const isActive = currentPath === link.href

          return (
            <NavigationMenuItem key={link.href}>
              <NavigationMenuLink
                href={link.href}
                active={isActive}
                className={cn(
                  navigationMenuTriggerStyle(),
                  "mqs-nav-link h-8 bg-transparent px-3 text-sm"
                )}
              >
                {link.label}
              </NavigationMenuLink>
            </NavigationMenuItem>
          )
        })}
      </NavigationMenuList>
    </NavigationMenu>
  )
}

export { NavLinks }
