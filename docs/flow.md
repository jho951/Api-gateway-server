```text
[Web / App / Desktop]
        |
        v
[gateway-server]
|    |    |
|    |    +--> auth-server
|    +-------> user-server
+-----------> block-server
```

### 고려사항
- 클라이언트 독립적인 인증 처리 모델로 고려하여 설계합니다.
- 얇지만 보안상 중요한 Edge Gateway여야 합니다.
- 외부 클라이언트는 내부 서비스 주소를 직접 몰라야 합니다.
- api-gateway는 단일 진입점이로 항상 gateway를 통해 들어오고, gateway가 내부 라우팅을 담당합니다.
- gateway는 “인증 판단자”, auth-server는 “인증 권위자"로 적용합니다.

### 흐름
 1. 공개 경로인지 확인해 해당 경로는 바로 통과
```text
/auth/login
/auth/oauth2/**
/health
일부 공개 문서 조회 API
```

2. 보호 경로면 인증 정보를 확인해 내부 서비스로 전달합니다.
```text
브라우저면:

쿠키 기반 세션 또는 access token 확인

앱/데스크톱이면:

Authorization: Bearer <token> 확인
형식 검증
토큰 존재 여부
헤더 형식
기본 구조 검증
auth-server와 연계하여 신뢰 검증
서명 검증
만료 여부
폐기 여부
세션/리프레시 상태
사용자 활성 상태
인증 성공 시 내부 헤더 주입

예:

X-User-Id
X-Auth-Subject
X-Client-Type
X-Request-Id
```