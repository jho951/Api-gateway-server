# 2026-03-24 Frontend SSO Team Request

## 목적

이 문서는 현재 `api-gateway`, `auth-service`, `user-service` 연동 기준으로 프론트 팀이 맞춰야 하는 SSO 호출 규칙을 정리한 문서입니다.

프론트는 인증, 사용자, 문서 API를 직접 개별 서비스로 호출하지 않고 반드시 gateway 진입 경로만 사용해야 합니다.

## 기본 원칙

- 프론트는 gateway 경로만 호출합니다.
- 프론트는 사용자 식별 헤더를 직접 만들지 않습니다.
- 쿠키 기반 인증이 필요한 요청은 반드시 `credentials: include` 기준으로 호출합니다.
- OAuth callback URI는 auth-service에 등록된 값과 정확히 일치해야 합니다.

## Gateway 기준 사용 경로

프론트는 아래 경로만 사용해주세요.

### Auth

- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/sso/start`
- `POST /auth/exchange`
- `GET /auth/me`
- `POST /auth/logout`

### User

- `POST /api/users/signup`
- `GET /api/users/me`

### Documents

- `/api/documents/v1/**`

## 하지 말아야 할 것

- `auth-service`, `user-service`, `documents-service` 내부 주소를 직접 호출하지 않습니다.
- 클라이언트에서 `X-User-Id`를 직접 보내지 않습니다.
- `/auth/**` 응답을 프론트에서 임의 포맷으로 가정하지 않습니다.
- OAuth callback 경로를 gateway 또는 프론트에서 임의 변경하지 않습니다.

## SSO 로그인 흐름

프론트가 따라야 하는 순서는 아래와 같습니다.

1. `GET /auth/sso/start?page=...` 또는 `GET /auth/sso/start?redirect_uri=...` 호출
2. 브라우저가 `302 Location` 을 따라 GitHub 인증으로 이동
3. 인증 완료 후 프론트 callback URI로 `ticket` 쿼리 파라미터 수신
4. 프론트가 `POST /auth/exchange` 로 `ticket` 전달
5. 성공 시 `204 No Content` 와 함께 `sso_session` 쿠키 저장
6. 이후 `GET /auth/me` 로 로그인 상태 확인

## 프론트 구현 요청사항

### 1. SSO 시작은 브라우저 이동 기준으로 처리

`GET /auth/sso/start` 는 일반 JSON API처럼 처리하지 말고, 브라우저 redirect 흐름으로 다뤄야 합니다.

권장 방식:

- `window.location.href = "/auth/sso/start?page=editor"`
- 또는 동일한 브라우저 네비게이션 방식

### 2. Callback URI는 등록값과 정확히 일치

다음 값은 auth-service 등록값과 정확히 같아야 합니다.

- scheme
- host
- port
- path

하나라도 다르면 `INVALID_REQUEST` 가 발생할 수 있습니다.

### 3. `/auth/exchange` 는 `204` 를 정상 처리

`POST /auth/exchange` 성공 시 응답 body가 없을 수 있습니다.

프론트는 아래를 정상 흐름으로 처리해야 합니다.

- HTTP `204 No Content`
- `Set-Cookie: sso_session=...`

즉 body가 비어 있어도 실패로 간주하면 안 됩니다.

### 4. 쿠키 기반 요청은 `credentials: include`

아래 요청은 쿠키가 필요할 수 있습니다.

- `POST /auth/exchange`
- `GET /auth/me`
- `POST /auth/logout`
- 필요 시 `POST /auth/refresh`

따라서 브라우저 호출은 반드시 credentials 포함 기준으로 맞춰주세요.

예시:

```ts
fetch("/auth/me", {
  method: "GET",
  credentials: "include",
});
```

axios 사용 시에도 같은 기준으로 맞춰주세요.

### 5. 로그인 상태 확인 기준

프론트의 로그인 상태 확인은 아래 기준으로 처리해주세요.

- SSO 세션 기반 상태 확인: `GET /auth/me`
- 사용자 도메인 정보 확인: 필요 시 `GET /api/users/me`

즉 인증 상태와 사용자 도메인 상세는 분리해서 보는 편이 안전합니다.

### 6. `Authorization` 과 쿠키 사용 구간 구분

프론트는 아래를 구분해서 다뤄주세요.

- ID/PW 로그인 후 access token 기반 API 호출
- SSO 로그인 후 `sso_session` 쿠키 기반 인증 상태 확인

두 흐름은 같은 앱에서 공존할 수 있으므로, 아래를 가정하지 마세요.

- 항상 access token 만 사용한다
- 항상 쿠키만 사용한다

현재 gateway/auth 구조는 두 방식을 모두 허용하는 방향입니다.

## 응답 처리 요청사항

프론트는 auth-service와 user-service의 응답을 gateway 공통 응답으로 재해석하지 말고 origin 계약 기준으로 처리해주세요.

중요 포인트:

- `302` 는 redirect 로 처리
- `204` 는 성공으로 처리
- `400`, `401`, `403` 은 origin JSON body 기준으로 처리
- 성공/실패 body는 `GlobalResponse` 또는 각 auth API 계약 기준으로 해석

## CORS / 브라우저 환경 체크

프론트 팀에서 아래를 확인해주세요.

- 최종 브라우저 진입 origin 과 gateway origin 관계
- cross-site 환경에서 `credentials: include` 가 실제로 적용되는지
- HTTPS 환경에서 `Secure` 쿠키가 저장되는지
- `SameSite=None` 쿠키가 브라우저 정책에 막히지 않는지

## 프론트 팀 회신 요청

아래 항목은 프론트 팀 확인이 필요합니다.

1. SSO 시작을 브라우저 redirect 방식으로 처리하는지
2. callback URI가 auth-service 등록값과 정확히 일치하는지
3. `POST /auth/exchange` 의 `204` 응답을 정상 성공으로 처리하는지
4. `/auth/me` 호출에 `credentials: include` 가 적용되는지
5. 클라이언트에서 `X-User-Id` 같은 사용자 식별 헤더를 직접 만들지 않는지

## 한 줄 요약

프론트는 gateway 경로만 호출하고, SSO는 `/auth/sso/start -> callback(ticket) -> /auth/exchange -> /auth/me` 흐름으로 처리해야 합니다.
쿠키가 필요한 요청은 반드시 `credentials: include` 를 사용하고, 클라이언트가 사용자 식별 헤더를 직접 만들면 안 됩니다.
