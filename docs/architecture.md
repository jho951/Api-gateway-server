# Architecture

Gateway는 public API version prefix를 소유하고, 각 backend service는 자기 upstream path만 소유합니다.

```txt
Client
  -> Gateway public route: /v1/**
  -> Service upstream route: service-owned path
```

## 핵심 책임

| 책임 | 설명 |
| --- | --- |
| Public route ownership | 외부 client는 Gateway의 `/v1/**`만 호출합니다. |
| Platform policy execution | `platform-security`가 credential 검증 결과, boundary, IP, rate-limit, admin authz를 기준으로 판정합니다. |
| Authorization delegation | `ADMIN` route의 최종 권한 판정은 authz-service에 위임되며, 이 호출은 platform policy 안에서 수행됩니다. |
| Header normalization | 외부 trusted header를 제거하고 Gateway가 검증한 context만 내부로 주입합니다. |
| Passthrough proxy | upstream 성공 응답을 다시 감싸지 않고 그대로 전달합니다. |

## 런타임 구조

현재 Gateway의 공식 런타임 모드는 `Hybrid Embedded Gateway Mode` 입니다.

- credential 검증 소유자: gateway
- edge policy 평가 소유자: `platform-security`
- admin 최종 authz 소유자: authz-service
- downstream header 주입 / proxy 소유자: gateway
- security audit 소유자: platform governance bridge
- operational audit 소유자: gateway

현재 Gateway는 Spring Boot + Spring Cloud Gateway 기반으로 실행됩니다.

```txt
GatewayApplication
  -> Spring Boot WebFlux/Netty
  -> GatewaySpringConfiguration route 등록
  -> GatewayCommonWebFilter 공통 header/trace 처리
  -> GatewayPlatformSecurityWebFilter 정책 검증/판정
  -> GatewayPolicyFilter downstream header 주입/프록시 실행
  -> Spring Cloud Gateway upstream proxy
  -> GatewayErrorWebExceptionHandler 오류 응답 정규화
```

핵심은 여기입니다. 지금 Gateway는 starter 기본 WebFlux filter를 주 런타임으로 쓰는 서비스가 아닙니다. 플랫폼 엔진을 내장하고, Gateway 런타임이 전체 흐름을 주도하는 하이브리드 모드입니다.

- `GatewayPlatformSecurityWebFilter`가 credential 검증, boundary/IP/rate-limit/admin authz 판정을 수행합니다.
- `GatewayPolicyFilter`는 판정이 끝난 요청에만 trusted header 정리, 내부 JWT 주입, upstream proxy 실행을 합니다.
- 실패 응답은 공통 `GatewayFailureResponseFactory`와 `GatewayResponseContractWriter`를 통해 Gateway 계약으로 렌더링됩니다.

## 주요 컴포넌트

| 컴포넌트 | 역할 |
| --- | --- |
| `GatewayApplication` | Spring Boot reactive 앱을 기동하고 기본 런타임 속성을 설정합니다. |
| `GatewaySpringConfiguration` | RouteLocator, timeout, config bean을 구성합니다. |
| `GatewayPlatformSecurityConfiguration` | platform-security policy service, auth validator, authz client, governance bridge, operational audit port, failure writer를 연결합니다. |
| `GatewayCommonWebFilter` | `X-Request-Id`, `X-Correlation-Id`, 공통 보안 헤더를 정리합니다. |
| `GatewayPlatformSecurityWebFilter` | auth-service 연동, platform policy 평가, authz-service 위임, deny 응답 생성을 담당합니다. |
| `GatewayPolicyFilter` | trusted header 제거, platform downstream header 주입, 내부 JWT 주입, passthrough 프록시를 담당합니다. |
| `GatewayFailureResponseFactory` | security failure, upstream timeout, 예외를 `GatewayErrorCode` 계약으로 분류합니다. |
| `GatewayResponseContractWriter` | 공통 JSON 응답 계약을 실제 HTTP 응답으로 렌더링합니다. |
| `GatewayErrorWebExceptionHandler` | 미매칭 라우트나 처리 중 예외를 공통 response writer로 전달합니다. |

## 요청 처리 단계

처음 보는 실무자는 아래 순서로 보면 실제 동작을 이해하기 쉽습니다.

