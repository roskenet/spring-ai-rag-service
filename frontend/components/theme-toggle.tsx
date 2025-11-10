"use client"

import { useTheme } from "@/app/client-layout"
import { Button } from "@/components/ui/button"

export function ThemeToggle() {
  const { isDark, toggleTheme } = useTheme()

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={toggleTheme}
      className="border-border hover:bg-background hover:text-foreground bg-transparent"
      title={isDark ? "Switch to light mode" : "Switch to dark mode"}
    >
      {isDark ? "☀️" : "🌙"}
    </Button>
  )
}
