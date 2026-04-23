# Platform

## 인증 정책

### 브라우저

- 브라우저는 쿠키 기반을 우선합니다.
- `ACCESS_TOKEN` 또는 `sso_session` 쿠키를 Gateway로 보냅니다.
- 브라우저 요청은 `credentials: 'include'`가 필요합니다.

### 비브라우저

- 모바일, CLI, 서버 간 호출은 `Authorization: Bearer <token>`을 우선합니다.

## 내부 정규화

Gateway는 인증 성공 후 내부 서비스에 검증된 context를 주입합니다.

| 헤더 | 설명 |
| --- | --- |
| `X-User-Id` | 인증된 사용자 ID |
| `X-Client-Type` | Gateway가 판정한 client channel |
| `X-Request-Id` | 단일 요청 추적 ID |
| `X-Correlation-Id` | 서비스 간 추적 ID |

외부에서 들어온 `X-User-*` 계열 trusted header는 신뢰하지 않습니다. 내부 서비스는 Gateway가 주입한 컨텍스트만 신뢰합니다.

## Authz 내부 호출 헤더

Gateway가 authz-service `POST /permissions/internal/admin/verify`를 호출할 때 전달하는 주요 헤더입니다.

| 헤더 | 값 |
| --- | --- |
| `X-User-Id` | 인증된 사용자 ID |
| `X-Session-Id` | 인증 세션 ID |
| `X-Original-Method` | client가 Gateway에 보낸 원본 HTTP method |
| `X-Original-Path` | client가 Gateway에 보낸 원본 path |
| `X-Request-Id` | 단일 요청 추적 ID |
| `X-Correlation-Id` | 서비스 간 추적 ID |
| `Authorization` | authz-service 운영 JWT internal auth용 `Bearer <internal-service-jwt>` |
| `X-Internal-Request-Secret` | local/test compat가 켜진 경우에만 전달하는 legacy secret |

## 권한 정책

- `GATEWAY_AUTHZ_CACHE_ENABLED`가 켜져 있으면 관리자 경로 판정 결과를 짧게 캐시할 수 있습니다.
- 캐시의 기본 prefix는 `gateway:admin-authz:` 입니다.
- authz-service가 응답하지 않으면 `ADMIN` 경로는 fail-closed로 거부됩니다.

## 플랫폼 보안 연동

- 현재 공식 모드는 `Hybrid Embedded Gateway Mode` 입니다.
- Gateway는 auth-service 검증 결과를 바탕으로 `platform-security`가 판정하도록 연결되어 있습니다.
- `PUBLIC`, `PROTECTED`, `ADMIN`, `INTERNAL` 분류는 Gateway route 기준으로 platform-security에 전달됩니다.
- credential 검증은 `GatewayPlatformSecurityWebFilter`가 수행하고, 그 결과를 `HybridSecurityRuntime`과 additive `SecurityPolicy` 흐름으로 넘깁니다.
- IP 허용, login rate-limit, admin authz 위임도 platform policy 흐름 안에서 처리됩니다.
- Gateway 본체는 허용된 요청을 upstream으로 전달하는 집행자 역할만 남깁니다.
- `platform-security-governance-bridge`는 security verdict를 governance audit recorder로 발행합니다.
- 관리자 최종 인가는 authz-service `/permissions/internal/admin/verify`가 결정하지만, 호출 위치는 Gateway 고유 `SecurityPolicy` bean 안의 platform policy 흐름입니다.
- security audit와 operational audit는 분리합니다. security audit는 platform bridge가, upstream/proxy audit는 gateway operational audit port가 담당합니다.
- gateway는 `platform-security-hybrid-web-adapter`를 sanctioned add-on으로 사용합니다. `GatewayApplication`은 hybrid auto-config만 exclude 하고, gateway 고유 filter chain과 `GatewayPlatformSecurityConfiguration`이 `HybridSecurityRuntime`을 조립합니다.

## 운영 권장값

| 환경변수 | 운영 기준 |
| --- | --- |
| `AUTH_JWT_VERIFY_ENABLED` | `true` |
| `AUTH_JWT_SHARED_SECRET` 또는 `AUTH_JWT_PUBLIC_KEY_PEM` | auth-service 토큰 검증 키와 일치 |
| `AUTHZ_ADMIN_VERIFY_URL` | authz-service의 `/permissions/internal/admin/verify` 전체 URL |
| `GATEWAY_INTERNAL_REQUEST_SECRET` | authz-service legacy/hybrid 내부 secret과 일치 |
| `AUTHZ_INTERNAL_JWT_SECRET` | authz-service 운영 JWT internal auth secret과 일치 |
| `AUTHZ_INTERNAL_JWT_ISSUER` | authz-service 운영 JWT issuer와 일치 |
| `AUTHZ_INTERNAL_JWT_AUDIENCE` | authz-service 운영 JWT audience와 일치 |
