# Конфигурация токенов для разных окружений

## 🏠 Локальная разработка (dev)

**Источник токена:** Переменная окружения `ZTOKEN`

```bash
export ZTOKEN="your-local-ztoken-here"
./gradlew bootRun
```

**Логика:**
- Использует `ZTOKEN` из переменной окружения
- Если есть `X-TokenInfo-Forward` заголовок - приоритет у него
- Fallback на `ZTOKEN` если заголовка нет

---

## 🧪 Тесты (test profile)

**Источник токена:** Заглушки (mocks)

```bash
./gradlew test
```

**Логика:**
- `MockChatModel` и `MockVectorStore` заглушки
- Реальные API вызовы к LLM не выполняются
- Spring AI отключен (`enabled: false`)
- `require-header-token: false`

---

## 🚀 Staging/Production

**Источник токена:** ТОЛЬКО заголовок `X-TokenInfo-Forward`

**Конфигурация:**
```yaml
rag:
  token:
    require-header-token: true  # ОБЯЗАТЕЛЬНО из заголовка
```

**Логика:**
1. ✅ Токен найден в `X-TokenInfo-Forward` → используется
2. ❌ Токен НЕ найден в заголовке → запрос отклоняется

**Пример запроса:**
```bash
curl -X POST https://staging.example.com/api/chat/ask \
  -H "X-TokenInfo-Forward: {\"access_token\":\"staging-user-token\"}" \
  -H "Content-Type: application/json" \
  -d '{"question": "Test"}'
```

---

## 🔍 Логирование

- `TokenInfoService` логирует источник токена
- В staging видно откуда взят токен:
  - `"Using access_token from X-TokenInfo-Forward header"`
  - `"X-TokenInfo-Forward header is required but not present"`

---

## ⚙️ Активация профилей

```bash
# Локально
./gradlew bootRun

# Тесты
./gradlew test

# Staging
java -jar app.jar --spring.profiles.active=staging
```
