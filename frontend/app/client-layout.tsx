"use client"

import type React from "react"
import { Geist, Geist_Mono } from "next/font/google"
import { Analytics } from "@vercel/analytics/next"
import { useState, useEffect, createContext, useContext } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { ThemeToggle } from "@/components/theme-toggle"

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
  const [isMounted, setIsMounted] = useState(false)

  useEffect(() => {
    setIsMounted(true)
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

  return <ThemeContext.Provider value={{ isDark, toggleTheme }}>{children}</ThemeContext.Provider>
}

function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()

  const isActive = (path: string) => {
    if (path === "/" && pathname === "/") return true
    if (path !== "/" && pathname.startsWith(path)) return true
    return false
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-secondary/20">
      {/* Header Navigation */}
      <nav className="border-b border-border backdrop-blur-md bg-background/80 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Logo */}
            <Link href="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
              <div className="w-8 h-8 bg-gradient-to-br from-primary to-secondary rounded-lg"></div>
              <span className="text-lg font-bold">ZEOS Knowledge</span>
            </Link>

            {/* Navigation Links */}
            <div className="flex items-center gap-4">
              <Link
                href="/chat"
                className={`text-sm transition-colors hover:text-primary ${
                  isActive("/chat") ? "font-semibold text-primary" : ""
                }`}
              >
                Chat
              </Link>
              <Link
                href="/knowledge-base"
                className={`text-sm transition-colors hover:text-primary ${
                  isActive("/knowledge-base") ? "font-semibold text-primary" : ""
                }`}
              >
                Knowledge Base
              </Link>
              <Link
                href="/analytics"
                className={`text-sm transition-colors hover:text-primary ${
                  isActive("/analytics") ? "font-semibold text-primary" : ""
                }`}
              >
                Analytics
              </Link>
              <Link
                href="/config"
                className={`text-sm transition-colors hover:text-primary ${
                  isActive("/config") ? "font-semibold text-primary" : ""
                }`}
              >
                Settings
              </Link>
              <ThemeToggle />
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>
    </div>
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
