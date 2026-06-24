# Order Processor API

The HTTP API is an optional extension point over the same order-processing path used by the AWS Lambda handler. It is disabled by default so the Lambda deployment remains the primary runtime.

## Enable API Mode

Run the application as a servlet web app and provide an API token from the environment:

```bash
TOKEN="$(openssl rand -hex 32)"

SPRING_MAIN_WEB_APPLICATION_TYPE=servlet \
ORDER_API_ENABLED=true \
ORDER_ENABLE_NOTIFICATION_PUBLISHING=false \
ORDER_API_AUTH_TOKEN="$TOKEN" \
mvn spring-boot:run
```

Default base URL:

```text
http://localhost:8080/api/v1/orders
```

The base path is configurable with `ORDER_API_BASE_PATH`.

## Authentication

The API uses a stateless bearer-token filter when `order.api.enabled=true`.

| Header | Required | Description |
| --- | --- | --- |
| `Authorization` | Yes | `Bearer <ORDER_API_AUTH_TOKEN>` by default. Header name is configurable with `ORDER_API_AUTH_HEADER_NAME`. |
| `Content-Type` | Yes | Must be `application/json`. |
| `X-Request-Id` | No | Client supplied request id. A UUID is generated when absent. |
| `X-Forwarded-For` | No | First IP is used as the structured-log source IP when present. |

Never store `ORDER_API_AUTH_TOKEN` in source control. Use a deployment secret, AWS Secrets Manager, SSM Parameter Store, or the secret mechanism provided by your platform.

Keep the Spring Boot process running while testing from another terminal. `curl: (7) Failed to connect` means the servlet API is not listening on port `8080`. A `502 Unable to publish OrderProcessedEvent to SNS` response means authentication and validation passed, but SNS publishing is enabled without reachable AWS credentials, topic permissions, or a local AWS emulator. Use `ORDER_ENABLE_NOTIFICATION_PUBLISHING=false` for controller-level local testing, or configure AWS/LocalStack for an end-to-end SNS test.

## Process Order

```http
POST /api/v1/orders/process
Authorization: Bearer replace-with-a-real-token
Content-Type: application/json
X-Request-Id: api-request-001
```

Request body is a direct `OrderEvent` JSON payload. It is not wrapped in an SNS envelope for API mode.

### Digital Order Request

```json
{
  "eventId": "evt-digital-001",
  "correlationId": "corr-001",
  "orderId": "order-001",
  "customerId": "customer-12345",
  "customerEmail": "ada@example.com",
  "orderType": "DIGITAL",
  "amount": 49.99,
  "currency": "USD",
  "occurredAt": "2026-06-23T10:15:30Z",
  "productCode": "EBOOK-001",
  "downloadUrl": "https://downloads.example.com/order-001"
}
```

### Physical Order Request

```json
{
  "eventId": "evt-physical-001",
  "correlationId": "corr-002",
  "orderId": "order-002",
  "customerId": "customer-67890",
  "customerEmail": "grace@example.com",
  "orderType": "PHYSICAL",
  "amount": 89.99,
  "currency": "USD",
  "occurredAt": "2026-06-23T10:20:30Z",
  "shippingAddress": "1 Lambda Way, Seattle, WA",
  "shippingMethod": "EXPRESS"
}
```

### Success Response

Returns `202 Accepted` after validation, idempotency checks, processing, and SNS notification publishing.

```json
{
  "status": "PROCESSED",
  "eventId": "evt-digital-001",
  "processedEventId": "<generated-uuid>",
  "correlationId": "corr-001",
  "orderId": "order-001",
  "orderType": "DIGITAL",
  "processedAt": "<current-timestamp>"
}
```

Duplicate events return `202 Accepted` with `status` set to `DUPLICATE_SKIPPED` and no `processedEventId`.

### Error Responses

| Status | Cause |
| --- | --- |
| `400 Bad Request` | Validation failure, unknown `orderType`, oversized payload, or malformed JSON. |
| `401 Unauthorized` | Missing or invalid bearer token. |
| `502 Bad Gateway` | SNS publish failure after processing. |
| `500 Internal Server Error` | Unexpected processing failure. |

Error body:

```json
{
  "timestamp": "2026-06-23T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "orderType is required"
}
```

## Curl Example

```bash
curl -i -X POST "http://localhost:8080/api/v1/orders/process" \
  -H "Authorization: Bearer replace-with-a-real-token" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: api-request-001" \
  --data-binary @samples/digital-order.json
```

## OpenAPI

The Swagger/OpenAPI 3 specification is available in:

```text
docs/openapi.yaml
```

Import that file into Swagger Editor, Postman, API Gateway, or a service catalog.
