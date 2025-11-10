import Link from "next/link"
import { Search, Upload, Settings } from "lucide-react"
import { PageContainer } from "@/components/page-container"

export default function Home() {
  return (
    <PageContainer>
        <div className="text-center space-y-8">
          <div className="space-y-4">
            <h1 className="text-5xl md:text-6xl font-bold bg-gradient-to-r from-primary via-secondary to-accent bg-clip-text text-transparent">
              Intelligent Knowledge Search
            </h1>
            <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
              Ask questions about your documents and get accurate answers powered by advanced AI technology
            </p>
          </div>

          <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4">
            <Link
              href="/chat"
              className="px-8 py-3 bg-gradient-to-r from-primary to-secondary text-primary-foreground rounded-lg font-semibold hover:shadow-lg hover:shadow-primary/40 transition-all"
            >
              Start Chatting
            </Link>
            <Link
              href="/knowledge-base"
              className="px-8 py-3 border border-primary text-primary rounded-lg font-semibold hover:bg-primary/10 transition-colors"
            >
              Manage Documents
            </Link>
          </div>
        </div>

        <div className="grid md:grid-cols-3 gap-6 mt-20">
          <div className="p-6 rounded-lg border border-border bg-card/50 backdrop-blur hover:border-primary/50 transition-colors">
            <div className="w-12 h-12 bg-gradient-to-br from-primary/20 to-secondary/20 rounded-lg mb-4 flex items-center justify-center">
              <Search className="w-6 h-6 text-primary" />
            </div>
            <h3 className="font-semibold mb-2">Smart Search</h3>
            <p className="text-sm text-muted-foreground">
              Search across your entire knowledge base with semantic understanding
            </p>
          </div>
          <div className="p-6 rounded-lg border border-border bg-card/50 backdrop-blur hover:border-primary/50 transition-colors">
            <div className="w-12 h-12 bg-gradient-to-br from-secondary/20 to-accent/20 rounded-lg mb-4 flex items-center justify-center">
              <Upload className="w-6 h-6 text-secondary" />
            </div>
            <h3 className="font-semibold mb-2">Easy Upload</h3>
            <p className="text-sm text-muted-foreground">
              Upload markdown, txt, and pdf files to build your knowledge base
            </p>
          </div>
          <div className="p-6 rounded-lg border border-border bg-card/50 backdrop-blur hover:border-primary/50 transition-colors">
            <div className="w-12 h-12 bg-gradient-to-br from-accent/20 to-primary/20 rounded-lg mb-4 flex items-center justify-center">
              <Settings className="w-6 h-6 text-accent" />
            </div>
            <h3 className="font-semibold mb-2">Configure</h3>
            <p className="text-sm text-muted-foreground">Fine-tune embeddings, models, and similarity thresholds</p>
          </div>
        </div>
    </PageContainer>
  )
}