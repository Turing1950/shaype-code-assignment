# Shaype Transaction Alert System

Real-time transaction monitoring with Kafka-based alerting and webhook notifications.

## Quick Start

```bash
# Start Kafka
docker run -p 9092:9092 apache/kafka:2.13-3.7.0

# Run application
./mvnw spring-boot:run
```

## Configuration

```yaml
kafka:
  bootstrap-servers: localhost:9092
  topic:
    transactions: transactions
    recon: recon

alert:
  amount-threshold: 10000
  watchlist-accounts:
    - "suspicious-account-1"
    - "suspicious-account-2"
  webhook:
    url: "http://localhost:8080/test-webhook"
    timeout-ms: 5000
```

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Application health |
| `GET /actuator/health/readiness` | Readiness probe |
| `GET /actuator/health/liveness` | Liveness probe |
| `GET /health/kafka` | Kafka connectivity |
| `GET /actuator/prometheus` | Metrics |
| `POST /test-webhook` | Test webhook receiver (test only) |

## Features

- **Real-time Processing**: Kafka consumer for transaction streams
- **Smart Alerting**: High amount & watchlist account detection
- **Webhook Delivery**: HTTP notifications with retry (exponential backoff)
- **Observability**: Prometheus metrics, structured logging with trace IDs
- **Reconciliation**: Kafka messages for audit trail
- **Health Checks**: Kubernetes-ready probes

## Alert Rules

- **High Amount**: Transactions > $10,000
- **Watchlist**: Suspicious account monitoring
- **Severity**: HIGH (both rules) | MEDIUM (watchlist only)

## Metrics

**Alerts**
- `alerts.triggered.total` - Total alerts fired
- `alerts.high_amount.total` - High amount alerts
- `alerts.watchlist.total` - Watchlist alerts

**Webhooks**
- `webhook.alerts.success.total` - Successful deliveries
- `webhook.alerts.failure.total` - Failed deliveries
- `webhook.alerts.4xx.total` - Client errors
- `webhook.alerts.5xx.total` - Server errors
- `webhook.alerts.duration` - Delivery time

**Kafka**
- `kafka.transactions.processed` - Transactions processed
- `kafka.transactions.generated_context_id` - Generated context IDs
- `kafka.transactions.processing_time` - Processing duration
- `kafka.transactions.active` - Active transactions