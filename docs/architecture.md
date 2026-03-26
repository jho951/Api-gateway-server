# v1 기준 API Gateway와 BFF 설계 방향

## 결론

v1 기준의 `api-gateway`는 **BFF 성질을 강하게 가져가면 안 된다.**  
현재 구조에서는 `api-gateway`를 **Edge Gateway** 로 두고, 추후 필요 시 **별도 BFF를 추가**하는 방향이 가장 안정적이다.

즉, 현재 권장 방향은 다음과 같다.

- `api-gateway = Edge Gateway`
- `BFF 역할 = 없음`
- 추후 `Web / App / Desktop` 요구사항이 분화되면 `BFF`를 별도 계층으로 분리

---

## 왜 v1에서 BFF 성질을 강하게 가져가면 안 되는가

### 1. 현재 v1의 핵심 과제는 플랫폼 입구 정리다

현재 v1의 1순위는 다음이다.

- 중앙 SSO 구축
- 여러 서비스가 하나의 인증 체계를 공유
- MSA 구조의 책임 분리
- Web뿐 아니라 App / Desktop 확장성 확보

즉, 지금 `gateway-server`의 핵심은 다음이다.

- 외부 요청 진입점 통일
- 인증 경계 설정
- 서비스 라우팅
- 내부 전달 규격 통일

반면 BFF는 보통 다음 문제를 해결할 때 등장한다.

- 화면 하나를 위해 여러 서비스 응답을 조합해야 할 때
- 모바일/웹/데스크톱이 서로 다른 응답 포맷을 원할 때
- 클라이언트별 API shape 최적화가 필요할 때
- 프론트에서 너무 많은 네트워크 호출이 발생할 때

즉, **BFF는 클라이언트 최적화 문제를 해결하는 계층**이고,  
현재 v1의 핵심 문제는 **인증과 서비스 경계 정리**다.

---

### 2. Gateway에 BFF 성질을 넣기 시작하면 빠르게 비대해진다

처음에는 gateway에 아래 역할만 둔다.

- 인증 체크
- 라우팅
- 공통 헤더 처리

하지만 여기에 BFF 성질이 들어가면 곧 다음 요구가 붙는다.

- 사용자 홈 화면용 응답 조합
- 에디터 초기 로딩 데이터 조합
- 그림판 대시보드용 custom response
- Web 전용 필드
- Mobile 전용 필드
- Desktop 전용 필드

이렇게 되면 gateway는 더 이상 단순한 게이트웨이가 아니라,  
**클라이언트 화면 로직을 아는 서버**가 된다.

이 프로젝트는 앞으로 다음으로 확장될 수 있다.

- editor
- paint
- 기타 협업 서비스
- web / app / desktop

이 구조에서 gateway가 BFF 역할까지 맡기 시작하면,  
결국 모든 화면 요구사항이 gateway에 몰리게 된다.

예를 들어 다음과 같은 상황이 반복된다.

- 인증 규칙 변경 → gateway 수정
- 라우팅 변경 → gateway 수정
- 모바일 응답 수정 → gateway 수정
- 웹 전용 조합 추가 → gateway 수정
- 새 서비스 연결 → gateway 수정

즉, **변경 집중점이 지나치게 커진다.**

---

### 3. SSO 중심 구조에서는 Gateway를 보안 경계로 유지하는 것이 더 중요하다

이 프로젝트는 일반 CRUD 앱이 아니라,  
**중앙 SSO 기반 멀티서비스 플랫폼**이다.

이 구조에서 gateway가 먼저 가져야 하는 성격은 BFF가 아니라  
**보안 경계(Edge Security Boundary)** 이다.

즉 gateway는 우선 아래에 집중해야 한다.

- 공개/보호 API 분리
- 토큰/세션 전달 규칙 통일
- 내부 헤더 스푸핑 방지
- auth-server와의 검증 계약
- 요청 추적
- 클라이언트 타입별 인증 전달 차이 흡수

이 역할이 먼저 안정되어야 한다.  
여기에 클라이언트 전용 화면 응답 조합까지 섞으면 책임이 모호해진다.

---

## 그렇다면 BFF는 아예 필요 없는가

아니다.  
추후에는 충분히 필요해질 가능성이 높다.

중요한 것은 다음 두 문장을 구분하는 것이다.

- **BFF가 필요해질 수 있다**
- **지금 gateway가 곧바로 BFF여야 한다**

이 둘은 다르다.

---

## BFF가 필요해지는 시점

다음 조건이 강해지면 BFF를 고려하면 된다.

### 1. 클라이언트별 요구 응답이 명확히 달라질 때
예시:

- 웹은 풍부한 메타데이터 필요
- 모바일은 가벼운 응답 필요
- 데스크톱은 동기화 상태 정보가 더 필요

### 2. 화면 하나를 위해 여러 서비스 응답을 합쳐야 할 때
예시:

- 에디터 홈 진입 시  
  `user + document summary + permission + recent activity` 를 한 번에 내려줘야 하는 경우

### 3. 프론트가 내부 서비스 구조를 너무 많이 알아야 할 때
예시:

- 프론트가 `user-server`, `block-server`, 추후 `paint-server`를 각각 직접 호출해야 하는 경우

### 4. 클라이언트별 release cycle이 다를 때
예시:

- 모바일은 앱 심사 때문에 응답 계약 안정성이 더 중요
- 웹은 빠르게 변경 가능
- 데스크톱은 별도 기능 흐름이 필요

이 시점부터는 별도의 BFF가 자연스럽다.

---

## 가장 추천하는 구조 패턴

### v1
- `API Gateway = Edge Gateway`
- 클라이언트는 gateway를 통해 각 서비스 접근
- gateway는 인증 / 라우팅 / 공통 정책만 담당
- 도메인 조합은 각 서비스 책임 또는 최소화

