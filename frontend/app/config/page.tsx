"use client"

import { useState, useEffect } from "react"
import {
  Button,
  Card,
  CardContent,
  Typography,
  Box,
  Container,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Slider,
  Checkbox,
  FormControlLabel,
  Alert,
  CircularProgress
} from "@mui/material"
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
    <Container maxWidth="xl">
      {/* Page Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Configuration
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Fine-tune your RAG system parameters
        </Typography>
      </Box>

      {/* Notifications */}
      {saved && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Settings saved successfully!
        </Alert>
      )}

      {isLoading && (
        <Alert severity="info" sx={{ mb: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <CircularProgress size={20} />
            Loading configuration...
          </Box>
        </Alert>
      )}

      {/* Embeddings Configuration */}
      <Card sx={{ bgcolor: 'background.paper', borderRadius: 2, mb: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Typography variant="h5" sx={{ fontWeight: 600, mb: 3 }}>
            Embeddings Configuration
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <FormControl fullWidth>
              <InputLabel>Embeddings Model</InputLabel>
              <Select
                value={config.embeddingsModel}
                label="Embeddings Model"
                onChange={(e) => handleChange("embeddingsModel", e.target.value)}
              >
                <MenuItem value="text-embedding-3-small">text-embedding-3-small</MenuItem>
                <MenuItem value="text-embedding-3-large">text-embedding-3-large</MenuItem>
                <MenuItem value="text-embedding-ada-002">text-embedding-ada-002</MenuItem>
              </Select>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
                Model used for creating document embeddings
              </Typography>
            </FormControl>

            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
              <Box>
                <Typography variant="body2" sx={{ fontWeight: 500, mb: 2 }}>
                  Chunk Size: {config.chunkSize}
                </Typography>
                <Slider
                  value={config.chunkSize}
                  onChange={(_, value) => handleChange("chunkSize", value as number)}
                  min={256}
                  max={2048}
                  step={256}
                  marks
                  valueLabelDisplay="auto"
                />
                <Typography variant="caption" color="text.secondary">
                  Size of document chunks in tokens
                </Typography>
              </Box>

              <Box>
                <Typography variant="body2" sx={{ fontWeight: 500, mb: 2 }}>
                  Overlap: {config.overlapPercentage}%
                </Typography>
                <Slider
                  value={config.overlapPercentage}
                  onChange={(_, value) => handleChange("overlapPercentage", value as number)}
                  min={0}
                  max={50}
                  step={5}
                  marks
                  valueLabelDisplay="auto"
                />
                <Typography variant="caption" color="text.secondary">
                  Overlap between consecutive chunks
                </Typography>
              </Box>
            </Box>
          </Box>
        </CardContent>
      </Card>

      <Card sx={{ bgcolor: 'background.paper', borderRadius: 2, mb: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Typography variant="h5" sx={{ fontWeight: 600, mb: 3 }}>
            Chunking Strategy
          </Typography>

          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2 }}>
            {Object.entries(CHUNKING_STRATEGIES).map(([key, strategy]) => (
              <Card
                key={key}
                onClick={() => handleChange("chunkingStrategy", key)}
                sx={{
                  border: 2,
                  borderColor: config.chunkingStrategy === key ? 'primary.main' : 'divider',
                  bgcolor: config.chunkingStrategy === key ? 'primary.light' : 'background.paper',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  '&:hover': {
                    borderColor: 'primary.main',
                    bgcolor: config.chunkingStrategy === key ? 'primary.light' : 'action.hover',
                  },
                }}
              >
                <CardContent sx={{ p: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
                    <Typography sx={{ fontSize: '1.5rem' }}>{strategy.icon}</Typography>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" sx={{ fontWeight: 600, mb: 0.5 }}>
                        {strategy.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {strategy.description}
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            ))}
          </Box>
        </CardContent>
      </Card>

      {/* Retrieval Configuration */}
      <Card sx={{ bgcolor: 'background.paper', borderRadius: 2, mb: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Typography variant="h5" sx={{ fontWeight: 600, mb: 3 }}>
            Retrieval Configuration
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
              <Box>
                <Typography variant="body2" sx={{ fontWeight: 500, mb: 2 }}>
                  Similarity Threshold: {config.similarityThreshold.toFixed(2)}
                </Typography>
                <Slider
                  value={config.similarityThreshold}
                  onChange={(_, value) => handleChange("similarityThreshold", value as number)}
                  min={0}
                  max={1}
                  step={0.05}
                  marks
                  valueLabelDisplay="auto"
                />
                <Typography variant="caption" color="text.secondary">
                  Minimum similarity score for retrieval
                </Typography>
              </Box>

              <Box>
                <Typography variant="body2" sx={{ fontWeight: 500, mb: 2 }}>
                  Max Results: {config.maxResults}
                </Typography>
                <Slider
                  value={config.maxResults}
                  onChange={(_, value) => handleChange("maxResults", value as number)}
                  min={1}
                  max={20}
                  step={1}
                  marks
                  valueLabelDisplay="auto"
                />
                <Typography variant="caption" color="text.secondary">
                  Maximum number of results to retrieve
                </Typography>
              </Box>
            </Box>

            <Box>
              <Typography variant="body2" sx={{ fontWeight: 500, mb: 2 }}>
                Top K for Re-ranking: {config.topK}
              </Typography>
              <Slider
                value={config.topK}
                onChange={(_, value) => handleChange("topK", value as number)}
                min={5}
                max={50}
                step={5}
                marks
                valueLabelDisplay="auto"
              />
              <Typography variant="caption" color="text.secondary">
                Number of results to re-rank
              </Typography>
            </Box>

            <Box sx={{ borderTop: 1, borderColor: 'divider', pt: 2 }}>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={config.includeCitations}
                    onChange={(e) => handleChange("includeCitations", e.target.checked)}
                    color="primary"
                  />
                }
                label="Include Source Citations"
              />
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                Enable to show document source and chunk references in RAG responses
              </Typography>
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* Generation Configuration */}
      <Card sx={{ bgcolor: 'background.paper', borderRadius: 2, mb: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Typography variant="h5" sx={{ fontWeight: 600, mb: 3 }}>
            Generation Configuration
          </Typography>

          <Box>
            <Typography variant="body2" sx={{ fontWeight: 500, mb: 2 }}>
              Temperature: {config.temperature.toFixed(2)}
            </Typography>
            <Slider
              value={config.temperature}
              onChange={(_, value) => handleChange("temperature", value as number)}
              min={0}
              max={2}
              step={0.1}
              marks
              valueLabelDisplay="auto"
            />
            <Typography variant="caption" color="text.secondary">
              Controls randomness (0 = deterministic, 2 = very random)
            </Typography>
          </Box>
        </CardContent>
      </Card>

      {/* Advanced Settings Info */}
      <Card sx={{ bgcolor: 'secondary.light', borderRadius: 2, mb: 4 }}>
        <CardContent sx={{ p: 3 }}>
          <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
            Parameter Guide
          </Typography>
          <Box component="ul" sx={{ pl: 0, m: 0, listStyle: 'none' }}>
            <Typography component="li" variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              • <strong>Embeddings Model:</strong> Choose between different embedding dimensions and performance
            </Typography>
            <Typography component="li" variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              • <strong>Chunking Strategy:</strong> Select the best strategy for your document types
            </Typography>
            <Typography component="li" variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              • <strong>Chunk Size:</strong> Larger chunks preserve context, smaller chunks increase retrieval precision
            </Typography>
            <Typography component="li" variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              • <strong>Similarity Threshold:</strong> Higher values retrieve only more relevant documents
            </Typography>
            <Typography component="li" variant="body2" color="text.secondary">
              • <strong>Include Citations:</strong> Enhance transparency by showing source references
            </Typography>
          </Box>
        </CardContent>
      </Card>

      {/* Action Buttons */}
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
        <Button variant="outlined" onClick={handleReset}>
          Reset to Defaults
        </Button>
        <Button
          onClick={handleSave}
          disabled={isSaving}
          variant="contained"
          sx={{
            background: 'linear-gradient(45deg, #2563eb, #7c3aed)',
            '&:hover': {
              background: 'linear-gradient(45deg, #1d4ed8, #6d28d9)',
              boxShadow: '0 8px 25px rgba(37, 99, 235, 0.3)',
            },
          }}
        >
          {isSaving ? "Saving..." : "Save Configuration"}
        </Button>
      </Box>
    </Container>
  )
}
