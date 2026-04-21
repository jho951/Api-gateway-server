# API

Gateway는 외부 공개 경로인 `/v1/**`를 받고, 내부 서비스가 이해하는 upstream 경로로 바꿔 전달합니다.

## 기준 용어

| 용어 | 의미 |
| --- | --- |
| Gateway | 외부 요청을 받고 인증, 라우팅, 프록시, 응답 정책을 적용하는 서버 |
| upstream | Gateway가 프록시로 전달하는 대상 서비스 |
| downstream | Gateway 뒤에서 실제 비즈니스 처리를 수행하는 내부 서비스 |
| RouteType | `PUBLIC`, `PROTECTED`, `ADMIN`, `INTERNAL` 라우트 구분 |
| passthrough | upstream 성공 응답의 `status`, `headers`, `body`를 Gateway가 그대로 전달하는 방식 |

## 서비스별 역할

| 서비스 | 책임 | 외부 client가 아는 경로 | Gateway가 내부로 보내는 경로 |
| --- | --- | --- | --- |
| gateway-service | public route 소유, 인증 선검사, 라우팅, trusted header 주입 | `/v1/**` | 각 서비스 upstream 경로 |
| auth-service | 로그인, 토큰, 세션 검증, SSO | `/v1/auth/**`, `/v1/login/oauth2/**` | `/auth/**`, `/login/oauth2/**` |
| user-service | 회원가입, 내 정보, 내부 사용자 생성/조회 | `/v1/users/**`, `/v1/internal/users/**` | `/users/**`, `/internal/users/**` |
| authz-service | 관리자 권한 판정 | 직접 노출 없음 | `/permissions/internal/admin/verify` |

## RouteType

| RouteType | 인증 필요 | 권한 검증 필요 | 예시 | 처리 방식 |
| --- | --- | --- | --- | --- |
| `PUBLIC` | 아니오 | 아니오 | `POST /v1/auth/login`, `POST /v1/users/signup` | 바로 upstream으로 프록시 |
| `PROTECTED` | 예 | 아니오 | `GET /v1/users/me`, `GET /v1/documents/**` | auth-service 검증 후 upstream으로 프록시 |
| `ADMIN` | 예 | 예 | `/v1/admin/**` | auth-service 검증 후 authz-service 권한 검증, 통과하면 upstream으로 프록시 |
| `INTERNAL` | 내부 호출만 | 필요 시 서비스가 판단 | `POST /v1/internal/users/find-or-create-and-link-social` | 내부 접근 허용 정책 통과 시 upstream으로 프록시 |

## Auth API

| Public route | Upstream route | RouteType | 설명 |
| --- | --- | --- | --- |
| `POST /v1/auth/login` | `POST /auth/login` | `PUBLIC` | 아이디/비밀번호 로그인 |
| `GET /v1/auth/login/github` | `GET /auth/login/github` | `PUBLIC` | GitHub SSO alias |
| `POST /v1/auth/refresh` | `POST /auth/refresh` | `PUBLIC` | refresh token으로 access token 재발급 |
| `POST /v1/auth/logout` | `POST /auth/logout` | `PUBLIC` | 로그아웃 및 쿠키 정리 |
| `GET /v1/auth/sso/start` | `GET /auth/sso/start` | `PUBLIC` | SSO 시작 |
| `GET /v1/auth/oauth2/authorize/github` | `GET /auth/oauth2/authorize/github` | `PUBLIC` | GitHub OAuth 시작 |
| `POST /v1/auth/exchange` | `POST /auth/exchange` | `PUBLIC` | SSO ticket 교환 |
| `GET /v1/auth/me` | `GET /auth/me` | `PUBLIC` | auth-service의 현재 인증 요약 |
| `GET /v1/auth/session` | `GET /auth/session` | `PUBLIC` | session 조회 alias |
| `/v1/auth/internal/**` | `/auth/internal/**` | 현재 구현상 `PUBLIC` | auth-service 내부 검증 계열입니다. 운영 노출 정책을 별도로 확인해야 합니다. |

## User API

| Public route | Upstream route | RouteType | 설명 |
| --- | --- | --- | --- |
| `POST /v1/users/signup` | `POST /users/signup` | `PUBLIC` | 이메일 기반 회원가입 |
| `GET /v1/users/me` | `GET /users/me` | `PROTECTED` | Gateway 인증 후 user-service에서 사용자 상세 조회 |
| `POST /v1/internal/users/find-or-create-and-link-social` | `POST /internal/users/find-or-create-and-link-social` | `INTERNAL` | SSO 연동 중 사용자 찾기, 생성, 소셜 연결 |
| `/v1/internal/users/**` | `/internal/users/**` | `INTERNAL` | user-service 내부 사용자 API |

