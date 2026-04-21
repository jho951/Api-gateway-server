# gateway-service Docs

이 디렉토리는 gateway-service의 API, 구조, 운영, 구현 기준을 분리해서 둔다.

## 문서 목록

- [API 분류](./api.md)
- [아키텍처](./architecture.md)
- [CI와 구현 기준](./ci-and-implementation.md)
- [계약 변경 절차](./contract-change-workflow.md)
- [데이터 저장소와 캐시](./database.md)
- [Docker 실행](./docker.md)
- [플랫폼 정책](./platform.md)
- [트러블슈팅](./troubleshooting.md)
- [Authz upstream OpenAPI](./openapi/authz-service.upstream.v1.yaml)

## 코드 모듈

- `app`: Spring Boot 실행 모듈
- `common`: 계약, 설정, 보안, 캐시, 라우팅 공용 모듈

## 추천 읽기 순서

처음 보는 실무자는 아래 순서가 가장 빠릅니다.

1. [아키텍처](./architecture.md)
2. [API 분류](./api.md)
3. [플랫폼 정책](./platform.md)
4. [Docker 실행](./docker.md)
5. [트러블슈팅](./troubleshooting.md)
