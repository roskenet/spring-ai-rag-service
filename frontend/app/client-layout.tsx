"use client"

import type React from "react"
import { Geist, Geist_Mono } from "next/font/google"
import { Analytics } from "@vercel/analytics/next"
import { useState, useEffect, createContext, useContext } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { ThemeProvider as MuiThemeProvider, CssBaseline, AppBar, Toolbar, Box, Typography, Container } from "@mui/material"
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

  const isActive = (path: string) => {
    if (path === "/" && pathname === "/") return true
    if (path !== "/" && pathname.startsWith(path)) return true
    return false
  }

  const isMI = pathname.startsWith('/market-intelligence')

  return (
    <Box sx={isMI ? { height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' } : { minHeight: '100vh' }}>
      {/* Header Navigation */}
      <AppBar position="sticky" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider', flexShrink: 0 }}>
        <Container maxWidth="xl">
          <Toolbar sx={{ justifyContent: 'space-between' }}>
            {/* Logo */}
            <Link href="/" style={{ textDecoration: 'none', color: 'inherit' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Box
                  sx={{
                    width: 32,
                    height: 32,
                    background: 'linear-gradient(135deg, #2563eb, #7c3aed)',
                    borderRadius: 1,
                  }}
                />
                <Typography variant="h6" component="span" sx={{ fontWeight: 700 }}>
                  Alchemistic Knowledge
                </Typography>
              </Box>
            </Link>

            {/* Navigation Links */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Link href="/chat" style={{ textDecoration: 'none' }}>
                <Typography
                  variant="body2"
                  sx={{
                    color: isActive("/chat") ? 'primary.main' : 'text.primary',
                    fontWeight: isActive("/chat") ? 600 : 400,
                    '&:hover': { color: 'primary.main' },
                    transition: 'color 0.2s',
                  }}
                >
                  Chat
                </Typography>
              </Link>
              <Link href="/knowledge-base" style={{ textDecoration: 'none' }}>
                <Typography
                  variant="body2"
                  sx={{
                    color: isActive("/knowledge-base") ? 'primary.main' : 'text.primary',
                    fontWeight: isActive("/knowledge-base") ? 600 : 400,
                    '&:hover': { color: 'primary.main' },
                    transition: 'color 0.2s',
                  }}
                >
                  Knowledge Base
                </Typography>
              </Link>
              <Link href="/analytics" style={{ textDecoration: 'none' }}>
                <Typography
                  variant="body2"
                  sx={{
                    color: isActive("/analytics") ? 'primary.main' : 'text.primary',
                    fontWeight: isActive("/analytics") ? 600 : 400,
                    '&:hover': { color: 'primary.main' },
                    transition: 'color 0.2s',
                  }}
                >
                  Analytics
                </Typography>
              </Link>
              <Link href="/config" style={{ textDecoration: 'none' }}>
                <Typography
                  variant="body2"
                  sx={{
                    color: isActive("/config") ? 'primary.main' : 'text.primary',
                    fontWeight: isActive("/config") ? 600 : 400,
                    '&:hover': { color: 'primary.main' },
                    transition: 'color 0.2s',
                  }}
                >
                  Settings
                </Typography>
              </Link>
              <Link href="/market-intelligence" style={{ textDecoration: 'none' }}>
                <Typography
                  variant="body2"
                  sx={{
                    color: isActive("/market-intelligence") ? 'primary.main' : 'text.primary',
                    fontWeight: isActive("/market-intelligence") ? 600 : 400,
                    '&:hover': { color: 'primary.main' },
                    transition: 'color 0.2s',
                  }}
                >
                  Market Intel
                </Typography>
              </Link>
              <UserInfo />
              <ThemeToggle />
            </Box>
          </Toolbar>
        </Container>
      </AppBar>

      {/* Main Content — market-intelligence uses its own full-screen layout */}
      {isMI ? (
        <Box sx={{ flex: 1, minHeight: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          {children}
        </Box>
      ) : (
        <Container maxWidth="xl" sx={{ py: 4 }}>
          {children}
        </Container>
      )}
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
