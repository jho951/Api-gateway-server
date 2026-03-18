# API Gateway Server

`docs/Design.md` 기준의 정책형 API Gateway 서버입니다.

## 현재 반영된 구조

- 외부 단일 진입점
- 현재 기본 운영 역할: `public / protected`
- 확장용 경로 타입 `admin / internal` 코드는 보유
- Auth Service 세션 검증 위임
- 일반 보호 경로용 짧은 TTL auth validation cache
- 관리자/내부 정책 로직 보유, 현재 기본 비활성
- Redis 중앙 1대 기준 설정
- `ip-guard` 기반 일반/관리자 IP allowlist
- trusted header 제거 후 게이트웨이 재주입
- request id / correlation id 주입
- 로그인 시작 경로 기본 rate limit
- 공통 JSON 에러 모델

## 서비스 URL 설정

다음 URL은 필수입니다.

- `AUTH_SERVICE_URL`
- `USER_SERVICE_URL`
- `BLOCK_SERVICE_URL`

선택:

- `PERMISSION_SERVICE_URL`
- `DOCUMENT_SERVICE_URL`
- `AUTH_VALIDATE_URL`
- `PERMISSION_ADMIN_VERIFY_URL`

기본값이 없는 경우 게이트웨이는 시작하지 않습니다.

## 주요 내부 호출

- 세션 검증: `POST /auth/internal/session/validate`
- 관리자 권한 추가 확인: `POST /permissions/internal/admin/verify`
- 관리자 권한 캐시: Redis

현재 기본 동작은 `Auth Service` 기반 검증만 수행하며, `Permission Service` 확인은 `GATEWAY_ADMIN_PERMISSION_CHECK_ENABLED=true` 일 때만 활성화됩니다.

## 라우트 정책

- `PUBLIC`
  - `/auth/login/github`
  - `/auth/oauth/github/callback`
- `PROTECTED`
  - `/auth/session`
  - `/users/me`
  - `/documents/**`
  - `/blocks/**`
  - `/permissions/**`
- `ADMIN`
  - `/admin/users/**`
  - `/admin/blocks/**`
  - `/admin/permissions/**`
- `INTERNAL`
  - `/auth/internal/**`
  - `/internal/**`

## 실행 예시

```bash
export AUTH_SERVICE_URL=http://localhost:8080
export USER_SERVICE_URL=http://localhost:8081
export BLOCK_SERVICE_URL=http://localhost:8082
export PERMISSION_SERVICE_URL=http://localhost:8083
export DOCUMENT_SERVICE_URL=http://localhost:8084

export GATEWAY_PORT=8080
export GATEWAY_ALLOWED_IPS=127.0.0.1,10.0.0.0/8
export GATEWAY_ADMIN_ALLOWED_IPS=127.0.0.1
export GATEWAY_AUTH_CACHE_TTL_SECONDS=15
export GATEWAY_LOGIN_RATE_LIMIT_PER_MINUTE=20
export GATEWAY_ADVANCED_ROUTE_POLICIES_ENABLED=false
export GATEWAY_ADMIN_PERMISSION_CHECK_ENABLED=false
export GATEWAY_PERMISSION_CACHE_ENABLED=false
export GATEWAY_PERMISSION_CACHE_TTL_SECONDS=300
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379

./gradlew run
```

기본값 `GATEWAY_ADVANCED_ROUTE_POLICIES_ENABLED=false` 에서는 `ADMIN` 경로도 `PROTECTED`와 동일하게 인증만 적용합니다.

## trusted header

외부에서 들어온 아래 헤더는 제거 후 게이트웨이가 재주입합니다.

- `X-User-Id`
- `X-User-Role`
- `X-Session-Id`
- `X-Request-Id`
- `X-Correlation-Id`

## Redis

현재 게이트웨이는 중앙 Redis 서버 1대를 기준으로 설정합니다.

- Redis 서버 레포지토리: `https://github.com/jho951/redis-server.git`
- 게이트웨이는 `REDIS_HOST`, `REDIS_PORT`, `REDIS_TIMEOUT_MS`로 접속합니다.
- Permission 검증이 비활성 상태이면 Redis 권한 캐시도 기본적으로 비활성입니다.

## 에러 응답 예시

고정된 에러 코드 계약은 [docs/Error-Codes.md](/Users/jhons/Downloads/BE/Api-gateway-server/docs/Error-Codes.md)를 따른다.

```json
{
  "httpStatus": 401,
  "success": false,
  "message": "인증이 필요합니다.",
  "code": 9101,
  "data": null
}
```