1. 요청이 `GatewayCommonWebFilter`를 지나면서 request id, correlation id, 공통 응답 헤더가 정리됩니다.
2. `GatewayPlatformSecurityWebFilter`가 경로를 보고 `PUBLIC`, `PROTECTED`, `ADMIN`, `INTERNAL` 경계를 결정합니다.
3. `PROTECTED`, `ADMIN`이면 auth-service의 `/auth/internal/session/validate`로 세션 또는 Bearer token을 검증합니다.
4. `platform-security`가 boundary, auth mode, IP, rate-limit 정책을 평가합니다.
5. `ADMIN`이면 같은 policy 흐름 안에서 authz-service `/permissions/internal/admin/verify`를 호출해 최종 권한을 판정합니다.
6. 거부되면 공통 response writer가 Gateway JSON 오류 계약으로 바로 응답합니다.
7. 허용되면 `GatewayPolicyFilter`가 외부 trusted header를 제거하고, 검증이 끝난 `X-User-Id`, platform downstream header, 내부 JWT를 주입합니다.
8. Spring Cloud Gateway가 upstream 서비스로 프록시하고, 성공 응답은 passthrough 합니다.

## 오류 처리 구조

- 비즈니스 정책 위반, 인증 실패, 인가 실패는 `GatewayPlatformSecurityWebFilter`가 판단하고, 공통 response writer가 `GatewayErrorCode` 계약으로 렌더링합니다.
- upstream 연결 실패는 `502 UPSTREAM_FAILURE`, upstream 응답 timeout은 `504 UPSTREAM_TIMEOUT`으로 변환합니다.
- 라우트 미존재 같은 기본 WebFlux 오류는 `GatewayErrorWebExceptionHandler`가 같은 response writer로 맞춥니다.

## 대표 흐름

### 로그인

```txt
Client
  -> POST /v1/auth/login
Gateway
  -> POST /auth/login
auth-service
  -> token/cookie 응답
Gateway
  -> upstream 응답 passthrough
```

로그인 API는 인증을 시작하는 진입점이므로 Gateway 인증 선검사를 하지 않습니다.

### 내 사용자 정보 조회

```txt
Client
  -> GET /v1/users/me
Gateway
  -> POST /auth/internal/session/validate
auth-service
  -> authenticated user context
Gateway
  -> GET /users/me
user-service
  -> 사용자 상세 응답
Gateway
  -> upstream 응답 passthrough
```

비활성 사용자 여부는 user-service가 DB 상태 기준으로 판단합니다. user-service가 `403`을 반환하면 Gateway는 그대로 전달합니다.

### 관리자 경로

```txt
Client
  -> /v1/admin/**
Gateway
  -> POST /auth/internal/session/validate
auth-service
  -> authenticated user context
Gateway
  -> POST /permissions/internal/admin/verify
authz-service
  -> 200 또는 403
Gateway
  -> 허용 시 upstream 호출, 거부 시 403
```

관리자 경로는 인증만으로 통과하지 않습니다. authz-service의 판정이 최종 기준입니다.

### SSO 중 사용자 생성/연결

```txt
auth-service
  -> internal call
Gateway 또는 service network
  -> POST /v1/internal/users/find-or-create-and-link-social
user-service
  -> POST /internal/users/find-or-create-and-link-social
```

Gateway를 거치면 `/v1/internal/users/...`를 받고, user-service로 전달될 때는 `/internal/users/...`가 됩니다. 서비스 네트워크에서 user-service를 직접 호출하면 `/internal/users/...`를 사용합니다.

## 주요 코드 위치

| 목적 | 파일 |
| --- | --- |
| Gateway가 받는 외부 auth 경로 | `common/src/main/java/com/gateway/contract/external/path/AuthApiPaths.java` |
| Gateway가 받는 외부 user 경로 | `common/src/main/java/com/gateway/contract/external/path/UserApiPaths.java` |
| Gateway가 내부로 호출하는 auth/authz 경로 | `common/src/main/java/com/gateway/contract/internal/path/ServicePaths.java` |
| 라우트 설정 데이터 | `common/src/main/java/com/gateway/config/GatewayConfig.java` |
| Spring Cloud Gateway route 등록 | `app/src/main/java/com/gateway/spring/GatewaySpringConfiguration.java` |
| Platform security 실행 필터 | `app/src/main/java/com/gateway/spring/GatewayPlatformSecurityWebFilter.java` |
| Gateway 실행 필터 | `app/src/main/java/com/gateway/spring/GatewayPolicyFilter.java` |
| auth-service 세션 검증 호출 | `common/src/main/java/com/gateway/auth/AuthServiceClient.java` |
| authz-service 관리자 권한 검증 호출 | `common/src/main/java/com/gateway/auth/AuthzServiceClient.java` |
