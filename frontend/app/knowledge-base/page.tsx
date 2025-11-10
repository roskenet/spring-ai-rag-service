"use client"

import type React from "react"
import { useState, useEffect } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { PageHeader } from "@/components/page-header"
import { PageContainer } from "@/components/page-container"
import { apiClient } from "@/lib/api"

interface DocumentMetadata {
  category: "technical-spec" | "api-doc" | "guide" | "faq" | "other"
  tags: string[]
  description?: string
}

interface Document {
  id: number
  filename: string
  title: string
  fileSize: number
  chunkCount: number
  status: string
  createdAt: string
  updatedAt: string
}

export default function KnowledgeBasePage() {
  const [documents, setDocuments] = useState<Document[]>([])
  const [isDragging, setIsDragging] = useState(false)
  const [expandedDoc, setExpandedDoc] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isUploading, setIsUploading] = useState(false)

  // Load documents from backend on component mount
  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      setIsLoading(true);
      const docs = await apiClient.getDocuments();
      setDocuments(docs);
    } catch (error) {
      console.error('Failed to load documents:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = () => {
    setIsDragging(false)
  }

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)

    const files = Array.from(e.dataTransfer.files)
    const supportedTypes = ["text/markdown", "text/plain", "application/pdf"]

    for (const file of files) {
      if (
        supportedTypes.some(
          (type) =>
            file.type.includes(type) ||
            file.name.endsWith(".md") ||
            file.name.endsWith(".txt") ||
            file.name.endsWith(".pdf"),
        )
      ) {
        await uploadFile(file);
      }
    }
  }

  const handleFileInput = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || [])
    const supportedTypes = ["text/markdown", "text/plain", "application/pdf"]

    for (const file of files) {
      if (
        supportedTypes.some(
          (type) =>
            file.type.includes(type) ||
            file.name.endsWith(".md") ||
            file.name.endsWith(".txt") ||
            file.name.endsWith(".pdf"),
        )
      ) {
        await uploadFile(file);
      }
    }

    // Clear the file input to allow re-selecting the same file
    e.target.value = '';
  }

  const uploadFile = async (file: File) => {
    try {
      setIsUploading(true);
      const response = await apiClient.uploadDocument(file);

      if (response.success) {
        // Reload documents to show the newly uploaded file
        await loadDocuments();
      } else {
        console.error('Upload failed:', response.message);
      }
    } catch (error) {
      console.error('Failed to upload file:', error);
    } finally {
      setIsUploading(false);
    }
  };

  const deleteDocument = async (id: number) => {
    try {
      await apiClient.deleteDocument(id);
      // Reload documents to reflect the deletion
      await loadDocuments();
    } catch (error) {
      console.error('Failed to delete document:', error);
    }
  }

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return "0 Bytes"
    const k = 1024
    const sizes = ["Bytes", "KB", "MB", "GB"]
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i]
  }

  const formatDate = (date: string | Date) => {
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    return new Intl.DateTimeFormat("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    }).format(dateObj)
  }

  const getCategoryInfo = (category: DocumentMetadata["category"]) => {
    const info: Record<string, { icon: string; label: string; color: string }> = {
      "technical-spec": { icon: "📋", label: "Technical Spec", color: "bg-blue-500/10 text-blue-600" },
      "api-doc": { icon: "🔌", label: "API Doc", color: "bg-purple-500/10 text-purple-600" },
      guide: { icon: "📖", label: "Guide", color: "bg-green-500/10 text-green-600" },
      faq: { icon: "❓", label: "FAQ", color: "bg-orange-500/10 text-orange-600" },
      other: { icon: "📎", label: "Other", color: "bg-gray-500/10 text-gray-600" },
    }
    return info[category] || info.other
  }

  const getTypeIcon = (type: string) => {
    switch (type) {
      case "pdf":
        return "📄"
      case "markdown":
        return "📝"
      case "txt":
        return "📋"
      default:
        return "📎"
    }
  }

  const totalChunks = documents.reduce((sum, doc) => sum + doc.chunkCount, 0)
  const totalSize = documents.reduce((sum, doc) => sum + doc.fileSize, 0)

  return (
    <PageContainer>
        <PageHeader
          title="Knowledge Base"
          description="Manage and organize your documents for RAG search"
        />

        {/* Stats */}
        <div className="grid md:grid-cols-3 gap-4">
          <Card className="p-4 border border-border bg-card/50 backdrop-blur">
            <p className="text-sm text-muted-foreground mb-1">Total Documents</p>
            <p className="text-3xl font-bold text-primary">{documents.length}</p>
          </Card>
          <Card className="p-4 border border-border bg-card/50 backdrop-blur">
            <p className="text-sm text-muted-foreground mb-1">Total Chunks</p>
            <p className="text-3xl font-bold text-secondary">{totalChunks}</p>
          </Card>
          <Card className="p-4 border border-border bg-card/50 backdrop-blur">
            <p className="text-sm text-muted-foreground mb-1">Storage Used</p>
            <p className="text-3xl font-bold text-accent">{formatFileSize(totalSize)}</p>
          </Card>
        </div>

        {/* Upload Area */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={`border-2 border-dashed rounded-lg p-8 text-center transition-all ${
            isDragging ? "border-primary bg-primary/10" : "border-border bg-card/30 hover:border-primary/50"
          }`}
        >
          <div className="space-y-2">
            {isUploading ? (
              <>
                <p className="text-2xl">⏳</p>
                <p className="font-semibold text-foreground">Uploading document...</p>
                <p className="text-sm text-muted-foreground mb-4">
                  Please wait while we process your file
                </p>
              </>
            ) : (
              <>
                <p className="text-2xl">📤</p>
                <p className="font-semibold text-foreground">Drag and drop your files here</p>
                <p className="text-sm text-muted-foreground mb-4">
                  Supported formats: Markdown (.md), Text (.txt), PDF (.pdf)
                </p>
                <div>
                  <input
                    id="file-upload"
                    type="file"
                    multiple
                    accept=".md,.txt,.pdf"
                    onChange={handleFileInput}
                    className="hidden"
                    disabled={isUploading}
                  />
                  <label htmlFor="file-upload" className="cursor-pointer">
                    <div className="inline-flex items-center justify-center rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 h-10 px-4 py-2 bg-gradient-to-r from-primary to-secondary hover:shadow-lg hover:shadow-primary/40 text-primary-foreground">
                      {isUploading ? "Uploading..." : "or Click to Browse"}
                    </div>
                  </label>
                </div>
              </>
            )}
          </div>
        </div>

        {/* Documents List */}
        <div>
          <h2 className="text-xl font-semibold mb-4">Your Documents</h2>
          {documents.length === 0 ? (
            <Card className="p-8 text-center border border-border/50 bg-card/30">
              <p className="text-muted-foreground">No documents yet. Upload your first document to get started!</p>
            </Card>
          ) : (
            <div className="space-y-3">
              {documents.map((doc) => {
                // Since backend doesn't return metadata, we'll provide defaults
                const categoryInfo = getCategoryInfo("other")
                const fileType = doc.filename.endsWith(".pdf") ? "pdf" :
                                doc.filename.endsWith(".md") ? "markdown" : "txt"
                return (
                  <Card
                    key={doc.id}
                    className="border border-border bg-card/50 backdrop-blur hover:border-primary/50 transition-all cursor-pointer"
                  >
                    <div className="p-4">
                      <div className="flex items-start justify-between mb-3">
                        <div className="flex items-center gap-4 flex-1">
                          <span className="text-2xl">{getTypeIcon(fileType)}</span>
                          <div className="flex-1">
                            <p className="font-semibold text-foreground">{doc.filename}</p>
                            <p className="text-xs text-muted-foreground">
                              {formatFileSize(doc.fileSize)} • {doc.chunkCount} chunks • Uploaded {formatDate(doc.createdAt)}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setExpandedDoc(expandedDoc === doc.id.toString() ? null : doc.id.toString())}
                            className="text-xs"
                          >
                            {expandedDoc === doc.id.toString() ? "Hide" : "Show"} Details
                          </Button>
                          <Button variant="outline" size="sm" className="text-xs bg-transparent">
                            View
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            className="text-xs text-destructive hover:bg-destructive/10 bg-transparent"
                            onClick={() => deleteDocument(doc.id)}
                          >
                            Delete
                          </Button>
                        </div>
                      </div>

                      {expandedDoc === doc.id.toString() && (
                        <div className="border-t border-border pt-4 mt-4 space-y-3">
                          <div>
                            <p className="text-xs font-semibold text-muted-foreground mb-2">Document Type</p>
                            <div
                              className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${categoryInfo.color}`}
                            >
                              {categoryInfo.icon} {categoryInfo.label}
                            </div>
                          </div>

                          <div>
                            <p className="text-xs font-semibold text-muted-foreground mb-1">Status</p>
                            <p className="text-sm text-foreground">{doc.status}</p>
                          </div>

                          <div>
                            <p className="text-xs font-semibold text-muted-foreground mb-1">Title</p>
                            <p className="text-sm text-foreground">{doc.title || doc.filename}</p>
                          </div>

                          <div>
                            <p className="text-xs font-semibold text-muted-foreground mb-1">Last Updated</p>
                            <p className="text-sm text-foreground">{formatDate(doc.updatedAt)}</p>
                          </div>
                        </div>
                      )}
                    </div>
                  </Card>
                )
              })}
            </div>
          )}
        </div>
    </PageContainer>
  )
}