### v2 이후
필요 시 아래 계층을 추가한다.

- `web-bff`
- `mobile-bff`
- `desktop-bff`

---

## 구조 예시

### 형태 1. Gateway 중심, 이후 BFF 추가

```text
[Web] -----> [web-bff] ------\
                              \
[Mobile] --> [mobile-bff] -----> [api-gateway] -> internal services
                              /
[Desktop] -> [desktop-bff] --/
```

### 형태 2. 초기에 Edge Gateway만 운영

```text
[Web / Mobile / Desktop]
            |
            v
      [api-gateway]
            |
            v
   [auth / user / block / ...]
```

v1에서는 **형태 2**가 더 적절하다.

---

## 그럼 v1 Gateway는 어디까지 해도 되는가

### 해도 되는 것
- 인증 상태 확인
- 사용자 식별 컨텍스트 전달
- 라우팅
- 공통 응답 형식 정리
- 에러 포맷 통일
- 공통 헤더 처리
- request id / trace id 부여
- CORS / 보안 정책
- 간단한 health / metadata endpoint
- 극히 얕은 수준의 endpoint aliasing

예:

- 외부: `/api/me`
- 내부: `user-server /users/me`

이 정도는 허용 가능하다.

---

### 하면 안 되는 것
- 홈 화면용 종합 응답 조합
- 화면 전용 DTO 재가공
- 모바일 전용 API shape 분기
- 웹/앱/데스크톱별 비즈니스 오케스트레이션
- `editor + user + permission` 응답 대조합
- 특정 화면 흐름을 아는 로직
- 서비스 도메인 규칙 판단

이 영역은 **BFF 성질이 강한 책임**이다.

---

## 현재 프로젝트 기준 판단

현재 v1 서비스 구성은 다음과 같다.

- `gateway-server`
- `auth-server`
- `user-server`
- `block-server`
- `redis-server`

이 단계에서는 아직 다음 특징이 강하다.

- 서비스 수가 초기 단계
- 인증 구조 안정화가 가장 중요
- 도메인 분리 자체가 1순위
- 화면 종류가 아직 충분히 폭발하지 않음

따라서 지금 BFF를 미리 넣는 것은 얻는 이점보다  
**복잡도와 책임 혼선이 더 크다.**

즉 현재 판단은 다음과 같다.

- **No**: v1 `api-gateway`를 BFF 중심으로 설계하지 않는다
- **Yes**: 나중에 BFF를 추가할 수 있도록 확장 가능한 gateway로 설계한다

---

## 설계 원칙

### 원칙 1
**Gateway는 인증 경계와 진입점 통제에 집중한다.**

### 원칙 2
**클라이언트 맞춤 응답 조합은 나중에 별도 BFF로 분리한다.**

### 원칙 3
**v1 gateway는 BFF를 포함하지 않고, BFF를 수용할 수 있는 구조로 설계한다.**

이 세 가지가 핵심 원칙이다.

---

## 문서용 권장 서술

다음 문장은 설계 문서에 그대로 넣어도 된다.

> v1의 `api-gateway`는 인증, 라우팅, 공통 보안 정책, 내부 사용자 컨텍스트 전달을 담당하는 Edge Gateway로 설계한다.  
> 클라이언트별 화면 최적화와 다중 서비스 응답 조합은 gateway의 기본 책임에 포함하지 않으며, 추후 Web / App / Desktop 요구사항이 분화될 경우 별도의 BFF 계층으로 분리한다.

---

## 최종 결론

v1 기준 `api-gateway`가 **BFF 성질을 강하게 가져가는 것은 추천하지 않는다.**

정리하면 다음과 같다.

- 지금은 **Edge Gateway로 설계**
- 나중에 필요하면 **BFF를 분리**
- 단, 추후 BFF를 붙일 수 있도록 **경계와 책임은 처음부터 명확하게 유지**

즉,

> **v1 gateway는 BFF가 아니라, BFF를 나중에 얹을 수 있는 안정적인 인증·라우팅 게이트웨이여야 한다.**

---

## 세션/토큰 검증 캐싱 전략

API Gateway는 모든 요청마다 auth-server를 호출하지 않고도 JWT/세션 상태를 빠르게 확인하기 위해 **다단계 캐시**를 둡니다.

- **L1 (Local)**: 인스턴스 내부에 3초 이하의 짧은 TTL로 인증 결과를 캐시해서 동일 토큰이 들어오는 연속 요청을 거의 즉시 처리합니다. `GATEWAY_SESSION_LOCAL_CACHE_TTL_SECONDS`로 조정할 수 있습니다.
- **L2 (Redis)**: L1 미스일 때 `redis`의 중앙 캐시를 조회해서 세션을 확인하며, 결과가 있으면 다시 L1에 채워 넣습니다. L2 TTL과 키 프리픽스는 `GATEWAY_SESSION_CACHE_TTL_SECONDS`, `GATEWAY_SESSION_CACHE_KEY_PREFIX`로 조정 가능합니다. `GATEWAY_SESSION_CACHE_ENABLED=false`로 Redis를 끌 수도 있습니다.
- **Origin (Auth/DB)**: L2도 실패할 때만 auth-server의 `/session/validate` 엔드포인트를 호출하고, 성공한 세션은 두 캐시에 다시 넣어 재사용합니다.

이 흐름은 auth-server에 대한 반복적인 부하를 피하면서 신뢰할 수 있는 검증을 유지하고, `auth-server`가 인증 책임을 계속 지도록 하면서도 gateway가 실제 요청 진입점에서 빠르게 응답할 수 있도록 돕습니다.
