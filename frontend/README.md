# ZEOS Knowledge Frontend

Modern Next.js frontend for the ZEOS RAG (Retrieval-Augmented Generation) platform with a beautiful Material UI interface.

## Architecture

- **Framework**: Next.js 16 with App Router
- **Language**: TypeScript
- **UI Framework**: Material UI (MUI) with Emotion styling
- **Theme System**: Light/Dark mode with persistent theme switching
- **Charts**: Recharts for analytics visualization
- **State Management**: React Context + localStorage
- **Icons**: Material Design Icons

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
- **Material Design**: Beautiful, consistent interface using Material UI components
- **Responsive Design**: Mobile-first responsive layout with MUI breakpoints
- **Dark/Light Mode**: Seamless theme switching with persistent preferences
- **Modern Components**: Professional interface with Cards, Typography, Buttons, and Form controls
- **Real-time Updates**: Live metrics and chat updates
- **Accessibility**: Built-in accessibility features from Material UI components

## Getting Started

### Prerequisites
- Node.js 20+
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

Update the API base URL in `lib/api.ts` if your backend is running on a different port:

```typescript
const API_BASE_URL = 'http://localhost:9090';
```

## Project Structure

```
frontend/
├── app/                    # Next.js 16 App Router pages
│   ├── analytics/         # Analytics dashboard with charts
│   ├── chat/             # Interactive chat interface
│   ├── config/           # RAG configuration panel
│   ├── knowledge-base/   # Document management
│   ├── client-layout.tsx # Client-side layout with theme provider
│   ├── layout.tsx        # Root server layout
│   ├── globals.css       # Global styles (minimal, MUI handles most)
│   └── page.tsx          # Home page
├── components/           # Reusable UI components
│   └── theme-toggle.tsx  # Dark/Light mode toggle
├── lib/                 # Utility functions
│   ├── api.ts          # API client with backend integration
│   └── mui-theme.ts    # Material UI theme configuration
└── package.json        # Dependencies and scripts
```

## Key Components

### Chat Interface (`app/chat/page.tsx`)
- **Material UI Chat**: Beautiful message bubbles using Paper and Typography components
- **Real-time Chat**: RAG backend integration with loading animations
- **Message History**: Conversation management with timestamps
- **Dynamic Configuration**: User preferences applied to chat requests
- **Responsive Design**: Mobile-optimized chat interface

### Analytics Dashboard (`app/analytics/page.tsx`)
- **Performance Metrics**: Query response times and accuracy with interactive charts
- **System Health**: Real-time monitoring with Material UI Cards and indicators
- **Usage Analytics**: Document access patterns and user behavior visualization
- **Interactive Charts**: Recharts integration with Material UI theming
- **Time Range Selection**: Configurable date ranges with Material UI Buttons

### Configuration Panel (`app/config/page.tsx`)
- **RAG Parameters**: Similarity threshold, max results, temperature using MUI Sliders
- **Model Selection**: Dropdown selection with Material UI Select components
- **Form Controls**: Checkboxes, Input fields, and Form validation
- **Real-time Updates**: Changes apply immediately to chat requests
- **Guided Interface**: Helper text and parameter explanations

### Knowledge Base (`app/knowledge-base/page.tsx`)
- **Drag & Drop Upload**: Material UI Paper component with upload styling
- **Document Management**: Cards-based document listing with expand/collapse
- **File Statistics**: Document count, chunks, and storage usage
- **Processing Status**: Real-time upload feedback with progress indicators
- **Document Actions**: View, delete, and manage documents with IconButtons

## API Integration

The frontend communicates with the backend through a centralized API client (`lib/api.ts`):

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

## Styling & Theme System

### Material UI (MUI) with Emotion
The project uses Material UI for comprehensive styling and component system:
- **Design System**: Consistent Material Design components
- **Theme Configuration**: Custom light and dark themes in `lib/mui-theme.ts`
- **Color Palette**: Brand-specific colors (primary blue #2563eb, secondary purple #7c3aed)
- **Typography**: Inter font family with optimized scales
- **Responsive Design**: MUI breakpoint system for mobile-first design

### Theme Configuration
Custom themes with extensive color palettes:
```typescript
export const lightTheme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#2563eb' },
    secondary: { main: '#7c3aed' },
    success: { main: '#10b981' },
    error: { main: '#ef4444' },
    // ... additional semantic colors
  },
  typography: {
    fontFamily: '"Inter", "Helvetica", "Arial", sans-serif',
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { textTransform: 'none' }
      }
    }
  }
});
```

### Component Styling
All components use Material UI's `sx` prop for consistent styling:
```typescript
<Card sx={{
  bgcolor: 'background.paper',
  borderRadius: 2,
  '&:hover': { boxShadow: 3 }
}}>
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
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
```

## Migration to Material UI

This project has been migrated from Tailwind CSS + Radix UI to Material UI for a more consistent, professional interface:

### What Changed
- **UI Framework**: Replaced Radix UI primitives with Material UI components
- **Styling**: Moved from Tailwind CSS to Material UI's `sx` prop system
- **Theme System**: Enhanced dark/light mode with Material UI's theme provider
- **Components**: All pages now use Material UI Cards, Typography, Buttons, Forms, etc.
- **Icons**: Switched to Material Design Icons
- **Responsive Design**: Updated to use MUI's breakpoint system

### Benefits
- **Consistency**: Material Design system ensures visual consistency
- **Accessibility**: Built-in accessibility features in all MUI components
- **Maintenance**: Reduced custom CSS and component maintenance
- **Documentation**: Comprehensive MUI documentation and community support
- **Performance**: Optimized component rendering and bundle size