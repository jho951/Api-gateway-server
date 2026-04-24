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
- editor-service 기동
- Redis endpoint 준비
  dev 단일 Docker host에서는 `redis-server` 또는 `central-redis` alias를 사용할 수 있습니다.
  EC2 분산 배포에서는 반드시 `REDIS_HOST`에 private DNS/IP 또는 관리형 Redis endpoint를 넣어야 합니다.
- Gateway 전용 환경변수 설정

## Compose 파일

| 환경 | Compose 파일 | env 파일 | 배포 방식 |
| --- | --- | --- | --- |
| `dev` | `docker/compose.yml` + `docker/dev/compose.yml` | `.env.dev` | 로컬 build |
| `prod` | `docker/compose.yml` + `docker/prod/compose.yml` | `.env.prod` | ECR image pull |

빌드 전용 override:

- `docker/compose.build.yml`: private package 인증이 필요한 build 설정만 포함

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
| `GATEWAY_IMAGE` | prod compose가 pull할 ECR 이미지 태그 |
| `AUTH_SERVICE_URL` | auth-service base URL |
| `USER_SERVICE_URL` | user-service base URL |
| `EDITOR_SERVICE_URL` | editor-service base URL (`BLOCK_SERVICE_URL` legacy fallback 허용) |
| `AUTHZ_ADMIN_VERIFY_URL` | authz-service 관리자 권한 검증 API 전체 URL |
| `GATEWAY_INTERNAL_REQUEST_SECRET` | authz-service legacy/hybrid internal secret |
| `AUTHZ_INTERNAL_JWT_SECRET` | authz-service 운영 JWT internal auth secret |
| `AUTHZ_INTERNAL_JWT_ISSUER` | authz-service 운영 JWT issuer |
| `AUTHZ_INTERNAL_JWT_AUDIENCE` | authz-service 운영 JWT audience |
| `REDIS_HOST` | Redis host |
| `REDIS_PORT` | Redis port |

운영 배포 기준:

- `AUTH_SERVICE_URL`, `USER_SERVICE_URL`, `EDITOR_SERVICE_URL`, `AUTHZ_ADMIN_VERIFY_URL`, `REDIS_HOST`는 single-host Compose alias가 아니라 VPC 내부 private DNS/IP 기준으로 넣습니다.
- `docker/prod/compose.yml`의 기본 bind는 `127.0.0.1:8080`입니다. Gateway만 public ingress 대상이고, public bind가 필요하면 Nginx/ALB 앞단 또는 Security Group 제한을 같이 둡니다.

현재 코드는 authz 검증 호출에 `AUTHZ_ADMIN_VERIFY_URL`, `GATEWAY_INTERNAL_REQUEST_SECRET`, `AUTHZ_INTERNAL_JWT_*`를 사용합니다. editor upstream은 `EDITOR_SERVICE_URL`을 canonical로 읽고, `BLOCK_SERVICE_URL`은 legacy fallback으로만 허용합니다.

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
- `docker/dev/compose.yml`: 개발용 실행 override, image/env/port만 선언
- `docker/compose.build.yml`: 개발 또는 CI build 전용 override, `build`와 secret만 선언
- `docker/prod/compose.yml`: 운영용 override, `GATEWAY_IMAGE` pull 사용

## GitHub Actions 배포 예시

현재 `cd.yml`은 `DEPLOY_COMMAND` secret 안의 쉘 명령을 그대로 실행합니다.

구조는 auth-service/authz-service와 같은 `docker/compose.yml + docker/{env}/compose.yml` 레이어를 따르되, gateway는 `prod`에서 ECR image pull 배포를 사용합니다.

배포 step에서 바로 쓸 수 있는 환경변수:

- `DEPLOY_ENVIRONMENT`: `dev` 또는 `prod`
- `GITHUB_SHA`: 배포할 커밋 SHA
- `IMAGE_REF`: `<account>.dkr.ecr.<region>.amazonaws.com/<env>-gateway-service:<sha>`
- `GH_TOKEN`, `GITHUB_ACTOR`: Dockerfile이 GitHub Packages 의존성 해석에 사용할 값
  운영 runtime에서는 사용하지 않고, CI 또는 로컬 build 단계에서만 사용

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
- 여러 서비스를 같은 EC2에 함께 올릴 때만 `SERVICE_SHARED_NETWORK` 외부 Docker network가 필요합니다.
- 서비스를 EC2별로 분리하면 gateway는 private DNS/IP로 다른 서비스를 호출하고 Docker network는 호스트 내부 통신 용도로만 사용합니다.

운영 기준:

- `dev`: 원격 또는 로컬에서 `docker compose up --build`
- `prod`: `GATEWAY_IMAGE=<account>.dkr.ecr.<region>.amazonaws.com/<env>-gateway-service:<sha>`를 주입하고 `docker compose pull && up -d`
