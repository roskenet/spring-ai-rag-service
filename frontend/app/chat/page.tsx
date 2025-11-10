"use client"

import type React from "react"
import { useState, useRef, useEffect } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
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
    <div className="flex flex-col h-[calc(100vh-64px-64px)]">
        {/* Messages Area */}
        <div className="flex-1 overflow-y-auto mb-4 space-y-4 pr-2">
          {messages.map((message) => (
            <div key={message.id} className={`flex ${message.role === "user" ? "justify-end" : "justify-start"}`}>
              <div
                className={`max-w-xl lg:max-w-2xl rounded-lg px-4 py-3 ${
                  message.role === "user"
                    ? "bg-gradient-to-r from-primary to-secondary text-primary-foreground rounded-br-none"
                    : "bg-card border border-border text-foreground rounded-bl-none"
                }`}
              >
                <p className="text-sm whitespace-pre-wrap break-words">{message.content}</p>
                <span className="text-xs opacity-60 mt-2 block">
                  {message.timestamp.toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </span>
              </div>
            </div>
          ))}

          {isLoading && (
            <div className="flex justify-start">
              <div className="bg-card border border-border rounded-lg px-4 py-3 rounded-bl-none">
                <div className="flex gap-1">
                  <div className="w-2 h-2 bg-primary rounded-full animate-bounce"></div>
                  <div
                    className="w-2 h-2 bg-primary rounded-full animate-bounce"
                    style={{ animationDelay: "0.1s" }}
                  ></div>
                  <div
                    className="w-2 h-2 bg-primary rounded-full animate-bounce"
                    style={{ animationDelay: "0.2s" }}
                  ></div>
                </div>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <Card className="border border-border bg-card/50 backdrop-blur p-4">
          <div className="flex gap-2 items-end">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask a question about your documents... (Shift+Enter for new line)"
              className="flex-1 bg-background border border-border rounded-lg px-4 py-2 resize-none focus:outline-none focus:ring-2 focus:ring-primary text-foreground placeholder:text-muted-foreground"
              rows={3}
              disabled={isLoading}
            />
            <Button
              onClick={handleSendMessage}
              disabled={isLoading || !input.trim()}
              className="bg-gradient-to-r from-primary to-secondary hover:shadow-lg hover:shadow-primary/40 transition-all"
            >
              Send
            </Button>
          </div>
        </Card>

        {/* Empty State Suggestions */}
        {messages.length === 1 && (
          <div className="mt-6 grid md:grid-cols-3 gap-4 text-center">
            <Card
              className="p-4 border border-border/50 bg-card/30 hover:border-primary/50 cursor-pointer transition-colors"
              onClick={() => setInput("What are the main topics covered in my documents?")}
            >
              <p className="text-sm font-semibold text-primary mb-1">📚 Explore</p>
              <p className="text-xs text-muted-foreground">Main topics in documents</p>
            </Card>
            <Card
              className="p-4 border border-border/50 bg-card/30 hover:border-primary/50 cursor-pointer transition-colors"
              onClick={() => setInput("Summarize the key information")}
            >
              <p className="text-sm font-semibold text-primary mb-1">✨ Summarize</p>
              <p className="text-xs text-muted-foreground">Key information summary</p>
            </Card>
            <Card
              className="p-4 border border-border/50 bg-card/30 hover:border-primary/50 cursor-pointer transition-colors"
              onClick={() => setInput("What insights can you provide?")}
            >
              <p className="text-sm font-semibold text-primary mb-1">💡 Insights</p>
              <p className="text-xs text-muted-foreground">Generate insights</p>
            </Card>
          </div>
        )}
    </div>
  )
}
