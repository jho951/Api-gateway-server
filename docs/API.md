## API

## Gateway Local API

현재 기본 운영 모드는 `public / protected` 두 역할만 사용한다.  
`admin / internal` 타입 코드는 유지하지만, `admin`은 기본적으로 `protected`처럼 동작한다.

| Method | Path | Purpose | Auth |
|---|---|---|---|
| `GET` | `/health` | liveness | No |
| `GET` | `/ready` | readiness | No |

## Public API

| Method | Path | Upstream | Notes |
|---|---|---|---|
| `GET` | `/auth/login/github` | Auth Service | login start, rate limit 적용 |
| `GET` | `/auth/oauth/github/callback` | Auth Service | OAuth2 callback |

## Protected API

| Path Pattern | Upstream | Policy |
|---|---|---|
| `/auth/session` | Auth Service | current session |
| `/users/me` | User Service | protected |
| `/documents/**` | Document Service | protected |
| `/blocks/**` | Block Service | protected, short auth cache allowed |
| `/permissions/**` | Permission Service | protected |

### Request

| Method | Path |
|---|---|
| `POST` | `/permissions/internal/admin/verify` |

### Forwarded Headers

- `X-Original-Method`
- `X-Original-Path`
- `X-User-Id`
- `X-User-Role`
- `X-Session-Id`
- `X-Request-Id`
- `X-Correlation-Id`

## Gateway -> Redis

현재 기본 모드에서는 사용하지 않는다.

- `GATEWAY_ADVANCED_ROUTE_POLICIES_ENABLED=false`
- `GATEWAY_ADMIN_PERMISSION_CHECK_ENABLED=false`

관리자 권한 검증을 다시 활성화하면 아래 캐시 계약을 사용한다.

기본 TTL:

- `GATEWAY_PERMISSION_CACHE_TTL_SECONDS`

기본 키 형식:

```text
gateway:admin-permission:{userId}|{role}|{sessionId}|{method}|{path}
```

## Trusted Headers To Downstream

### 게이트웨이는 외부 요청의 동일 이름 헤더를 제거한 뒤 아래 값을 재주입한다.

- `X-User-Id`
- `X-User-Role`
- `X-Session-Id`
- `X-Request-Id`
- `X-Correlation-Id`
