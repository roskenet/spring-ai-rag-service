"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { PageHeader } from "@/components/page-header"
import { PageContainer } from "@/components/page-container"
import { apiClient } from "@/lib/api"
import type { RagConfiguration } from "@/lib/api"

interface Config {
  embeddingsModel: string
  chunkingStrategy: string
  chunkSize: number
  overlapPercentage: number
  similarityThreshold: number
  maxResults: number
  includeCitations: boolean
  temperature: number
  topK: number
}

const CHUNKING_STRATEGIES = {
  intelligent: {
    name: "Intelligent Chunking",
    description:
      "Context-aware segmentation that preserves document structure, headers, and code blocks. Best for technical documentation.",
    icon: "🧠",
  },
  fixed: {
    name: "Fixed Size Chunking",
    description: "Consistent chunk sizes for predictable performance. Good for simple text documents.",
    icon: "📏",
  },
  recursive: {
    name: "Recursive Chunking",
    description:
      "Hierarchical splitting based on document structure. Ideal for structured documents with clear sections.",
    icon: "🌳",
  },
  codeAware: {
    name: "Code-Aware Chunking",
    description: "Specialized for technical documentation with code examples. Preserves syntax and API documentation.",
    icon: "💻",
  },
}

export default function ConfigPage() {
  const [config, setConfig] = useState<Config>({
    embeddingsModel: "text-embedding-3-small",
    chunkingStrategy: "intelligent",
    chunkSize: 1000,
    overlapPercentage: 20,
    similarityThreshold: 0.7,
    maxResults: 5,
    includeCitations: true,
    temperature: 0.7,
    topK: 10,
  })

  const [saved, setSaved] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)

  // Load configuration from backend
  useEffect(() => {
    loadConfiguration();
  }, []);

  const loadConfiguration = async () => {
    try {
      setIsLoading(true);
      const backendConfig = await apiClient.getConfiguration();

      // Map backend config to frontend config format
      setConfig({
        embeddingsModel: backendConfig.embeddingsModel || "text-embedding-3-small",
        chunkingStrategy: backendConfig.chunkingStrategy || "intelligent",
        chunkSize: backendConfig.chunkSize || 1000,
        overlapPercentage: backendConfig.overlapPercentage || 20,
        similarityThreshold: backendConfig.similarityThreshold || 0.7,
        maxResults: backendConfig.maxResults || 5,
        includeCitations: backendConfig.includeCitations !== undefined ? backendConfig.includeCitations : true,
        temperature: backendConfig.temperature || 0.7,
        topK: backendConfig.topK || 10,
      });
    } catch (error) {
      console.error('Failed to load configuration:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (key: keyof Config, value: string | number | boolean) => {
    setConfig((prev) => ({
      ...prev,
      [key]:
        typeof value === "string" && !["embeddingsModel", "chunkingStrategy"].includes(key)
          ? Number.parseFloat(value)
          : value,
    }))
    setSaved(false)
  }

  const handleSave = async () => {
    try {
      setIsSaving(true);

      // Map frontend config to backend format
      const backendConfig: Partial<RagConfiguration> = {
        embeddingsModel: config.embeddingsModel,
        chunkingStrategy: config.chunkingStrategy,
        chunkSize: config.chunkSize,
        overlapPercentage: config.overlapPercentage,
        similarityThreshold: config.similarityThreshold,
        maxResults: config.maxResults,
        includeCitations: config.includeCitations,
        temperature: config.temperature,
        topK: config.topK,
        selectedModel: "gpt-4o-mini", // Default model
        isActive: true,
      };

      // Save to backend
      await apiClient.updateConfiguration(backendConfig);

      // Also save to localStorage for immediate use in chat
      localStorage.setItem("ragConfig", JSON.stringify(config));

      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (error) {
      console.error('Failed to save configuration:', error);
      // Still save to localStorage as fallback
      localStorage.setItem("ragConfig", JSON.stringify(config));
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } finally {
      setIsSaving(false);
    }
  }

  const handleReset = () => {
    setConfig({
      embeddingsModel: "text-embedding-3-small",
      chunkingStrategy: "intelligent",
      chunkSize: 1000,
      overlapPercentage: 20,
      similarityThreshold: 0.7,
      maxResults: 5,
      includeCitations: true,
      temperature: 0.7,
      topK: 10,
    })
    setSaved(false)
  }

  return (
    <PageContainer>
        <PageHeader
          title="Configuration"
          description="Fine-tune your RAG system parameters"
        />

        {/* Notifications */}
        {saved && (
          <div className="p-4 bg-primary/10 border border-primary/30 rounded-lg">
            <p className="text-sm text-primary font-semibold">Settings saved successfully!</p>
          </div>
        )}

        {isLoading && (
          <div className="p-4 bg-secondary/10 border border-secondary/30 rounded-lg">
            <p className="text-sm text-secondary font-semibold">Loading configuration...</p>
          </div>
        )}

        {/* Embeddings Configuration */}
        <Card className="border border-border bg-card/50 backdrop-blur p-6">
          <h2 className="text-xl font-semibold mb-4 text-foreground">Embeddings Configuration</h2>

          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-foreground block mb-2">Embeddings Model</label>
              <select
                value={config.embeddingsModel}
                onChange={(e) => handleChange("embeddingsModel", e.target.value)}
                className="w-full bg-background border border-border rounded-lg px-4 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="text-embedding-3-small">text-embedding-3-small</option>
                <option value="text-embedding-3-large">text-embedding-3-large</option>
                <option value="text-embedding-ada-002">text-embedding-ada-002</option>
              </select>
              <p className="text-xs text-muted-foreground mt-1">Model used for creating document embeddings</p>
            </div>

            <div className="grid md:grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-foreground block mb-2">Chunk Size: {config.chunkSize}</label>
                <input
                  type="range"
                  min="256"
                  max="2048"
                  step="256"
                  value={config.chunkSize}
                  onChange={(e) => handleChange("chunkSize", Number.parseInt(e.target.value))}
                  className="w-full"
                />
                <p className="text-xs text-muted-foreground mt-1">Size of document chunks in tokens</p>
              </div>

              <div>
                <label className="text-sm font-medium text-foreground block mb-2">
                  Overlap: {config.overlapPercentage}%
                </label>
                <input
                  type="range"
                  min="0"
                  max="50"
                  step="5"
                  value={config.overlapPercentage}
                  onChange={(e) => handleChange("overlapPercentage", Number.parseInt(e.target.value))}
                  className="w-full"
                />
                <p className="text-xs text-muted-foreground mt-1">Overlap between consecutive chunks</p>
              </div>
            </div>
          </div>
        </Card>

        <Card className="border border-border bg-card/50 backdrop-blur p-6">
          <h2 className="text-xl font-semibold mb-4 text-foreground">Chunking Strategy</h2>

          <div className="grid md:grid-cols-2 gap-4">
            {Object.entries(CHUNKING_STRATEGIES).map(([key, strategy]) => (
              <div
                key={key}
                onClick={() => handleChange("chunkingStrategy", key)}
                className={`border-2 rounded-lg p-4 cursor-pointer transition-all ${
                  config.chunkingStrategy === key
                    ? "border-primary bg-primary/10"
                    : "border-border bg-card/50 hover:border-primary/50"
                }`}
              >
                <div className="flex items-start gap-3">
                  <span className="text-3xl">{strategy.icon}</span>
                  <div className="flex-1">
                    <p className="font-semibold text-foreground text-sm">{strategy.name}</p>
                    <p className="text-xs text-muted-foreground mt-1">{strategy.description}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>

        {/* Retrieval Configuration */}
        <Card className="border border-border bg-card/50 backdrop-blur p-6">
          <h2 className="text-xl font-semibold mb-4 text-foreground">Retrieval Configuration</h2>

          <div className="space-y-4">
            <div className="grid md:grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-foreground block mb-2">
                  Similarity Threshold: {config.similarityThreshold.toFixed(2)}
                </label>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  value={config.similarityThreshold}
                  onChange={(e) => handleChange("similarityThreshold", Number.parseFloat(e.target.value))}
                  className="w-full"
                />
                <p className="text-xs text-muted-foreground mt-1">Minimum similarity score for retrieval</p>
              </div>

              <div>
                <label className="text-sm font-medium text-foreground block mb-2">
                  Max Results: {config.maxResults}
                </label>
                <input
                  type="range"
                  min="1"
                  max="20"
                  step="1"
                  value={config.maxResults}
                  onChange={(e) => handleChange("maxResults", Number.parseInt(e.target.value))}
                  className="w-full"
                />
                <p className="text-xs text-muted-foreground mt-1">Maximum number of results to retrieve</p>
              </div>
            </div>

            <div>
              <label className="text-sm font-medium text-foreground block mb-2">
                Top K for Re-ranking: {config.topK}
              </label>
              <input
                type="range"
                min="5"
                max="50"
                step="5"
                value={config.topK}
                onChange={(e) => handleChange("topK", Number.parseInt(e.target.value))}
                className="w-full"
              />
              <p className="text-xs text-muted-foreground mt-1">Number of results to re-rank</p>
            </div>

            <div className="border-t border-border pt-4">
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.includeCitations}
                  onChange={(e) => handleChange("includeCitations", e.target.checked)}
                  className="w-4 h-4 accent-primary"
                />
                <span className="text-sm font-medium text-foreground">Include Source Citations</span>
              </label>
              <p className="text-xs text-muted-foreground mt-2">
                Enable to show document source and chunk references in RAG responses
              </p>
            </div>
          </div>
        </Card>

        {/* Generation Configuration */}
        <Card className="border border-border bg-card/50 backdrop-blur p-6">
          <h2 className="text-xl font-semibold mb-4 text-foreground">Generation Configuration</h2>

          <div>
            <label className="text-sm font-medium text-foreground block mb-2">
              Temperature: {config.temperature.toFixed(2)}
            </label>
            <input
              type="range"
              min="0"
              max="2"
              step="0.1"
              value={config.temperature}
              onChange={(e) => handleChange("temperature", Number.parseFloat(e.target.value))}
              className="w-full"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Controls randomness (0 = deterministic, 2 = very random)
            </p>
          </div>
        </Card>

        {/* Advanced Settings Info */}
        <Card className="border border-border/50 bg-secondary/10 p-6 mb-8">
          <h3 className="font-semibold text-foreground mb-2">Parameter Guide</h3>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li>
              • <strong>Embeddings Model:</strong> Choose between different embedding dimensions and performance
            </li>
            <li>
              • <strong>Chunking Strategy:</strong> Select the best strategy for your document types
            </li>
            <li>
              • <strong>Chunk Size:</strong> Larger chunks preserve context, smaller chunks increase retrieval precision
            </li>
            <li>
              • <strong>Similarity Threshold:</strong> Higher values retrieve only more relevant documents
            </li>
            <li>
              • <strong>Include Citations:</strong> Enhance transparency by showing source references
            </li>
          </ul>
        </Card>

        {/* Action Buttons */}
        <div className="flex gap-4 justify-end">
          <Button variant="outline" onClick={handleReset} className="border-border hover:bg-background bg-transparent">
            Reset to Defaults
          </Button>
          <Button
            onClick={handleSave}
            disabled={isSaving}
            className="bg-gradient-to-r from-primary to-secondary hover:shadow-lg hover:shadow-primary/40 transition-all"
          >
            {isSaving ? "Saving..." : "Save Configuration"}
          </Button>
        </div>
    </PageContainer>
  )
}
