# WhatTheBus (단국대 셔틀 실시간 위치 알림)

단국대 셔틀·시내버스 위치를 실시간으로 수집·캐시·제공하는 Spring Boot 기반 중앙 서버입니다. 라즈베리파이가 보내는 좌표를 DTLS(PSK) 또는(테스트용) HTTP로 받아 Redis에 최신 상태를 저장하고, 좌표 기록을 RDB에 누적한 뒤 React 클라이언트에 제공합니다. 시내버스 정보는 GBUS 공공 API를 주기적으로 조회해 함께 노출합니다.

## 아키텍처 한눈에 보기
### 셔틀 위치
```
라즈베리파이 ── DTLS 5684/UDP (PSK) ──> DtlsServerService
                            ↳ (테스트/백업) HTTP POST /api/pi/location
                              RaspberryPiController
            └── 검증/변환 ──> ShuttleLocationBusinessService
            └── 캐시 ──────> Redis (shuttle:location:<id>)
            └── 기록 ──────> DB LocationHistory
            └── 제공 ──────> GET /api/shuttle/**
```

### 시내버스 도착 정보
```
GBUS 공공 API ──(40초마다)──> BusArrivalService
                         └──> Redis (bus:arrivals:<stop>)
React/클라이언트 ── GET /api/bus/arrivals
```

## 주요 기능
 - 셔틀 위치 수집/캐시: DTLS(PSK)로 받은 좌표를 Redis 최신 상태 + DB 기록으로 저장(HTTP 엔드포인트는 테스트/백업용으로만 준비되어 있으며 실운영에서는 사용하지 않음).
- 셔틀 조회: 단일/전체 셔틀 최신 좌표 반환, 운행 없으면 204 + 안내 헤더.
- 시내버스 도착 정보: 죽전역·치과병원·인문관·정문 정류장의 24, 720-3 도착 예정/잔여좌석을 40초 간격으로 갱신·캐시.
- 보안 채널: DTLS PSK(5684/udp) 사용, HTTP 병행 가능.
- 레이트 리미팅(임시): Redis ZSet + Resilience4j 서킷브레이커 기반 샘플 코드가 포함되어 있으며, 실제 운영용은 추후 재구현 예정.
- 운영 가시성: `/api/pi/health`, Swagger UI(`/swagger-ui/index.html`), DTLS DEBUG 로그 옵션.
- 인증/JWT(임시 미사용): JWT 관련 의존성과 환경변수가 있으나 현재 운영 경로에서는 쓰지 않습니다. 단순 헤더(`X-API-Key`)로 대체 운용 중이며, 향후 정식 인증 체계 도입 또는 제거될 수 있습니다.

## API 요약
- POST `/api/pi/location` : 디바이스 위치 업데이트.  
  예시:
  ```json
  { "shuttleId": "SHUTTLE_01", "latitude": 37.321, "longitude": 127.126, "timestamp": 1719300000 }
  ```
- GET `/api/pi/health` : 디바이스-서버 헬스체크.
- GET `/api/shuttle/{shuttleId}/location` : 특정 셔틀 최신 좌표.
- GET `/api/shuttle/locations` : 운행 중 셔틀 좌표 배열(없으면 204).
- GET `/api/bus/arrivals` : 4개 정류장의 24, 720-3 도착 정보.
- 문서: `/swagger-ui/index.html`, `/v3/api-docs`.

## 로컬 실행
1. 사전 준비: Java 21, Redis(필수). Redis 없으면 `docker run -p 6379:6379 redis:7`.
2. 환경 설정(.env 또는 시스템 환경변수, dev 기본값은 `application-dev.properties` 참고):
   - `bus.api.service-key` (GBUS 키)
   - `SHUTTLE_LOCATION_KEY_PREFIX` (기본 `shuttle:location:`)
   - `app.dtls.enabled` (기본 true), `app.dtls.port` (기본 5684)
3. 서버 실행: `./gradlew bootRun` (dev 프로필: H2 + Redis).
4. 테스트: `./gradlew test`

## 프로덕션/배포
- 프로필: `SPRING_PROFILES_ACTIVE=prod`
- 의존: Redis, PostgreSQL.
- 주요 환경변수:
  - `DB_USERNAME`, `DB_PASSWORD`, `db.host`, `db.port`
  - `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`(옵션)
  - `JWT_SECRET`, `JWT_EXPIRATION`
  - `dtls.psk.<DEVICE_ID>` (Base64 PSK, 예: `dtls.psk.shuttle_01`)
  - `bus.api.service-key`, `SHUTTLE_LOCATION_KEY_PREFIX`, `app.cors.allowed-origins`
- Docker Compose: `docker compose build app` → `docker compose up -d` (Spring Boot + Nginx + Certbot).  
  `deploy/nginx/conf.d/whatthebus.conf.sample` 복사 후 도메인 교체, 80/443/5684(udp) 개방. 상세 절차는 `DEPLOYMENT.md`, `OCI_DEPLOYMENT.md` 참고.
- DTLS는 Nginx를 거치지 않고 컨테이너 5684/udp로 직접 수신.

## 참고
- `build/` 산출물은 커밋 전 정리하세요.
- 기본 CORS 허용 도메인: `https://what-the-bus-web.vercel.app` (환경변수로 변경 가능).
- DTLS 문제 시 `logging.level.WhatTheBus.Service.DtlsServerService=DEBUG`로 로그 확인.
