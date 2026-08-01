"use client"

import type React from "react"
import { Geist, Geist_Mono } from "next/font/google"
import { Analytics } from "@vercel/analytics/next"
import { useState, useEffect, createContext, useContext } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { ThemeProvider as MuiThemeProvider, CssBaseline, AppBar, Toolbar, Box, Typography, Container, Button, useTheme as useMuiTheme } from "@mui/material"
import { lightTheme, darkTheme } from "@/lib/mui-theme"
import { ThemeToggle } from "@/components/theme-toggle"
import { UserInfo } from "@/components/user-info"

const _geist = Geist({ subsets: ["latin"] })
const _geistMono = Geist_Mono({ subsets: ["latin"] })

interface ThemeContextType {
  isDark: boolean
  toggleTheme: () => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error("useTheme must be used within ThemeProvider")
  }
  return context
}

function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [isDark, setIsDark] = useState(false)

  useEffect(() => {
    const isDarkMode = localStorage.getItem("theme") === "dark"
    setIsDark(isDarkMode)
    if (isDarkMode) {
      document.documentElement.classList.add("dark")
    }
  }, [])

  const toggleTheme = () => {
    const newIsDark = !isDark
    setIsDark(newIsDark)
    localStorage.setItem("theme", newIsDark ? "dark" : "light")
    if (newIsDark) {
      document.documentElement.classList.add("dark")
    } else {
      document.documentElement.classList.remove("dark")
    }
  }

  // Always render the same tree structure (light theme on server/first paint).
  // isDark starts false so server and first client render both use lightTheme.
  // The useEffect above applies the saved preference after hydration.
  return (
    <ThemeContext.Provider value={{ isDark, toggleTheme }}>
      <MuiThemeProvider theme={isDark ? darkTheme : lightTheme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  )
}

function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const muiTheme = useMuiTheme()

  const navItems = [
    { href: '/chat', label: 'Chat' },
    { href: '/knowledge-base', label: 'Knowledge Base' },
    { href: '/analytics', label: 'Analytics' },
    { href: '/config', label: 'Settings' },
  ]

  const isActive = (path: string) => {
    if (path === "/" && pathname === "/") return true
    if (path !== "/" && pathname.startsWith(path)) return true
    return false
  }

  const NavLink = ({ href, label }: { href: string; label: string }) => (
    <Button
      component={Link}
      href={href}
      variant="text"
      size="small"
      sx={{
        color: isActive(href) ? 'common.white' : 'inherit',
        fontWeight: isActive(href) ? 600 : 500,
        backgroundColor: isActive(href) ? 'action.hover' : 'transparent',
        '&:hover': {
          backgroundColor: isActive(href) ? 'action.selected' : 'action.hover',
          color: 'common.white',
        },
        transition: muiTheme.transitions.create(['background-color', 'color'], {
          duration: muiTheme.transitions.duration.shorter,
        }),
        textTransform: 'none',
        fontSize: '0.9375rem',
      }}
    >
      {label}
    </Button>
  )

  return (
    <Box sx={{ minHeight: '100vh' }}>
      {/* Header Navigation */}
      <AppBar position="sticky" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ justifyContent: 'space-between', gap: 3, minHeight: 64 }}>
            {/* Logo */}
            <Link href="/" style={{ textDecoration: 'none', color: 'inherit' }}>
              <Box
                component="span"
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  flexShrink: 0,
                  transition: 'opacity 0.2s',
                  '&:hover': {
                    opacity: 0.8,
                  },
                }}
              >
                <Box
                  sx={{
                    width: 36,
                    height: 36,
                    background: 'linear-gradient(135deg, #2563eb, #7c3aed)',
                    borderRadius: 1,
                    flexShrink: 0,
                  }}
                />
                <Typography
                  variant="h6"
                  component="span"
                  sx={{
                    fontWeight: 700,
                    fontSize: '1.25rem',
                    letterSpacing: '-0.5px',
                  }}
                >
                  zKnowledge
                </Typography>
              </Box>
            </Link>

            {/* Navigation Links */}
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 0.5,
                flex: 1,
                justifyContent: 'center',
              }}
            >
              {navItems.map((item) => (
                <NavLink key={item.href} href={item.href} label={item.label} />
              ))}
            </Box>

            {/* Right Actions */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexShrink: 0 }}>
              <UserInfo />
              <ThemeToggle />
            </Box>
          </Toolbar>
        </Container>
      </AppBar>

      {/* Main Content */}
      <Container maxWidth="xl" sx={{ py: 4 }}>
        {children}
      </Container>
    </Box>
  )
}

export function ClientLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <>
      <ThemeProvider>
        <AppShell>{children}</AppShell>
      </ThemeProvider>
      <Analytics />
    </>
  )
}

export default ClientLayout
