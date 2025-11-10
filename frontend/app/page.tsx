import Link from "next/link"
import { Box, Typography, Button, Card, CardContent, Avatar } from "@mui/material"
import { Search, Upload, Settings } from "@mui/icons-material"

export default function Home() {
  return (
    <Box>
      {/* Hero Section */}
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography
          variant="h1"
          sx={{
            fontSize: { xs: '2.5rem', md: '4rem' },
            fontWeight: 700,
            background: 'linear-gradient(45deg, #2563eb, #7c3aed, #3b82f6)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent',
            mb: 2,
          }}
        >
          Intelligent Knowledge Search
        </Typography>

        <Typography
          variant="h5"
          color="text.secondary"
          sx={{ maxWidth: '600px', mx: 'auto', mb: 4 }}
        >
          Ask questions about your documents and get accurate answers powered by advanced AI technology
        </Typography>

        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, gap: 2, justifyContent: 'center' }}>
          <Link href="/chat" style={{ textDecoration: 'none' }}>
            <Button
              variant="contained"
              size="large"
              sx={{
                px: 4,
                py: 1.5,
                background: 'linear-gradient(45deg, #2563eb, #7c3aed)',
                '&:hover': {
                  background: 'linear-gradient(45deg, #1d4ed8, #6d28d9)',
                  boxShadow: '0 8px 25px rgba(37, 99, 235, 0.3)',
                },
              }}
            >
              Start Chatting
            </Button>
          </Link>
          <Link href="/knowledge-base" style={{ textDecoration: 'none' }}>
            <Button
              variant="outlined"
              size="large"
              sx={{ px: 4, py: 1.5 }}
            >
              Manage Documents
            </Button>
          </Link>
        </Box>
      </Box>

      {/* Features Section */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 3, mt: 8 }}>
        <Card
          sx={{
            height: '100%',
            transition: 'all 0.3s ease',
            '&:hover': {
              borderColor: 'primary.main',
              boxShadow: 3,
            },
          }}
        >
          <CardContent sx={{ p: 3 }}>
            <Avatar
              sx={{
                width: 56,
                height: 56,
                mb: 2,
                bgcolor: 'primary.main',
                background: 'linear-gradient(45deg, rgba(37, 99, 235, 0.2), rgba(124, 58, 237, 0.2))',
              }}
            >
              <Search color="primary" />
            </Avatar>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
              Smart Search
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Search across your entire knowledge base with semantic understanding
            </Typography>
          </CardContent>
        </Card>

        <Card
          sx={{
            height: '100%',
            transition: 'all 0.3s ease',
            '&:hover': {
              borderColor: 'primary.main',
              boxShadow: 3,
            },
          }}
        >
          <CardContent sx={{ p: 3 }}>
            <Avatar
              sx={{
                width: 56,
                height: 56,
                mb: 2,
                bgcolor: 'secondary.main',
                background: 'linear-gradient(45deg, rgba(124, 58, 237, 0.2), rgba(59, 130, 246, 0.2))',
              }}
            >
              <Upload color="secondary" />
            </Avatar>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
              Easy Upload
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Upload markdown, txt, and pdf files to build your knowledge base
            </Typography>
          </CardContent>
        </Card>

        <Card
          sx={{
            height: '100%',
            transition: 'all 0.3s ease',
            '&:hover': {
              borderColor: 'primary.main',
              boxShadow: 3,
            },
          }}
        >
          <CardContent sx={{ p: 3 }}>
            <Avatar
              sx={{
                width: 56,
                height: 56,
                mb: 2,
                bgcolor: 'info.main',
                background: 'linear-gradient(45deg, rgba(59, 130, 246, 0.2), rgba(37, 99, 235, 0.2))',
              }}
            >
              <Settings color="info" />
            </Avatar>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
              Configure
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Fine-tune embeddings, models, and similarity thresholds
            </Typography>
          </CardContent>
        </Card>
      </Box>
    </Box>
  )
}