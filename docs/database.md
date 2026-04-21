# Database

gateway-service는 비즈니스 소유 DB를 직접 갖지 않습니다.

## 데이터 소유권

| 데이터 | 소유 서비스 | Gateway 역할 |
| --- | --- | --- |
| 로그인, 토큰, 세션 | auth-service | 세션 검증 API 호출 |
| 사용자 프로필, 상태 | user-service | 인증 context를 주입하고 응답 passthrough |
| 관리자 권한, RBAC 판정 | authz-service | 관리자 경로에서 권한 검증 API 호출 |
| 문서, 워크스페이스 | block/document service | 인증 context를 주입하고 프록시 |

## Redis 사용

Gateway는 Redis를 캐시 용도로 사용할 수 있습니다.

| 캐시 | 환경변수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| Session cache | `GATEWAY_SESSION_CACHE_ENABLED` | `true` | auth-service 세션 검증 결과를 짧게 캐시 |
| Session cache TTL | `GATEWAY_SESSION_CACHE_TTL_SECONDS` | `60` | Redis 세션 캐시 TTL |
| Local session cache TTL | `GATEWAY_SESSION_LOCAL_CACHE_TTL_SECONDS` | `3` | JVM 메모리 세션 캐시 TTL |
| Authz cache | `GATEWAY_AUTHZ_CACHE_ENABLED` | `false` | 관리자 권한 판정 결과 캐시 |
| Authz cache TTL | `GATEWAY_AUTHZ_CACHE_TTL_SECONDS` | `300` | 권한 캐시 TTL |
| Authz cache prefix | `GATEWAY_AUTHZ_CACHE_PREFIX` | `gateway:admin-authz:` | 권한 캐시 key prefix |

Redis 장애가 있어도 Gateway의 인증/인가 의미가 바뀌면 안 됩니다. 캐시 miss 또는 Redis 오류 시 원본 서비스 검증을 기준으로 판단합니다.

## `/v1/users/me` 상태 판정

비활성 사용자 여부는 user-service가 DB 상태로 판단합니다. user-service가 `403`을 반환하면 Gateway는 해당 응답을 그대로 전달합니다.
