# Gateway Auth Strategy

이 문서는 인증/권한/캐시 책임이 어느 서버에 있어야 하는지와, 경로별 검사 전략을 정의한다.

## 책임 분리

### API Gateway

- 외부 요청의 단일 진입점
- 경로 분류
- 어떤 검사를 수행할지 결정
- Auth Service 호출 여부 결정
- Permission Service 호출 여부 결정
- Redis 캐시 사용 여부 결정

### Auth Service

- 세션 유효성 판단
- SSO 상태 판단
- 로그인 상태 검증
- 사용자 ID / 역할 / 세션 ID 반환

### Permission Service

- 관리자 작업 가능 여부의 최종 판단
- 세밀한 권한 정책 검증

### Redis

- 관리자 권한 확인 결과의 짧은 TTL 캐시
- 초기에는 중앙 Redis 1대를 사용
- 운영에서는 replica 구조로 확장 가능

## 핵심 원칙

인증 사실은 Auth Service가 판단하고,  
경로별 정책 적용은 Gateway가 수행한다.

즉:

- `누가 로그인했는가?` -> Auth Service
- `이 경로에 어떤 검사가 필요한가?` -> Gateway
- `이 관리 작업이 허용되는가?` -> Permission Service
- `그 권한 결과를 잠시 재사용할 것인가?` -> Gateway + Redis

## Path-Based Strategy

게이트웨이는 `GatewayApiPaths` 기준으로 경로를 선별적으로 처리한다.

### 1. PUBLIC

대상 예시:

- `/health`
- `/ready`
- `/auth/login/github`
- `/auth/oauth/github/callback`

정책:

- Auth Service 호출 생략
- Permission Service 호출 생략
- Redis 권한 캐시 사용 안 함

### 2. USERS_ME / 일반 보호 경로

대상 예시:

- `/auth/session`
- `/users/me`
- `/documents/**`
- `/blocks/**`
- `/permissions/**`

정책:

- 세션 검증 수행
- Permission Service 확인 생략
- 일반 protected route는 짧은 auth cache 허용 가능

즉:

```text
Gateway -> Auth Service
```

### 3. ADMIN

대상 예시:

- `/admin/users/**`
- `/admin/blocks/**`
- `/admin/permissions/**`

정책:

- 매 요청 세션 검증
- admin role 확인
- Permission Service 확인
- Redis 권한 캐시 사용
- admin IP allowlist 적용
- audit-ready log 기록

즉:

```text
첫 요청:
Gateway -> Auth Service
Gateway -> Permission Service
Gateway -> Redis SETEX

이후 요청:
Gateway -> Auth Service
Gateway -> Redis GET
```

주의:

- 관리자 경로는 세션 검증을 생략하지 않는다
- Redis는 Permission 결과 캐시에만 사용한다
- 세션의 source of truth는 Auth Service다

## 왜 Gateway에서 해야 하는가

이 전략은 Auth 서버가 아니라 Gateway에 있어야 한다.

이유:

1. 경로 분류는 Gateway가 가장 잘 알고 있다.
2. public/protected/admin/internal 정책은 Gateway 책임이다.
3. Permission 확인은 모든 요청이 아니라 일부 경로에만 적용해야 한다.
4. Redis 캐시도 “어떤 경로에서 캐시를 사용할지”를 아는 Gateway가 제어해야 한다.
5. Auth Service에 이 정책을 몰아넣으면 인증 서버가 정책 오케스트레이터까지 떠안게 된다.

## 잘못된 배치 예시

다음은 권장하지 않는다.

- Auth Service가 모든 경로의 권한 정책까지 판단
- Auth Service가 Permission Service 호출을 대신 수행
- Auth Service가 관리자 권한 캐시까지 소유

문제:

- Auth Service가 과도하게 비대해짐
- Gateway의 정책 계층이 사라짐
- 병목이 Auth Service로 집중됨

## 권장 흐름 요약

### Public

```text
Client -> Gateway -> Upstream
```

### Protected

```text
Client -> Gateway -> Auth Service -> Upstream
```

### Admin

```text
Client -> Gateway -> Auth Service
                 -> Redis or Permission Service
                 -> Upstream
```

## Current Decision

현재 프로젝트 기준 최종 결정:

- `PUBLIC_ALL`: 확인 생략
- `USERS_ME` 및 일반 보호 경로: 세션만 확인
- `ADMIN_ALL`: 세션 + `ADMIN_VERIFY`
- 관리자 권한 결과는 Redis에 짧게 캐시
- 이 오케스트레이션 책임은 Auth 서버가 아니라 API Gateway에 둔다
- Redis 인프라는 초기에는 중앙 단일 서버, 운영에서는 replica 구조로 확장한다
