"use client"

import { useState, useEffect } from "react"
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Typography,
  Box,
  Alert,
  Chip,
  IconButton,
  Tooltip,
} from "@mui/material"
import { Key, CheckCircle, Warning, Delete } from "@mui/icons-material"
import {
  setToken,
  getToken,
  clearToken,
  getTokenExpiry,
  isTokenExpired,
} from "@/lib/token-store"

export function TokenPanel() {
  const [open, setOpen] = useState(false)
  const [tokenInput, setTokenInput] = useState("")
  const [currentToken, setCurrentToken] = useState<string | null>(null)
  const [expiry, setExpiry] = useState<Date | null>(null)
  const [error, setError] = useState<string | null>(null)

  const refresh = () => {
    const t = getToken()
    setCurrentToken(t)
    setExpiry(t ? getTokenExpiry(t) : null)
  }

  useEffect(() => {
    refresh()
    // Re-check expiry every minute
    const interval = setInterval(refresh, 60_000)
    return () => clearInterval(interval)
  }, [])

  const handleSave = () => {
    setError(null)
    const trimmed = tokenInput.trim()
    if (!trimmed) {
      setError("Token cannot be empty")
      return
    }
    // Basic JWT structure check: three base64url parts separated by dots
    if (trimmed.split('.').length !== 3) {
      setError("Does not look like a valid JWT token (expected header.payload.signature format)")
      return
    }
    if (isTokenExpired(trimmed)) {
      setError("This token is already expired — please run `ztoken` to get a fresh one")
      return
    }
    setToken(trimmed)
    setTokenInput("")
    refresh()
    setOpen(false)
  }

  const handleClear = () => {
    clearToken()
    setTokenInput("")
    refresh()
  }

  const tokenStatus = () => {
    if (!currentToken) return "none"
    if (isTokenExpired(currentToken)) return "expired"
    if (expiry) {
      const minutesLeft = (expiry.getTime() - Date.now()) / 60_000
      if (minutesLeft < 15) return "expiring"
    }
    return "valid"
  }

  const status = tokenStatus()

  return (
    <>
      <Tooltip title="Configure datalake access token">
        <IconButton
          onClick={() => setOpen(true)}
          size="small"
          sx={{
            border: 1,
            borderColor: status === "valid" ? "success.main"
              : status === "expiring" ? "warning.main"
              : status === "expired" ? "error.main"
              : "divider",
          }}
          aria-label="Configure token"
        >
          {status === "valid" || status === "expiring" ? (
            <CheckCircle fontSize="small" color={status === "expiring" ? "warning" : "success"} />
          ) : (
            <Key fontSize="small" color={status === "expired" ? "error" : "disabled"} />
          )}
        </IconButton>
      </Tooltip>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Datalake Access Token</DialogTitle>
        <DialogContent>
          <Box sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              To query the Zalando datalake, paste your token below. Run{" "}
              <code style={{ background: "rgba(0,0,0,0.08)", padding: "2px 4px", borderRadius: 3 }}>
                ztoken
              </code>{" "}
              in your terminal to get one. Tokens expire after ~2 hours — the indicator in the
              toolbar will turn yellow when it&apos;s close to expiry.
            </Typography>

            <Typography variant="caption" color="text.secondary" sx={{ fontStyle: "italic" }}>
              Stored in browser session storage only — cleared when you close this tab.
            </Typography>

            {/* Current token status */}
            {currentToken && (
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, flexWrap: "wrap" }}>
                <Chip
                  label={
                    status === "valid" ? "Token active"
                    : status === "expiring" ? "Expires soon"
                    : "Token expired"
                  }
                  color={status === "valid" ? "success" : status === "expiring" ? "warning" : "error"}
                  size="small"
                  icon={status === "expired" ? <Warning /> : <CheckCircle />}
                />
                {expiry && (
                  <Typography variant="caption" color="text.secondary">
                    {status === "expired"
                      ? `Expired ${expiry.toLocaleTimeString()}`
                      : `Expires ${expiry.toLocaleTimeString()}`}
                  </Typography>
                )}
                <Tooltip title="Remove token">
                  <IconButton size="small" onClick={handleClear} color="error">
                    <Delete fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Box>
            )}

            {error && <Alert severity="error">{error}</Alert>}

            <TextField
              label="Paste token here"
              multiline
              rows={4}
              value={tokenInput}
              onChange={(e) => { setTokenInput(e.target.value); setError(null) }}
              placeholder="eyJ..."
              fullWidth
              inputProps={{ style: { fontFamily: "monospace", fontSize: 12 } }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setOpen(false); setTokenInput(""); setError(null) }}>
            Cancel
          </Button>
          <Button onClick={handleSave} variant="contained" disabled={!tokenInput.trim()}>
            Save Token
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
