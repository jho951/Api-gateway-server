# CI/CD

이 문서는 현재 이 프로젝트에 설정된 CI/CD 구조를 설명합니다.

기준 파일:

- [ci.yml](/Users/jhons/Downloads/BE/Api-gateway-server/.github/workflows/ci.yml)
- [cd.yml](/Users/jhons/Downloads/BE/Api-gateway-server/.github/workflows/cd.yml)

## 현재 구조 요약

- CI: 코드가 빌드 가능한지 확인
- CD: Docker 이미지를 GHCR로 publish

즉 현재는 "빌드 검증 + 이미지 배포"까지 자동화되어 있고, 실제 서버 반영까지는 포함되어 있지 않습니다.

## 1. CI

CI는 코드 변경이 들어왔을 때 기본 빌드 상태를 확인하는 단계입니다.

### 실행 조건

- `main` 브랜치 push
- `develop` 브랜치 push
- `feature/**` 브랜치 push
- `fix/**` 브랜치 push
- `refactor/**` 브랜치 push
- `chore/**` 브랜치 push
- 모든 pull request

### 하는 일

1. 레포 checkout
2. JDK 17 설정
3. Gradle 실행 권한 부여
4. `compileJava` 수행
5. `installDist` 수행
6. Docker 이미지 빌드 확인

### 목적

- Java 코드가 컴파일되는지 확인
- 배포 산출물이 만들어지는지 확인
- Dockerfile 기준 이미지 빌드가 깨지지 않았는지 확인

## 2. CD

CD는 빌드된 애플리케이션 이미지를 레지스트리에 publish 하는 단계입니다.

### 실행 조건

- `main` 브랜치 push
- `v*` 태그 push
- 수동 실행 (`workflow_dispatch`)

### 하는 일

1. 레포 checkout
2. Docker Buildx 설정
3. GHCR 로그인
4. Docker 이미지 메타데이터 생성
5. Docker 이미지 빌드 및 push

### publish 대상

- Registry: `ghcr.io`
- Image name: `${{ github.repository }}`

예시:

```text
ghcr.io/<owner>/<repo>:latest
ghcr.io/<owner>/<repo>:main
ghcr.io/<owner>/<repo>:v1.0.0
ghcr.io/<owner>/<repo>:sha-...
```

## 3. 현재 CD가 하지 않는 것

현재 CD는 다음 단계까지는 하지 않습니다.

- EC2, VM, 온프레미스 서버 SSH 배포
- Docker Compose 서버 재기동
- Kubernetes rollout
- ECS/EKS 배포

즉 "이미지 publish"까지만 자동화된 상태입니다.

## 4. 왜 이렇게 구성했는가

현재 프로젝트에는 배포 서버 정보나 배포 플랫폼 정보가 없기 때문에, 가장 안전한 기본형으로 구성했습니다.

- CI는 코드/이미지 빌드 검증
- CD는 이미지 publish

이렇게 두면 이후 배포 환경이 정해졌을 때 CD 뒤에 실제 배포 job만 추가하면 됩니다.

## 5. 실제 서버 배포를 붙이려면 필요한 것

다음 중 하나가 필요합니다.

- SSH 접속 가능한 서버 정보
- Docker Compose 기반 운영 서버
- Kubernetes/ECS 같은 배포 플랫폼 정보
- 배포용 secret

예:

- `SSH_HOST`
- `SSH_USER`
- `SSH_PRIVATE_KEY`
- 운영 서버의 `.env`
- 운영용 이미지 태그 전략

## 6. 추천 다음 단계

현재 상태에서 추천 순서는 다음입니다.

1. GHCR 이미지가 정상 push 되는지 확인
2. 운영 서버에서 해당 이미지를 pull 할 수 있게 준비
3. 배포 방식 결정
4. 그 방식에 맞는 deploy job 추가

## 7. 관련 문서

- [Runtime Profiles](/Users/jhons/Downloads/BE/Api-gateway-server/docs/Runtime-Profiles.md)
- [Proxy Rules](/Users/jhons/Downloads/BE/Api-gateway-server/docs/Proxy-Rules.md)
