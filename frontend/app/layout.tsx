import type React from "react"
import type { Metadata } from "next"
import "./globals.css"
import { ClientLayout } from "./client-layout"
import { AppRouterCacheProvider } from "@mui/material-nextjs/v15-appRouter"

export const metadata: Metadata = {
  title: "zKnowledge - Intelligent Document Search",
  description: "Advanced RAG platform for intelligent document search and knowledge discovery",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="font-sans antialiased">
        <AppRouterCacheProvider options={{ enableCssLayer: true }}>
          <ClientLayout>{children}</ClientLayout>
        </AppRouterCacheProvider>
      </body>
    </html>
  )
}
