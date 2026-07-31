# AWS Credential Configuration for Different Environments

The application uses AWS Bedrock exclusively and resolves credentials via the
[AWS default credentials chain](https://docs.aws.amazon.com/sdkref/latest/guide/standardized-credentials.html)
in every profile. No application code handles credentials directly.

## 🏠 Local Development (dev)

**Credential Source:** `~/.aws/credentials`, `aws configure`, or exported environment variables.

```bash
export AWS_REGION="eu-central-1"
./gradlew bootRun
```

---

## 🧪 Tests (test profile)

**Credential Source:** None — Spring AI is disabled entirely.

```bash
./gradlew test
```

No real API calls are made to Bedrock during tests.

---

## 🚀 Staging/Production

**Credential Source:** IAM role or EC2/EKS instance profile provided by the platform's infrastructure.

```yaml
spring:
  ai:
    bedrock:
      aws:
        region: ${AWS_REGION:eu-central-1}
        # No explicit credentials - resolved via default credentials chain
```

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
