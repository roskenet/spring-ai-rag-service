# Token Configuration for Different Environments

## 🏠 Local Development (dev)

**Token Source:** Environment variable `ZTOKEN`

```bash
export ZTOKEN="your-local-ztoken-here"
./gradlew bootRun
```

**Logic:**
- Uses `ZTOKEN` from environment variable
- If `X-TokenInfo-Forward` header is present - it takes priority
- Fallback to `ZTOKEN` if no header

---

## 🧪 Tests (test profile)

**Token Source:** Mocks/stubs

```bash
./gradlew test
```

**Logic:**
- `DynamicTokenConfiguration` excluded with `@Profile("!test")`
- No real API calls to LLM services
- Standard Spring AI auto-configuration
- `require-header-token: false`

---

## 🚀 Staging/Production

**Token Source:** ONLY `X-TokenInfo-Forward` header

**Configuration:**
```yaml
rag:
  token:
    require-header-token: true  # MANDATORY from header
```

**Logic:**
1. ✅ Token found in `X-TokenInfo-Forward` → used
2. ❌ Token NOT found in header → request rejected

**Example request:**
```bash
curl -X POST https://staging.example.com/api/chat/ask \
  -H "X-TokenInfo-Forward: {\"access_token\":\"staging-user-token\"}" \
  -H "Content-Type: application/json" \
  -d '{"question": "Test"}'
```

---

## 🔍 Logging

- `TokenInfoService` logs token source
- In staging you can see where token came from:
  - `"Using access_token from X-TokenInfo-Forward header"`
  - `"X-TokenInfo-Forward header is required but not present"`

---

## ⚙️ Profile Activation

```bash
# Local development
./gradlew bootRun

# Tests
./gradlew test

# Staging
java -jar app.jar --spring.profiles.active=staging
```
