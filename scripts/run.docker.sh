#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_PROJECT_NAME="api-gateway-server"
ACTION="${1:-up}"
ENV_NAME="${2:-dev}"
shift $(( $# > 0 ? 1 : 0 )) || true
shift $(( $# > 0 ? 1 : 0 )) || true

usage() {
  echo "Usage: ./scripts/run.docker.sh [up|down|build|logs|ps|restart] [dev|prod] [docker compose options]" >&2
}

case "$ACTION" in
  up|down|build|logs|ps|restart) ;;
  *) usage; exit 1 ;;
esac

case "$ENV_NAME" in
  dev|prod)
    BASE_COMPOSE_FILE="$PROJECT_ROOT/docker/compose.yml"
    ENV_COMPOSE_FILE="$PROJECT_ROOT/docker/$ENV_NAME/compose.yml"
    ;;
  *) usage; exit 1 ;;
esac

ENV_FILE="$PROJECT_ROOT/.env.$ENV_NAME"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Env file not found: $ENV_FILE" >&2
  echo "Create it from .env.example before running Docker." >&2
  exit 1
fi

if [[ ! -f "$BASE_COMPOSE_FILE" ]]; then
  echo "Base Compose file not found: $BASE_COMPOSE_FILE" >&2
  exit 1
fi

if [[ ! -f "$ENV_COMPOSE_FILE" ]]; then
  echo "Compose file not found: $ENV_COMPOSE_FILE" >&2
  exit 1
fi

gradle_property() {
  local key="$1"
  local gradle_properties="${HOME}/.gradle/gradle.properties"
  [[ -f "$gradle_properties" ]] || return 0
  awk -F= -v key="$key" '$1 == key { print $2; exit }' "$gradle_properties"
}

if [[ -z "${GH_TOKEN:-}" ]]; then
  GH_TOKEN="$(gradle_property githubPackagesToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property githubToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property ghToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property gh_token)"
  export GH_TOKEN
fi

if [[ -z "${GITHUB_ACTOR:-}" ]]; then
  GITHUB_ACTOR="$(gradle_property githubPackagesUsername)"
  [[ -n "$GITHUB_ACTOR" ]] || GITHUB_ACTOR="$(gradle_property githubUsername)"
  [[ -n "$GITHUB_ACTOR" ]] || GITHUB_ACTOR="jho951"
  export GITHUB_ACTOR
fi

SHARED_NETWORK="${SERVICE_SHARED_NETWORK:-${BACKEND_SHARED_NETWORK:-${MSA_SHARED_NETWORK:-service-backbone-shared}}}"
if ! docker network inspect "$SHARED_NETWORK" >/dev/null 2>&1; then
  echo "Creating external network: $SHARED_NETWORK"
  docker network create "$SHARED_NETWORK" >/dev/null
fi

compose() {
  APP_ENV="$ENV_NAME" GATEWAY_ENV_FILE="$ENV_FILE" SERVICE_SHARED_NETWORK="$SHARED_NETWORK" BACKEND_SHARED_NETWORK="$SHARED_NETWORK" MSA_SHARED_NETWORK="$SHARED_NETWORK" \
    docker compose --env-file "$ENV_FILE" -p "$COMPOSE_PROJECT_NAME" -f "$BASE_COMPOSE_FILE" -f "$ENV_COMPOSE_FILE" "$@"
}

case "$ACTION" in
  up)
    if [[ "$ENV_NAME" == "prod" ]]; then
      compose pull "$@"
      compose up -d "$@"
    else
      compose up --build "$@"
    fi
    ;;
  down) compose down --remove-orphans "$@" ;;
  build) compose build "$@" ;;
  logs) compose logs -f "$@" ;;
  ps) compose ps "$@" ;;
  restart)
    compose down --remove-orphans
    if [[ "$ENV_NAME" == "prod" ]]; then
      compose pull "$@"
      compose up -d "$@"
    else
      compose up --build "$@"
    fi
    ;;
esac