## Authz API

authz-service는 일반 client가 직접 호출하는 public API가 아닙니다. Gateway가 관리자 경로를 처리할 때 내부로 호출합니다.

| 호출 주체 | Internal route | 설명 |
| --- | --- | --- |
| Gateway | `POST /permissions/internal/admin/verify` | 관리자 경로 요청을 허용할지 최종 판정 |

관리자 경로 처리 순서:

1. 사용자의 Bearer token 또는 cookie session을 auth-service로 검증합니다.
2. 인증 결과에서 `userId`, `sessionId`를 얻습니다.
3. authz-service의 `/permissions/internal/admin/verify`를 호출합니다. 운영 기준으로 `Authorization: Bearer <internal-service-jwt>`와 `X-Internal-Request-Secret`를 함께 전달합니다.
4. authz-service가 `200`을 반환하면 요청을 계속 진행합니다.
5. `403`, `400`, 네트워크 오류, timeout이면 Gateway는 fail-closed로 거부합니다.

## 응답 정책

성공 응답은 upstream 응답을 그대로 전달합니다. Gateway가 성공 JSON envelope를 다시 만들지 않습니다.

실패 응답은 `GatewayErrorCode`와 `GatewayErrorResponse`로 생성합니다. 실패 JSON에는 `code`, `message`, `path`, `requestId`가 포함됩니다.

## Gateway 실패 코드

아래 표는 현재 구현의 `GatewayErrorCode` enum 기준입니다.

| HTTP | Code | 이름 | 의미 |
| --- | --- | --- | --- |
| `400` | `1000` | `INVALID_REQUEST` | 요청 형식이나 파라미터가 잘못된 경우 |
| `400` | `1001` | `INVALID_REQUEST_CHANNEL` | 요청 채널을 판정할 수 없는 경우 |
| `400` | `1002` | `INVALID_CLIENT_TYPE` | 지원하지 않는 클라이언트 타입인 경우 |
| `401` | `1003` | `MISSING_AUTH_CREDENTIALS` | 인증 정보가 없거나 현재 채널에서 쓸 수 있는 인증 수단이 없는 경우 |
| `401` | `1004` | `AUTH_CHANNEL_MISMATCH` | 클라이언트 채널과 인증 수단이 맞지 않는 경우 |
| `401` | `1005` | `UNAUTHORIZED` | 인증 시도는 했지만 검증에 실패한 경우 |
| `403` | `1006` | `FORBIDDEN` | 접근이 허용되지 않는 경우 |
| `404` | `1007` | `NOT_FOUND` | 요청 경로를 찾을 수 없는 경우 |
| `405` | `1008` | `METHOD_NOT_ALLOWED` | 허용되지 않은 HTTP method인 경우 |
| `413` | `1009` | `PAYLOAD_TOO_LARGE` | 요청 본문이 허용 크기를 초과한 경우 |
| `429` | `1010` | `TOO_MANY_REQUESTS` | 요청이 너무 많은 경우 |
| `500` | `1011` | `INTERNAL_ERROR` | Gateway 내부 처리 중 오류가 발생한 경우 |
| `502` | `1012` | `UPSTREAM_FAILURE` | upstream 연결 실패, DNS 실패, 연결 거부 등 |
| `504` | `1013` | `UPSTREAM_TIMEOUT` | upstream 응답 시간이 초과된 경우 |

운영에서 자주 보는 항목은 아래처럼 해석하면 됩니다.

- `1005`: 인증 검증 실패입니다. token, cookie, auth-service 응답을 먼저 확인합니다.
- `1006`: Gateway 정책 거부이거나 authz-service의 관리자 권한 거부입니다.
- `1012`: upstream 자체가 죽었거나 URL, DNS, 포트가 잘못된 경우가 많습니다.
- `1013`: upstream 지연이나 timeout 설정 부족일 가능성이 큽니다.

## 현재 확인된 정합성 주의 사항

| 항목 | 현재 상태 | 결정 필요 |
| --- | --- | --- |
| `/v1/permissions/**` | shared routing 계약에는 Authz route로 표현되어 있지만 Gateway 구현은 직접 프록시하지 않습니다. | public/internal route로 열지, 문서에서 관리자 위임 흐름만 남길지 결정 |
| contract lock commit | gateway, auth-service, user-service, authz-service의 lock commit이 서로 다릅니다. | 같은 contract commit 기준으로 맞출지 결정 |
