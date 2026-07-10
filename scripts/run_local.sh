#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-json}"
SKIP_IMPORT="${2:-}"

cd "$ROOT_DIR"

case "$MODE" in
  json)
    export INSIGHTFLOW_ANALYTICS_MODE=json
    export INSIGHTFLOW_AI_PROVIDER="${INSIGHTFLOW_AI_PROVIDER:-local}"
    export INSIGHTFLOW_JWT_SECRET="${INSIGHTFLOW_JWT_SECRET:-dev-only-change-me-dev-only-change-me}"
    echo "Starting InsightFlow in JSON fallback mode."
    ;;
  postgres)
    export INSIGHTFLOW_ANALYTICS_MODE=postgres
    export INSIGHTFLOW_AI_PROVIDER="${INSIGHTFLOW_AI_PROVIDER:-local}"
    export INSIGHTFLOW_JWT_SECRET="${INSIGHTFLOW_JWT_SECRET:-dev-only-change-me-dev-only-change-me}"
    echo "Starting PostgreSQL with Docker Compose."
    docker compose up -d
    echo "Waiting for PostgreSQL healthcheck."
    postgres_ready=false
    for _ in {1..30}; do
      if docker compose exec -T postgres pg_isready -U "${POSTGRES_USER:-insightflow_user}" -d "${POSTGRES_DB:-insightflow}" >/dev/null 2>&1; then
        postgres_ready=true
        break
      fi
      sleep 2
    done
    if [ "$postgres_ready" != "true" ]; then
      echo "PostgreSQL did not become healthy. Run 'docker compose logs postgres' for details." >&2
      exit 1
    fi
    if [ "$SKIP_IMPORT" != "--skip-import" ]; then
      echo "Importing Olist CSV files into PostgreSQL."
      python3 scripts/import_olist_to_postgres.py
    else
      echo "Skipping CSV import."
    fi
    ;;
  *)
    echo "Usage: ./scripts/run_local.sh [json|postgres] [--skip-import]" >&2
    exit 1
    ;;
esac

echo "Opening http://localhost:8080 after Spring Boot starts."
cd "$ROOT_DIR/backend"
./mvnw spring-boot:run
