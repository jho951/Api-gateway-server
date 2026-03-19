# Api-gateway-server

내부 서비스를 외부 단일 진입점으로 연결하는 API Gateway 서버입니다.


### 현재 구조

- Gateway는 외부 요청의 단일 진입점입니다.
- 외부 사용자가 보는 흐름은 `public`, `protected` 두 가지가 중심입니다.
- `/auth`, `/users`, `/blocks` 같은 경로를 각 내부 서비스로 전달합니다.
- 보호 경로에서는 `Authorization` 헤더 형식을 얕게 확인한 뒤 대상 서비스로 전달합니다.
- 실제 토큰 검증과 권한 판단은 Auth Service 또는 각 도메인 서비스가 담당합니다.
- `internal` 경로는 사용자 기능 API가 아니라 Gateway와 내부 서비스 사이의 계약입니다.
- `local`과 `prod` 설정은 루트의 `.env.local`, `.env.prod`로 분리되어 있습니다.

## 세부 문서
[WIKI](https://github.com/jho951/Api-gateway-server/wiki/)

## 빠른 시작

### 요구 사항

- Java 17
- Docker, Docker Compose

### 로컬 실행

```bash
bash scripts/run.local.sh local
```

### Docker 실행

```bash
bash scripts/run.docker.sh local
```

기본 포트는 `http://localhost:8080` 입니다.

## Docker 점검 순서

Docker에서 Gateway를 올린 뒤에는 아래 순서로 확인합니다.

1. Gateway 재빌드 후 실행

```bash
bash scripts/run.docker.sh local
```

2. 헬스 체크 확인

```bash
curl -i http://localhost:8080/health
curl -i http://localhost:8080/ready
```

3. 업스트림 서비스 포트 확인

- `auth-service`: `8081`
- `user-service`: `8082`
- `block-service`: `8083`
- `permission-service`: `8084`

예:

```bash
curl -i http://localhost:8081
curl -i http://localhost:8082
```

4. 로그인 시작 경로 확인

```bash
curl -i http://localhost:8080/auth/login/github
```

여기서 `502`가 나오면 Gateway에서 업스트림 연결이 실패한 것입니다.
여기서 `403 IP_BLOCKED`가 나오면 보통 `auth-service`의 IP guard 정책 문제입니다.

5. 보호 API 호출 확인

```bash
curl -i http://localhost:8080/users/me \
  -H "Authorization: Bearer <access-token>"
```

현재 구조에서는 Gateway가 토큰을 직접 검증하지 않고 `Authorization` 헤더를 대상 서비스로 전달합니다.
다만 보호 경로에서는 아래 정도의 얕은 선검증만 수행합니다.

- `Authorization: Bearer <token>` 형식 확인
- 세그먼트 개수, base64url 문자셋, 비어 있는 payload 같은 명백한 비정상 JWT 차단
- 선택적 payload `exp` 만료 체크

토큰의 실제 신뢰 판단, 사용자 상태 확인, 권한 판단, 세션/철회 반영은 각 downstream 서비스 구현에 따라 결정됩니다.
