# CI and Implementation

## 구현 기준

변경할 때는 아래 순서로 확인합니다.

1. Gateway public route가 `AuthApiPaths`, `UserApiPaths`, `DocumentApiPaths` 중 어디에 속하는지 확인합니다.
2. `GatewayConfig.buildRoutes`에 RouteType과 upstream base URL이 맞게 등록되어 있는지 확인합니다.
3. `/v1` prefix 제거 후 실제 upstream service가 받는 경로와 일치하는지 확인합니다.
4. 인증이 필요한 경로라면 `PROTECTED` 또는 `ADMIN`인지 확인합니다.
5. 관리자 경로라면 authz-service `POST /permissions/internal/admin/verify` 호출에 필요한 헤더가 모두 전달되는지 확인합니다.
6. 변경이 외부에서 관측되는 API, header, status, env를 바꾸면 service-contract와 `contract.lock.yml`을 함께 갱신합니다.

## 로컬 검증

```bash
./gradlew clean test
```

빠른 검증:

```bash
./gradlew test
```

## CI 기준

`contract.lock.yml`은 이 repo가 따르는 service-contract commit을 고정합니다.

CI는 다음 항목을 확인합니다.

- contract lock 파일 형식과 service name
- 계약 영향 변경이 있을 때 lock 갱신 여부
- Java 17 기준 테스트
- Docker image build 가능 여부

## 구현 시 주의할 점

| 항목 | 기준 |
| --- | --- |
| 성공 응답 | upstream 응답을 passthrough 합니다. |
| 실패 응답 | `GatewayExceptionHandler`가 `GatewayErrorResponse`로 변환합니다. |
| 외부 trusted header | 신뢰하지 않고 제거 후 Gateway가 다시 주입합니다. |
| 인증 token | 외부 token을 downstream에 그대로 신뢰시키지 않습니다. |
| 관리자 권한 | Gateway 내부 role 판단만으로 통과시키지 않고 authz-service에 위임합니다. |
