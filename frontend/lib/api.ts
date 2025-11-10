// API client for ZEOS Knowledge backend integration
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// Types matching backend DTOs
export interface ChatRequest {
  question: string;
  maxResults?: number;
  similarityThreshold?: number;
  includeSourceInfo?: boolean;
  selectedModel?: string;
  temperature?: number;
  topK?: number;
  sessionId?: string;
  includeCitations?: boolean;
}

export interface ChatResponse {
  answer: string;
  question: string;
  sources: SourceDocument[];
  responseTimeMs: number;
}

export interface SourceDocument {
  filename: string;
  title: string;
  content: string;
  similarity: number;
  chunkIndex: number;
}

export interface DocumentUploadResponse {
  documentId: number;
  filename: string;
  message: string;
  success: boolean;
  status: string;
}

export interface RagConfiguration {
  id?: number;
  configKey: string;
  embeddingsModel: string;
  chunkingStrategy: string;
  chunkSize: number;
  overlapPercentage: number;
  similarityThreshold: number;
  maxResults: number;
  includeCitations: boolean;
  temperature: number;
  topK: number;
  selectedModel: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface DocumentDto {
  id: number;
  filename: string;
  title: string;
  fileSize: number;
  chunkCount: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface AnalyticsDashboard {
  totalStorage: number | null;
  totalDocuments: number;
  totalChunks: number | null;
  last24Hours: {
    queries: number;
    avgResponseTime: number | null;
    avgAccuracy: number | null;
  };
  last7Days: {
    queries: number;
    avgResponseTime: number | null;
    avgAccuracy: number | null;
  };
}

class ApiClient {
  private baseURL: string;

  constructor(baseURL: string = API_BASE_URL) {
    this.baseURL = baseURL;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;

    const config: RequestInit = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    };

    try {
      const response = await fetch(url, config);

      if (!response.ok) {
        throw new Error(`API Error: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`API request failed: ${url}`, error);
      throw error;
    }
  }

  // Chat API
  async sendMessage(request: ChatRequest): Promise<ChatResponse> {
    return this.request<ChatResponse>('/chat/ask', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // Document Management API
  async uploadDocument(file: File, chunkingStrategy?: string, embeddingModel?: string): Promise<DocumentUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    if (chunkingStrategy) {
      formData.append('chunkingStrategy', chunkingStrategy);
    }
    if (embeddingModel) {
      formData.append('embeddingModel', embeddingModel);
    }

    return this.request<DocumentUploadResponse>('/documents/upload', {
      method: 'POST',
      body: formData,
      headers: {}, // Remove Content-Type to let browser set boundary for FormData
    });
  }

  async getDocuments(): Promise<DocumentDto[]> {
    return this.request<DocumentDto[]>('/documents');
  }

  async deleteDocument(id: number): Promise<void> {
    return this.request<void>(`/documents/${id}`, {
      method: 'DELETE',
    });
  }

  // Configuration API
  async getConfiguration(): Promise<RagConfiguration> {
    return this.request<RagConfiguration>('/config');
  }

  async updateConfiguration(config: Partial<RagConfiguration>): Promise<RagConfiguration> {
    return this.request<RagConfiguration>('/config/default', {
      method: 'PUT',
      body: JSON.stringify(config),
    });
  }

  // Analytics API
  async getAnalyticsDashboard(): Promise<AnalyticsDashboard> {
    return this.request<AnalyticsDashboard>('/analytics/dashboard');
  }

  async getQueryAnalytics(startTime?: string, endTime?: string): Promise<any> {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);

    const query = params.toString() ? `?${params.toString()}` : '';
    return this.request<any>(`/analytics/queries${query}`);
  }

  async getDocumentAnalytics(startTime?: string, endTime?: string): Promise<any> {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);

    const query = params.toString() ? `?${params.toString()}` : '';
    return this.request<any>(`/analytics/documents${query}`);
  }

  async getSystemAnalytics(startTime?: string, endTime?: string): Promise<any> {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);

    const query = params.toString() ? `?${params.toString()}` : '';
    return this.request<any>(`/analytics/system${query}`);
  }

  async getPerformanceMetrics(hours: number = 24): Promise<any> {
    return this.request<any>(`/analytics/performance?hours=${hours}`);
  }

  async getUsageAnalytics(days: number = 7): Promise<any> {
    return this.request<any>(`/analytics/usage?days=${days}`);
  }
}

// Export singleton instance
export const apiClient = new ApiClient();

// Utility function to generate session ID
export function generateSessionId(): string {
  return `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}