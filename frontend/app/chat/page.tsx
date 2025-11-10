"use client"

import type React from "react"
import { useState, useRef, useEffect } from "react"
import Link from "next/link"
import { Button, Card, CardContent, Box, Paper, Typography, TextField } from "@mui/material"
import { apiClient, generateSessionId } from "@/lib/api"

interface Message {
  id: string
  role: "user" | "assistant"
  content: string
  timestamp: Date
}

export default function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "0",
      role: "assistant",
      content:
        "Hello! I'm your ZEOS Knowledge assistant. Ask me any questions about your knowledge base, and I'll search through your documents to find accurate answers.",
      timestamp: new Date(),
    },
  ])
  const [input, setInput] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const [sessionId] = useState(() => generateSessionId())
  const [config, setConfig] = useState<any>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Load configuration from localStorage
  useEffect(() => {
    const savedConfig = localStorage.getItem('ragConfig')
    if (savedConfig) {
      setConfig(JSON.parse(savedConfig))
    }
  }, [])

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSendMessage = async () => {
    if (!input.trim()) return

    const userMessage: Message = {
      id: Date.now().toString(),
      role: "user",
      content: input,
      timestamp: new Date(),
    }

    setMessages((prev) => [...prev, userMessage])
    setInput("")
    setIsLoading(true)

    try {
      // Real API call to RAG backend with user configuration
      const requestParams: any = {
        question: input,
        sessionId: sessionId,
      }

      // Apply user configuration if available
      if (config) {
        requestParams.maxResults = config.maxResults || 5
        requestParams.similarityThreshold = config.similarityThreshold || 0.7
        requestParams.includeCitations = config.includeCitations !== undefined ? config.includeCitations : true
        requestParams.temperature = config.temperature || 0.7
        requestParams.topK = config.topK || 10
      }

      const response = await apiClient.sendMessage(requestParams);

      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: response.answer,
        timestamp: new Date(),
      }

      setMessages((prev) => [...prev, assistantMessage])
    } catch (error) {
      console.error('Failed to send message:', error);

      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: "I'm sorry, but I encountered an error while processing your question. Please try again later.",
        timestamp: new Date(),
      }

      setMessages((prev) => [...prev, errorMessage])
    } finally {
      setIsLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 128px)' }}>
      {/* Messages Area */}
      <Box sx={{ flex: 1, overflowY: 'auto', mb: 2, px: 1 }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {messages.map((message) => (
            <Box
              key={message.id}
              sx={{
                display: 'flex',
                justifyContent: message.role === "user" ? "flex-end" : "flex-start"
              }}
            >
              <Paper
                sx={{
                  maxWidth: { xs: '85%', lg: '70%' },
                  px: 2,
                  py: 1.5,
                  borderRadius: 2,
                  ...(message.role === "user" ? {
                    background: 'linear-gradient(45deg, #2563eb, #7c3aed)',
                    color: 'white',
                    borderBottomRightRadius: 4,
                  } : {
                    bgcolor: 'background.paper',
                    border: 1,
                    borderColor: 'divider',
                    borderBottomLeftRadius: 4,
                  })
                }}
              >
                <Typography
                  variant="body2"
                  sx={{
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-words',
                    lineHeight: 1.5
                  }}
                >
                  {message.content}
                </Typography>
                <Typography
                  variant="caption"
                  sx={{
                    opacity: 0.7,
                    mt: 1,
                    display: 'block',
                    color: message.role === "user" ? 'rgba(255,255,255,0.8)' : 'text.secondary'
                  }}
                >
                  {message.timestamp.toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </Typography>
              </Paper>
            </Box>
          ))}

          {isLoading && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-start' }}>
              <Paper sx={{ borderRadius: 2, borderBottomLeftRadius: 4, px: 2, py: 1.5, border: 1, borderColor: 'divider' }}>
                <Box sx={{ display: 'flex', gap: 0.5 }}>
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      bgcolor: 'primary.main',
                      borderRadius: '50%',
                      animation: 'bounce 1.4s ease-in-out infinite both',
                      '@keyframes bounce': {
                        '0%, 80%, 100%': { transform: 'scale(0)' },
                        '40%': { transform: 'scale(1)' }
                      }
                    }}
                  />
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      bgcolor: 'primary.main',
                      borderRadius: '50%',
                      animation: 'bounce 1.4s ease-in-out infinite both',
                      animationDelay: '0.1s',
                      '@keyframes bounce': {
                        '0%, 80%, 100%': { transform: 'scale(0)' },
                        '40%': { transform: 'scale(1)' }
                      }
                    }}
                  />
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      bgcolor: 'primary.main',
                      borderRadius: '50%',
                      animation: 'bounce 1.4s ease-in-out infinite both',
                      animationDelay: '0.2s',
                      '@keyframes bounce': {
                        '0%, 80%, 100%': { transform: 'scale(0)' },
                        '40%': { transform: 'scale(1)' }
                      }
                    }}
                  />
                </Box>
              </Paper>
            </Box>
          )}

          <div ref={messagesEndRef} />
        </Box>
      </Box>

      {/* Input Area */}
      <Card sx={{ bgcolor: 'background.paper', backdropFilter: 'blur(8px)' }}>
        <CardContent sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-end' }}>
            <TextField
              multiline
              rows={3}
              fullWidth
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask a question about your documents... (Shift+Enter for new line)"
              disabled={isLoading}
              variant="outlined"
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 2,
                }
              }}
            />
            <Button
              onClick={handleSendMessage}
              disabled={isLoading || !input.trim()}
              variant="contained"
              sx={{
                background: 'linear-gradient(45deg, #2563eb, #7c3aed)',
                minWidth: 80,
                height: 'fit-content',
                '&:hover': {
                  background: 'linear-gradient(45deg, #1d4ed8, #6d28d9)',
                  boxShadow: '0 8px 25px rgba(37, 99, 235, 0.3)',
                },
              }}
            >
              Send
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* Empty State Suggestions */}
      {messages.length === 1 && (
        <Box sx={{ mt: 3, display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>
          <Card
            sx={{
              p: 2,
              cursor: 'pointer',
              transition: 'all 0.2s',
              bgcolor: 'background.paper',
              border: 1,
              borderColor: 'divider',
              '&:hover': {
                borderColor: 'primary.main',
                boxShadow: 1,
              },
            }}
            onClick={() => setInput("What are the main topics covered in my documents?")}
          >
            <Typography variant="body2" color="primary.main" sx={{ fontWeight: 600, mb: 0.5 }}>
              📚 Explore
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Main topics in documents
            </Typography>
          </Card>
          <Card
            sx={{
              p: 2,
              cursor: 'pointer',
              transition: 'all 0.2s',
              bgcolor: 'background.paper',
              border: 1,
              borderColor: 'divider',
              '&:hover': {
                borderColor: 'primary.main',
                boxShadow: 1,
              },
            }}
            onClick={() => setInput("Summarize the key information")}
          >
            <Typography variant="body2" color="primary.main" sx={{ fontWeight: 600, mb: 0.5 }}>
              ✨ Summarize
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Key information summary
            </Typography>
          </Card>
          <Card
            sx={{
              p: 2,
              cursor: 'pointer',
              transition: 'all 0.2s',
              bgcolor: 'background.paper',
              border: 1,
              borderColor: 'divider',
              '&:hover': {
                borderColor: 'primary.main',
                boxShadow: 1,
              },
            }}
            onClick={() => setInput("What insights can you provide?")}
          >
            <Typography variant="body2" color="primary.main" sx={{ fontWeight: 600, mb: 0.5 }}>
              💡 Insights
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Generate insights
            </Typography>
          </Card>
        </Box>
      )}
    </Box>
  )
}
