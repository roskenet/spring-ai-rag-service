# Тестирование извлечения токена из X-TokenInfo-Forward

## Что было реализовано:

### 1. TokenInfoService
- Извлекает `access_token` из заголовка `X-TokenInfo-Forward`
- Парсит JSON и извлекает поле `access_token`
- Возвращает fallback на `ZTOKEN` если токен не найден

### 2. DynamicTokenConfiguration
- Настраивает interceptor для всех HTTP запросов к OpenAI API
- Динамически подставляет правильный токен в Authorization header

### 3. Логика работы:
1. Входящий HTTP запрос содержит заголовок `X-TokenInfo-Forward` с JSON
2. `TokenInfoService` извлекает `access_token` из JSON
3. `DynamicTokenInterceptor` использует этот токен для всех запросов к LLM API
4. Если токен не найден - используется `ZTOKEN` из конфигурации

## Тест:

Для тестирования отправьте запрос с заголовком:

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -H "X-TokenInfo-Forward: {\"access_token\":\"your-test-token\",\"sub\":\"user123\",\"exp\":1234567890}" \
  -d '{
    "question": "Тестовый вопрос",
    "maxResults": 5,
    "includeSourceInfo": true,
    "similarityThreshold": 0.3
  }'
```

## Ожидаемое поведение:
1. В логах будет видно: "Using access_token from X-TokenInfo-Forward header for LLM request"
2. Запросы к OpenAI API будут использовать токен из заголовка вместо ZTOKEN
3. При отсутствии заголовка будет использоваться ZTOKEN из переменной окружения

## Важные моменты:
- Токен извлекается из RequestContextHolder, поэтому работает только в контексте HTTP запроса
- Interceptor применяется ко всем запросам к OpenAI API (chat и embedding)
- Реализация fallback'а гарантирует работу без заголовка