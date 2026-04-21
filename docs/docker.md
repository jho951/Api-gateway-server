# Docker

## 실행 요구 사항

- Java 17
- Docker
- Docker Compose

## 기본 포트

- Gateway: `http://localhost:8080`

## 사전 준비

- auth-service 기동
- authz-service 기동
- user-service 기동
- block-service 기동
- redis 기동
- Gateway 전용 환경변수 설정

## Compose 파일

| 환경 | Compose 파일 | env 파일 | 배포 방식 |
| --- | --- | --- | --- |
| `dev` | `docker/compose.yml` + `docker/dev/compose.yml` | `.env.dev` | 로컬 build |
| `prod` | `docker/compose.yml` + `docker/prod/compose.yml` | `.env.prod` | GHCR image pull |

멀티모듈 기준:

- 실행 모듈: `app`
- 공용 모듈: `common`
- Docker 이미지에는 `app/build/libs/gateway-service.jar`가 들어갑니다.

실행:

```bash
./scripts/run.docker.sh up dev
./scripts/run.docker.sh up prod
```

종료:

```bash
./scripts/run.docker.sh down dev
./scripts/run.docker.sh down prod
```

Local JVM:

```bash
bash scripts/run.local.sh
```

로컬 JVM 실행은 내부적으로 `./gradlew :app:bootRun`을 호출합니다.

## 기동 확인

```bash
curl -i http://localhost:8080/health
curl -i http://localhost:8080/ready
```

## 주요 환경변수

| 환경변수 | 용도 |
| --- | --- |
| `GATEWAY_IMAGE` | prod compose가 pull할 GHCR 이미지 태그 |
| `AUTH_SERVICE_URL` | auth-service base URL |
| `USER_SERVICE_URL` | user-service base URL |
| `BLOCK_SERVICE_URL` | block/document service base URL |
| `AUTHZ_ADMIN_VERIFY_URL` | authz-service 관리자 권한 검증 API 전체 URL |
| `GATEWAY_INTERNAL_REQUEST_SECRET` | authz-service legacy/hybrid internal secret |
| `AUTHZ_INTERNAL_JWT_SECRET` | authz-service 운영 JWT internal auth secret |
| `AUTHZ_INTERNAL_JWT_ISSUER` | authz-service 운영 JWT issuer |
| `AUTHZ_INTERNAL_JWT_AUDIENCE` | authz-service 운영 JWT audience |
| `REDIS_HOST` | Redis host |
| `REDIS_PORT` | Redis port |

현재 코드는 authz 검증 호출에 `AUTHZ_ADMIN_VERIFY_URL`, `GATEWAY_INTERNAL_REQUEST_SECRET`, `AUTHZ_INTERNAL_JWT_*`를 사용합니다.

## 디렉토리 구조

```text
docker
├── Dockerfile
├── compose.yml
├── dev
│   └── compose.yml
└── prod
    └── compose.yml
```

- `docker/compose.yml`: auth-service, authz-service와 같은 공통 base compose
- `docker/dev/compose.yml`: 개발용 override, 로컬 build 사용
- `docker/prod/compose.yml`: 운영용 override, `GATEWAY_IMAGE` pull 사용

## GitHub Actions 배포 예시

현재 `cd.yml`은 `DEPLOY_COMMAND` secret 안의 쉘 명령을 그대로 실행합니다.

구조는 auth-service/authz-service와 같은 `docker/compose.yml + docker/{env}/compose.yml` 레이어를 따르되, gateway는 `prod`에서 GHCR image pull 배포를 사용합니다.

배포 step에서 바로 쓸 수 있는 환경변수:

- `DEPLOY_ENVIRONMENT`: `dev` 또는 `prod`
- `GITHUB_SHA`: 배포할 커밋 SHA
- `IMAGE_REF`: `ghcr.io/<owner>/<repo>:<sha>`
- `GH_TOKEN`, `GITHUB_ACTOR`: Dockerfile이 GitHub Packages 의존성 해석에 사용할 값

권장 `DEPLOY_COMMAND` secret 예시:

```bash
ssh -o StrictHostKeyChecking=no "$DEPLOY_SSH_USER@$DEPLOY_SSH_HOST" "
  set -euo pipefail
  cd /srv/gateway-service
  export SERVICE_SHARED_NETWORK=\"service-backbone-shared\"
  export GATEWAY_IMAGE=\"$IMAGE_REF\"
  if [ \"$DEPLOY_ENVIRONMENT\" = \"prod\" ]; then
    docker compose --env-file .env.prod -f docker/compose.yml -f docker/prod/compose.yml pull
    docker compose --env-file .env.prod -f docker/compose.yml -f docker/prod/compose.yml up -d
  else
    export GH_TOKEN=\"$GH_TOKEN\"
    export GITHUB_ACTOR=\"$GITHUB_ACTOR\"
    docker compose --env-file .env.dev -f docker/compose.yml -f docker/dev/compose.yml up -d --build
  fi
"
```

원격 서버 준비 조건:

- `/srv/gateway-service`에 이 레포가 clone 되어 있어야 합니다.
- `.env.dev` 또는 `.env.prod`가 서버에 있어야 합니다.
- Docker, Docker Compose, Git이 설치되어 있어야 합니다.
- `SERVICE_SHARED_NETWORK`에 해당하는 외부 Docker network가 있어야 합니다.

운영 기준:

- `dev`: 원격 또는 로컬에서 `docker compose up --build`
- `prod`: `GATEWAY_IMAGE=ghcr.io/...:<sha>`를 주입하고 `docker compose pull && up -d`
