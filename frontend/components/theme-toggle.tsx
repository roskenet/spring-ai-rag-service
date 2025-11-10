"use client"

import { useTheme } from "@/app/client-layout"
import { IconButton } from "@mui/material"
import { Brightness4, Brightness7 } from "@mui/icons-material"

export function ThemeToggle() {
  const { isDark, toggleTheme } = useTheme()

  return (
    <IconButton
      onClick={toggleTheme}
      color="inherit"
      title={isDark ? "Switch to light mode" : "Switch to dark mode"}
      size="small"
    >
      {isDark ? <Brightness7 /> : <Brightness4 />}
    </IconButton>
  )
}
