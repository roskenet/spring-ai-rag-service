# ZEOS RAG Frontend

Modern Next.js frontend for the ZEOS RAG (Retrieval-Augmented Generation) platform.

## Architecture

- **Framework**: Next.js 14 with App Router
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **UI Components**: Radix UI primitives
- **Charts**: Recharts for analytics visualization
- **State Management**: React Context + localStorage

## Features

### Core Functionality
- **Interactive Chat**: Real-time chat interface with RAG responses
- **Document Management**: Upload, view, and manage knowledge base documents
- **Configuration**: Dynamic RAG parameter tuning (similarity threshold, max results, etc.)

### Analytics Dashboard
- **Query Performance**: Response time and accuracy metrics
- **System Health**: Real-time system performance monitoring
- **Document Analytics**: Usage patterns and processing metrics
- **Interactive Charts**: Visualizations using Recharts

### UI/UX
- **Responsive Design**: Mobile-first responsive layout
- **Dark/Light Mode**: Theme switching support
- **Modern UI**: Clean, professional interface using Radix UI
- **Real-time Updates**: Live metrics and chat updates

## Getting Started

### Prerequisites
- Node.js 18+
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The application will be available at `http://localhost:3000`

### Backend Connection

Update the API base URL in `src/lib/api.ts` if your backend is running on a different port:

```typescript
const API_BASE_URL = 'http://localhost:9090';
```

## Project Structure

```
src/
├── app/                    # Next.js 14 App Router pages
│   ├── analytics/         # Analytics dashboard
│   ├── chat/             # Chat interface
│   ├── configuration/    # RAG configuration
│   ├── knowledge-base/   # Document management
│   └── layout.tsx        # Root layout
├── components/           # Reusable UI components
│   ├── ui/              # Base UI components (Radix)
│   ├── analytics/       # Analytics-specific components
│   ├── chat/           # Chat-specific components
│   └── navigation/     # Navigation components
├── lib/                 # Utility functions
│   ├── api.ts          # API client
│   ├── types.ts        # TypeScript types
│   └── utils.ts        # Helper functions
└── hooks/              # Custom React hooks
```

## Key Components

### Chat Interface (`src/app/chat/page.tsx`)
- Real-time chat with RAG backend
- Message history and conversation management
- Dynamic configuration integration
- File upload for context

### Analytics Dashboard (`src/app/analytics/page.tsx`)
- **Performance Metrics**: Query response times and accuracy
- **System Health**: Memory, CPU, and error rate monitoring
- **Usage Analytics**: Document access patterns and user behavior
- **Interactive Charts**: Time-series data visualization

### Configuration Panel (`src/app/configuration/page.tsx`)
- **RAG Parameters**: Similarity threshold, max results, temperature
- **Model Selection**: Choose between available models
- **Real-time Updates**: Changes apply immediately to chat requests

### Knowledge Base (`src/app/knowledge-base/page.tsx`)
- **Document Upload**: Drag-and-drop file upload
- **Document Management**: View, search, and delete documents
- **Processing Status**: Real-time upload and processing feedback

## API Integration

The frontend communicates with the backend through a centralized API client (`src/lib/api.ts`):

### Endpoints Used
- `POST /api/chat/ask` - Chat requests
- `GET /api/documents` - Document listing
- `POST /api/documents/upload` - Document upload
- `GET /api/analytics/*` - Analytics data
- `GET /api/config` - Configuration retrieval
- `POST /api/config` - Configuration updates

### Configuration Management
Settings are stored in localStorage and sent with each request:
```typescript
{
  similarityThreshold: 0.7,
  maxResults: 5,
  temperature: 0.3,
  model: "gpt-4"
}
```

## Styling

### Tailwind CSS
The project uses Tailwind CSS for styling with a custom configuration:
- **Custom Colors**: Brand-specific color palette
- **Typography**: Optimized font scales and line heights
- **Spacing**: Consistent spacing system
- **Responsive**: Mobile-first responsive design

### Component Variants
UI components use `class-variance-authority` for consistent variant management:
```typescript
const buttonVariants = cva("base-styles", {
  variants: {
    variant: { default: "...", destructive: "..." },
    size: { sm: "...", lg: "..." }
  }
});
```

## Development

### Code Quality
- **TypeScript**: Strict type checking enabled
- **ESLint**: Code linting with Next.js rules
- **Prettier**: Code formatting (if configured)

### Building for Production
```bash
# Build the application
npm run build

# Start production server
npm run start
```

### Environment Variables
Create a `.env.local` file for environment-specific settings:
```
NEXT_PUBLIC_API_URL=http://localhost:9090
```

## Analytics Features

### Real-time Metrics
- **Query Performance**: Average response time, success rate
- **System Health**: Memory usage, CPU utilization
- **Error Tracking**: Error rates and failure analysis

### Visualization
- **Time Series Charts**: Historical performance data
- **Distribution Charts**: Query result distributions
- **Health Indicators**: Traffic light system for system status

### Filtering and Time Ranges
- **Date Range Picker**: Analyze data for specific periods
- **Metric Filtering**: Focus on specific metrics
- **Drill-down**: Detailed views for specific data points

## Deployment

The frontend can be deployed to:
- **Vercel**: Optimized for Next.js applications
- **Netlify**: Static site deployment
- **Docker**: Containerized deployment
- **Self-hosted**: Traditional server deployment

### Docker Deployment
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
```