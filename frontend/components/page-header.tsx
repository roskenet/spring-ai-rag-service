import React from "react"

interface PageHeaderProps {
  title: string
  description?: string
  className?: string
  children?: React.ReactNode
}

export function PageHeader({ title, description, className = "", children }: PageHeaderProps) {
  return (
    <div className={`space-y-2 ${className}`}>
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-primary via-secondary to-accent bg-clip-text text-transparent">
            {title}
          </h1>
          {description && (
            <p className="text-muted-foreground mt-2">{description}</p>
          )}
        </div>
        {children && (
          <div className="ml-4">
            {children}
          </div>
        )}
      </div>
    </div>
  )
}