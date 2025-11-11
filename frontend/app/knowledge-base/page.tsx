"use client"

import type React from "react"
import { useState, useEffect } from "react"
import {
  Button,
  Card,
  CardContent,
  Typography,
  Box,
  Container,
  Chip,
  IconButton,
  Collapse,
  Paper
} from "@mui/material"
import { ExpandMore, ExpandLess, Visibility, Delete, Upload } from "@mui/icons-material"
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
  const [selectedFiles, setSelectedFiles] = useState<File[]>([])
  const [uploadErrors, setUploadErrors] = useState<string[]>([])

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

  const validateAndProcessFiles = (files: File[]) => {
    const supportedTypes = ["text/markdown", "text/plain", "application/pdf"]
    const supportedExtensions = [".md", ".txt", ".pdf"]

    const validFiles: File[] = []
    const errors: string[] = []

    files.forEach(file => {
      const isValidType = supportedTypes.some(type => file.type.includes(type))
      const isValidExtension = supportedExtensions.some(ext => file.name.toLowerCase().endsWith(ext))

      if (isValidType || isValidExtension) {
        validFiles.push(file)
      } else {
        errors.push(`"${file.name}" is not supported. Only .md, .txt, and .pdf files are allowed.`)
      }
    })

    if (errors.length > 0) {
      setUploadErrors(errors)
    }

    if (validFiles.length > 0) {
      setSelectedFiles(prev => [...prev, ...validFiles])
    }
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    setUploadErrors([]) // Clear previous errors

    const files = Array.from(e.dataTransfer.files)
    validateAndProcessFiles(files)
  }

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    setUploadErrors([]) // Clear previous errors

    const files = Array.from(e.target.files || [])
    validateAndProcessFiles(files)

    // Clear the file input to allow re-selecting the same file
    e.target.value = '';
  }

  const uploadSelectedFiles = async () => {
    if (selectedFiles.length === 0) return;

    try {
      setIsUploading(true);
      setUploadErrors([]); // Clear any previous errors
      let successCount = 0;
      let errorCount = 0;
      const uploadErrors: string[] = [];

      for (const file of selectedFiles) {
        try {
          const response = await apiClient.uploadDocument(file);

          if (response.success) {
            successCount++;
          } else {
            errorCount++;
            uploadErrors.push(`Failed to upload "${file.name}": ${response.message}`);
          }
        } catch (error) {
          errorCount++;
          uploadErrors.push(`Failed to upload "${file.name}": ${error instanceof Error ? error.message : 'Unknown error'}`);
        }
      }

      // Show upload errors if any
      if (uploadErrors.length > 0) {
        setUploadErrors(uploadErrors);
      }

      // Clear selected files and reload documents
      setSelectedFiles([]);
      await loadDocuments();

    } catch (error) {
      setUploadErrors([`Upload process failed: ${error instanceof Error ? error.message : 'Unknown error'}`]);
    } finally {
      setIsUploading(false);
    }
  };

  const removeSelectedFile = (index: number) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
  };

  const clearSelectedFiles = () => {
    setSelectedFiles([]);
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
    <Container maxWidth="xl">
      {/* Page Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Knowledge Base
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manage and organize your documents for RAG search
        </Typography>
      </Box>

      {/* Stats */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2, mb: 4 }}>
        <Card sx={{ bgcolor: 'background.paper', borderRadius: 2 }}>
          <CardContent sx={{ p: 3 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Total Documents
            </Typography>
            <Typography variant="h4" color="primary.main" sx={{ fontWeight: 'bold' }}>
              {documents.length}
            </Typography>
          </CardContent>
        </Card>
        <Card sx={{ bgcolor: 'background.paper', borderRadius: 2 }}>
          <CardContent sx={{ p: 3 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Total Chunks
            </Typography>
            <Typography variant="h4" color="secondary.main" sx={{ fontWeight: 'bold' }}>
              {totalChunks}
            </Typography>
          </CardContent>
        </Card>
        <Card sx={{ bgcolor: 'background.paper', borderRadius: 2 }}>
          <CardContent sx={{ p: 3 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Storage Used
            </Typography>
            <Typography variant="h4" color="success.main" sx={{ fontWeight: 'bold' }}>
              {formatFileSize(totalSize)}
            </Typography>
          </CardContent>
        </Card>
      </Box>

      <Card sx={{ bgcolor: 'background.paper', borderRadius: 2, mb: 4 }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h6" sx={{ fontWeight: 600, mb: 3, textAlign: 'center' }}>
            Upload Documents
          </Typography>

          {/* Error Messages */}
          {uploadErrors.length > 0 && (
            <Box sx={{ mb: 3 }}>
              {uploadErrors.map((error, index) => (
                <Box
                  key={index}
                  sx={{
                    p: 2,
                    mb: 1,
                    bgcolor: 'error.light',
                    color: 'error.contrastText',
                    borderRadius: 1,
                    border: 1,
                    borderColor: 'error.main'
                  }}
                >
                  <Typography variant="body2">{error}</Typography>
                </Box>
              ))}
            </Box>
          )}

          {/* Drag and Drop Area with Hidden File Input */}
          <Paper
            component="label"
            htmlFor="file-upload"
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            sx={{
              border: 2,
              borderStyle: 'dashed',
              borderColor: isDragging ? 'primary.main' : uploadErrors.length > 0 ? 'error.main' : 'divider',
              bgcolor: isDragging ? 'primary.light' : 'transparent',
              borderRadius: 2,
              p: 4,
              textAlign: 'center',
              transition: 'all 0.2s',
              cursor: 'pointer',
              display: 'block',
              '&:hover': {
                borderColor: 'primary.main',
                bgcolor: 'action.hover',
              },
              mb: selectedFiles.length > 0 ? 3 : 0
            }}
          >
            <input
              id="file-upload"
              type="file"
              multiple
              accept=".md,.txt,.pdf"
              onChange={handleFileInput}
              style={{ display: 'none' }}
            />
            <Upload sx={{ fontSize: '3rem', color: 'primary.main', mb: 2 }} />
            <Typography variant="h6" sx={{ fontWeight: 500, mb: 1 }}>
              Drop files here or click to browse
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Supported formats: .md, .txt, .pdf
            </Typography>
            <Typography variant="caption" color="text.secondary">
              You can select multiple files at once
            </Typography>
          </Paper>

          {/* Selected Files Display */}
          {selectedFiles.length > 0 && (
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  Ready to Upload ({selectedFiles.length} file{selectedFiles.length > 1 ? 's' : ''})
                </Typography>
                <Button
                  variant="text"
                  size="small"
                  onClick={clearSelectedFiles}
                  disabled={isUploading}
                >
                  Clear All
                </Button>
              </Box>

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 3 }}>
                {selectedFiles.map((file, index) => (
                  <Box
                    key={index}
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      p: 2,
                      border: 1,
                      borderColor: 'success.main',
                      borderRadius: 1,
                      bgcolor: 'success.light'
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                      <Typography sx={{ fontSize: '1.5rem' }}>
                        {file.name.endsWith('.pdf') ? '📄' : file.name.endsWith('.md') ? '📝' : '📋'}
                      </Typography>
                      <Box>
                        <Typography variant="body2" sx={{ fontWeight: 500, color: 'success.contrastText' }}>
                          {file.name}
                        </Typography>
                        <Typography variant="caption" sx={{ color: 'success.contrastText', opacity: 0.8 }}>
                          {formatFileSize(file.size)}
                        </Typography>
                      </Box>
                    </Box>
                    <IconButton
                      size="small"
                      onClick={() => removeSelectedFile(index)}
                      disabled={isUploading}
                      sx={{ color: 'success.contrastText' }}
                    >
                      <Delete />
                    </IconButton>
                  </Box>
                ))}
              </Box>

              {/* Upload Button */}
              <Box sx={{ textAlign: 'center' }}>
                <Button
                  variant="contained"
                  onClick={uploadSelectedFiles}
                  disabled={isUploading || selectedFiles.length === 0}
                  size="large"
                  sx={{
                    background: 'linear-gradient(45deg, #10b981, #059669)',
                    '&:hover': {
                      background: 'linear-gradient(45deg, #059669, #047857)',
                      boxShadow: '0 8px 25px rgba(16, 185, 129, 0.3)',
                    },
                    px: 6,
                    py: 1.5,
                    fontSize: '1.1rem'
                  }}
                >
                  {isUploading ? (
                    <>⏳ Uploading {selectedFiles.length} file{selectedFiles.length > 1 ? 's' : ''}...</>
                  ) : (
                    <>🚀 Upload {selectedFiles.length} file{selectedFiles.length > 1 ? 's' : ''}</>
                  )}
                </Button>
              </Box>
            </Box>
          )}
        </CardContent>
      </Card>


      {/* Documents List */}
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 600, mb: 3 }}>
          Your Documents
        </Typography>
        {documents.length === 0 ? (
          <Card sx={{ bgcolor: 'background.paper', borderRadius: 2 }}>
            <CardContent sx={{ p: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.secondary">
                No documents yet. Upload your first document to get started!
              </Typography>
            </CardContent>
          </Card>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {documents.map((doc) => {
              // Since backend doesn't return metadata, we'll provide defaults
              const categoryInfo = getCategoryInfo("other")
              const fileType = doc.filename.endsWith(".pdf") ? "pdf" :
                              doc.filename.endsWith(".md") ? "markdown" : "txt"
              const isExpanded = expandedDoc === doc.id.toString()

              return (
                <Card
                  key={doc.id}
                  sx={{
                    bgcolor: 'background.paper',
                    borderRadius: 2,
                    transition: 'all 0.2s',
                    '&:hover': {
                      borderColor: 'primary.main',
                      boxShadow: 2,
                    },
                  }}
                >
                  <CardContent sx={{ p: 3 }}>
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 2 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flex: 1 }}>
                        <Typography sx={{ fontSize: '1.5rem' }}>{getTypeIcon(fileType)}</Typography>
                        <Box sx={{ flex: 1 }}>
                          <Typography variant="h6" sx={{ fontWeight: 600 }}>
                            {doc.filename}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {formatFileSize(doc.fileSize)} • {doc.chunkCount} chunks • Uploaded {formatDate(doc.createdAt)}
                          </Typography>
                        </Box>
                      </Box>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Button
                          size="small"
                          onClick={() => setExpandedDoc(isExpanded ? null : doc.id.toString())}
                          endIcon={isExpanded ? <ExpandLess /> : <ExpandMore />}
                        >
                          {isExpanded ? "Hide" : "Show"} Details
                        </Button>
                        <IconButton size="small" color="primary">
                          <Visibility />
                        </IconButton>
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => deleteDocument(doc.id)}
                        >
                          <Delete />
                        </IconButton>
                      </Box>
                    </Box>

                    <Collapse in={isExpanded}>
                      <Box sx={{ borderTop: 1, borderColor: 'divider', pt: 2, mt: 2 }}>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                          <Box>
                            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, mb: 1, display: 'block' }}>
                              Document Type
                            </Typography>
                            <Chip
                              label={`${categoryInfo.icon} ${categoryInfo.label}`}
                              size="small"
                              variant="outlined"
                            />
                          </Box>

                          <Box>
                            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, mb: 0.5, display: 'block' }}>
                              Status
                            </Typography>
                            <Typography variant="body2">{doc.status}</Typography>
                          </Box>

                          <Box>
                            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, mb: 0.5, display: 'block' }}>
                              Title
                            </Typography>
                            <Typography variant="body2">{doc.title || doc.filename}</Typography>
                          </Box>

                          <Box>
                            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, mb: 0.5, display: 'block' }}>
                              Last Updated
                            </Typography>
                            <Typography variant="body2">{formatDate(doc.updatedAt)}</Typography>
                          </Box>
                        </Box>
                      </Box>
                    </Collapse>
                  </CardContent>
                </Card>
              )
            })}
          </Box>
        )}
      </Box>
    </Container>
  )
}
