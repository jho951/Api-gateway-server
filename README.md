# Api Gateway Server

API Gateway는 외부 요청의 단일 진입점입니다.

## 문서

- 문서 인덱스: [docs/README.md](docs/README.md)
- API 분류: [docs/api.md](docs/api.md)
- 아키텍처: [docs/architecture.md](docs/architecture.md)
- 실행과 Docker: [docs/docker.md](docs/docker.md)
- 계약 변경 절차: [docs/contract-change-workflow.md](docs/contract-change-workflow.md)

## 빠른 실행

```bash
bash scripts/run.docker.sh dev
```

로컬 JVM 실행:

```bash
bash scripts/run.local.sh
```

## 모듈 구조

- `app`: Spring Boot + Spring Cloud Gateway 런타임
- `common`: 계약, 설정, 보안, 캐시, 라우팅, 공용 도메인 코드

## 기동 확인

```bash
curl -i http://localhost:8080/health
curl -i http://localhost:8080/ready
```
