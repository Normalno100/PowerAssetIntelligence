# Production-ready Infrastructure (Docker Compose)

## Stack
- PostgreSQL 16 (persistent volume + healthcheck)
- Kafka (KRaft mode, internal + external listeners)
- Spring Boot API (`app`)
- Python ML Service (`ml-service`)
- Prometheus (metrics scraping)
- Loki + Promtail (centralized log collection)
- Grafana (metrics/log visualization)

## Quick start
```bash
cp .env.example .env
docker compose up -d --build
```

## Endpoints
- API: `http://localhost:8080`
- Frontend: `http://localhost:3001`
- API Health: `http://localhost:8080/actuator/health`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Loki API: `http://localhost:3100`
- ML Health: `http://localhost:8000/health`

## Notes
- Adjust credentials and ports via `.env`.
- For production, replace default passwords and use secrets manager.
- Log retention is configured in Loki (`336h`) and Docker json-file rotation (`10m x 5 files`).
