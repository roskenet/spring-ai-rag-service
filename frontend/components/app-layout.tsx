"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { ThemeToggle } from "@/components/theme-toggle"
import { ReactNode } from "react"

interface AppLayoutProps {
  children: ReactNode
}

export function AppLayout({ children }: AppLayoutProps) {
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
              <span className="text-lg font-bold">RAG Chat</span>
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