# 📦 MSA 기반 B2B 물류 운영 플랫폼, "*BoxOffice*"
> **전국 17개 허브를 기반으로 B2B 물류의 주문·배송·업체·상품 관리를 처리하는 MSA 기반 물류 운영 플랫폼입니다.**
> 주요 도메인을 독립 서비스로 분리하고, 주문 및 배송 상태 변경을 Kafka 이벤트로 발행하여 Slack 알림과 AI 기반 발송 시한 예측 로직이 비동기적으로 연동되도록 설계했습니다.

<br>

## 👥 팀원 역할분담
| **이름** | **GitHub** | **역할 (Domain)** | **주요 업무 및 성과** |
| --- | --- | --- | --- |
| **손형호 (리더)** | <a href="https://github.com/GolemOnce"><img src="https://img.shields.io/badge/GitHub-GolemOnce-181717?style=flat-square&logo=github&logoColor=white"/></a> | Delivery Domain | 프로젝트 총괄 및 일정 관리, 배송 도메인 API 구현 |
| **권효승** | <a href="https://github.com/hy-ogu"><img src="https://img.shields.io/badge/GitHub-hy--ogu-181717?style=flat-square&logo=github&logoColor=white"/></a> | Notification / Slack Domain, AI Domain | 알림 및 Slack 연동 기능 구현, AI 도메인 API 구현 |
| **박주원** | <a href="https://github.com/k-r-1"><img src="https://img.shields.io/badge/GitHub-k--r--1-181717?style=flat-square&logo=github&logoColor=white"/></a> | Company & Product Domain | 업체 및 상품 도메인 API 구현 |
| **오영현** | <a href="https://github.com/dddd2356"><img src="https://img.shields.io/badge/GitHub-dddd2356-181717?style=flat-square&logo=github&logoColor=white"/></a> | Hub Domain | 공통 모듈 및 허브 도메인 API 구현 |
| **하준영** | <a href="https://github.com/HaJunyoung"><img src="https://img.shields.io/badge/GitHub-HaJunyoung-181717?style=flat-square&logo=github&logoColor=white"/></a> | User & Auth Domain, Delivery Manager | 사용자 및 인증 기능 구현, 배송 담당자 도메인 API 구현, 인프라 설계 |
| **한혜수** | <a href="https://github.com/hyesuhan"><img src="https://img.shields.io/badge/GitHub-hyesuhan-181717?style=flat-square&logo=github&logoColor=white"/></a> | Order Domain | 주문 도메인 API 구현 |

<br>


## 🔍 프로젝트 목적/상세

### 🛠️ Tech Stack

| 분야 | 기술 |
|------|------|
| 💻 Backend | ![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5.14-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white) ![QueryDSL](https://img.shields.io/badge/QueryDSL_5.1.0-0769AD?style=flat-square)|
| 🔨 Build | ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white) |
| ☁️ MSA | ![Spring Cloud](https://img.shields.io/badge/Spring_Cloud_2025.0.2-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Gateway](https://img.shields.io/badge/Gateway-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Eureka](https://img.shields.io/badge/Eureka-6DB33F?style=flat-square&logo=spring&logoColor=white) ![OpenFeign](https://img.shields.io/badge/OpenFeign-6DB33F?style=flat-square&logo=spring&logoColor=white) |
| 🗄️ Database | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL_15-4169E1?style=flat-square&logo=postgresql&logoColor=white) |
| ⚡ Cache | ![Redis](https://img.shields.io/badge/Redis_7-DC382D?style=flat-square&logo=redis&logoColor=white) |
| 📡 Messaging | ![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=flat-square&logo=apachekafka&logoColor=white) |
| 🔐 Security | ![Keycloak](https://img.shields.io/badge/Keycloak_26.0.0-4D4D4D?style=flat-square&logo=keycloak&logoColor=white) ![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white) |
| 🤖 AI | ![Gemini API](https://img.shields.io/badge/Gemini_API-4285F4?style=flat-square&logo=google-gemini&logoColor=white) |
| 🚀 Infra | ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white) ![Docker Compose](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white) |
| 📊 Monitoring | ![Zipkin](https://img.shields.io/badge/Zipkin-FE7A16?style=flat-square) |

<br>

### ⚙️ System Architecture

#### 🧩 Conceptual Architecture
API Gateway를 단일 진입점으로 하여 라우팅 및 전역 인증(JWT)을 처리하며, 내부 서비스 간에는 동기적 FeignClient 호출과 비동기적 Kafka 이벤트 연동을 전략적으로 혼합하여 사용합니다.
<p align="center">
  <img src="https://github.com/user-attachments/assets/b54479b3-acb8-426b-b442-923a2630f356" width="700">
</p>

#### 🧬 ERD Diagram (Database Per Service)
마이크로서비스 원칙에 따라 각 도메인별로 물리적인 데이터베이스(스키마)를 완벽히 분리(Database per Service)하여 서비스 간의 결합도를 낮췄습니다.
<p align="center">
  <img src="https://github.com/user-attachments/assets/dc2c8022-e769-467a-ae30-b861995f3d95" width="800">
</p>

#### 🛡️ Network & Security (Firewall)
* **API Gateway 전역 인증:** `JwtAuthenticationFilter`를 통해 모든 외부 요청의 Keycloak JWT 토큰을 검증합니다.
* **망 분리:** 외부망(Public Subnet)에서는 API Gateway(8080)만 접근이 가능하며, 실제 비즈니스 로직을 처리하는 6개의 MSA 컨테이너와 DB/Kafka 인프라는 내부망(Private Subnet)으로 철저히 격리했습니다.

<br>

> 인바운드 정책

| 대상           | 허용 포트       | 소스                 | 설명                        |
| ------------ | ----------- | ------------------ | ------------------------- |
| ALB          | 80, 443     | 0.0.0.0/0          | 외부 HTTPS 트래픽 수신           |
| API Gateway  | 8080        | ALB SG             | ALB를 통해서만 접근 허용           |
| MSA Service  | 8081 ~ 8086 | Gateway SG         | Gateway 및 내부 서비스만 접근 가능   |
| Kafka Broker | 9092        | App SG             | 애플리케이션 서브넷 내부 통신만 허용      |
| Redis        | 6379        | App SG             | 애플리케이션 서브넷 내부 통신만 허용      |
| PostgreSQL   | 5432        | App SG             | 애플리케이션 서브넷 내부 통신만 허용      |
| Keycloak     | 8443        | Gateway SG, App SG | JWT 공개키 조회 및 토큰 검증용 접근 허용 |

> 아웃바운드 정책

| 대상                        | 포트                 | 설명                            |
| ------------------------- | ------------------ | ----------------------------- |
| MSA → 외부 인터넷              | 차단                 | 외부 네트워크 직접 접근 제한              |
| Slack Service → Slack API | 443                | Slack 알림 전송 허용 (화이트리스트 적용 가능) |
| 내부 서비스 간 통신               | FeignClient, Kafka | 서비스 간 통신 허용                   |



#### 💻 Infra Diagram

<p align="center">
  <img src="https://github.com/user-attachments/assets/f789fcb7-cd2a-48fd-bcba-da32a74efcc6" width="600">
</p>


| Layer             | 요소                                       | 서브넷          |
| ----------------- | ---------------------------------------- | ------------ |
| Ingress           | ALB → API Gateway                        | Public       |
| Service Discovery | Eureka Server                            | Public       |
| Application       | MSA Container (6 Services)               | Private      |
| Data              | Kafka Cluster, Redis, PostgreSQL (6 DBs) | Private Data |
| Authentication    | Keycloak                                 |       |


<br>

## ✨ Core Features (주요 기능)

> ### ✔️ 각자 도메인 1~2줄 정도 추가하면 좋을 것 같습니다
> (예: 주문 도메인 - 재고 차감을 위한 Saga 패턴 적용 여부)

> (예: AI 도메인 - Gemini를 활용한 배송 지연 확률 예측 로직)

> (예: 배송/허브 도메인 - 허브 간 최적 경로 산출 알고리즘 적용 등)

------------

### 1. 🔐 중앙 집중식 인증 및 다중 권한 제어 (User & Auth)
* **Keycloak 기반 SSO 및 IAM:** 인증과 인가 책임을 `Keycloak`으로 위임하여 각 마이크로서비스는 비즈니스 로직에만 집중하도록 아키텍처를 설계했습니다.
* **데이터 격리 (Data Isolation):** `HUB_MANAGER` 권한을 가진 유저는 시스템 전역 데이터가 아닌, **자신이 소속된 허브의 기사님/유저/주문 목록만 조회할 수 있도록** 강력한 데이터 격리 정책을 적용했습니다.

### 2. 🚚 라운드 로빈(Round-Robin) 기반 배송 기사 자동 배정 (Delivery Manager)
* 특정 허브에 신규 배송 건이 발생 시, **대기 중(`WAITING`)인 기사님들 중 '마지막 배정 시각(`lastAssignedAt`)'이 가장 오래된 순서(오름차순)**로 쿼리하여 기사님을 공평하게 자동 배정합니다.

### 3. 분산 트랜잭션 관리 및 재고 정합성 (Order & Product)
* **Saga Pattern (Choreography):** 주문 생성 시, 상품 서비스로 재고 차감 요청을 비동기로 전달합니다. 재고 부족 등으로 실패할 경우 보상 이벤트를 발행하여 주문을 자동 취소(Canceled) 처리함으로써 데이터 최종 정합성(Eventual Consistency)을 보장합니다.

### 4. 🤖 AI 기반 예측 및 비동기 알림 (AI & Notification)
* **Gemini 연동:** 신규 주문 생성 이벤트를 수신하면, AI 서비스가 해당 주문의 발송 시한과 배송 지연 확률을 예측하여 데이터를 가공합니다.
* **Slack Webhook:** 재시도 로직과 DLQ가 적용된 Notification 서비스를 통해 가공된 알림을 관리자 슬랙 채널로 안전하게 전송합니다.
  
<br>

## 📈 Performance & Observability (성능 최적화 및 모니터링)
> (예시입니다)
> 실제로 측정하신 내용 있으면 추가 부탁드립니다

- **응답 속도 개선 (Redis Cache-Aside):** 허브 정보, 권한 검증 등 변경이 적고 조회가 빈번한 데이터에 Redis 캐싱을 적용하여 API 평균 응답 시간을 최소화했습니다. (목표: 캐시 적중률 80% 이상)
- **대용량 트래픽 처리 (Kafka Partitioning):** 주문 생성 및 배송 이벤트 등 트래픽이 몰리는 구간에 Kafka 파티셔닝을 적용하여 비동기 병렬 처리 Throughput(처리량)을 극대화했습니다.
- **분산 추적 (Zipkin):** 단일 클라이언트 요청이 여러 마이크로서비스를 거치는 전체 흐름(Trace)을 가시화하여, 병목 현상이나 500 에러 발생 지점(예: FeignClient 실패)을 신속하게 디버깅할 수 있는 환경을 구축했습니다.

<br>

## 🤝 Collaboration & Process (협업 방식 및 개발 문화)

### 1. API Contract 주도 설계와 병렬 개발
* **문제:** MSA 환경에서 타 서비스의 API가 완성될 때까지 개발이 블로킹되는 의존성 병목 발생.
* **해결:** 기획 단계에서 프론트/백엔드뿐만 아니라 **백엔드 서비스 간(Feign) 주고받을 JSON 스펙을 사전 정의(API Contract)**. 이를 통해 타 팀원의 진행도와 무관하게 독립적인 병렬 개발과 테스트(Mocking)를 진행했습니다.

### 2. CI 파이프라인 구축 (GitHub Actions)
* **Dev CI:** PR 생성 시 `Checkstyle`, `Unit Test`, `Jacoco Code Coverage(최소 50%)`를 자동 검증하며, 불필요한 빌드 캐시로 인한 에러를 예방(`clean test`)합니다.
* **Main CI:** Main 브랜치 병합 시, Docker Compose를 활용한 `Integration Test`를 수행하여 실제 인프라 환경에서의 정합성을 2차 검증합니다. (Discord Webhook 연동)
* **Flyway DB 마이그레이션:** 로컬 스키마 변경 시 발생하던 환경 불일치 문제를 해결하기 위해 Flyway를 도입, `git pull` 만으로 모든 팀원이 동일한 DB 상태를 유지하도록 자동화했습니다.
  
<br>

## 💥 Troubleshooting (트러블슈팅)

### Issue 1. 외부 API 호출 시 트랜잭션 점유 시간 최소화 (Facade 패턴)
* **상황:** 업체 생성/수정 시, 타 서비스(Hub)를 호출하여 유효성을 검증하는 로직과 DB 저장 로직이 한 곳에 혼재됨.
* **문제:** 외부 API(Feign) 호출 시 발생할 수 있는 네트워크 지연이 DB 트랜잭션에 포함되어, DB 커넥션 풀 고갈 위험 증가.
* **해결:** **Facade 계층**을 도입하여 트랜잭션이 없는 상태에서 외부 API 검증을 선처리하고, 실제 DB 조작이 필요한 핵심 로직만 Service 계층(@Transactional)으로 위임하여 **트랜잭션 범위를 획기적으로 축소**했습니다.

### Issue 2. 분산 환경에서의 보상 트랜잭션(Compensating Transaction) 처리
* **상황:** `User Service` 회원가입 시 Keycloak 유저 생성 후 로컬 DB 저장 단계에서 예외가 발생하면, Keycloak에만 유저가 남는 **고아 데이터(Orphan Data)** 현상 발생.
* **해결:** 단순 `@Transactional`로는 외부 시스템(Keycloak) 롤백이 불가능함을 인지하고, `catch` 블록 내에서 `keycloakClient.deleteUser()` API를 명시적으로 호출하는 **프로그래밍적 보상 트랜잭션 패턴**을 적용해 정합성을 확보했습니다.

### Issue 3. 분산 환경의 Kafka 이벤트 발행 타이밍 이슈 (Ghost Event 방지)
* **상황:** 배송 배차 처리 로직 내에서 상태를 변경(DB Save)한 직후 Kafka 이벤트를 발행하는 구조로 구현.
* **문제:** `@Transactional` 내부에서 Kafka를 직접 호출할 경우, 비즈니스 로직 예외로 DB가 롤백되더라도 **Kafka 이벤트는 이미 발행(Commit-Rollback 불일치)**되어 정합성 파괴 문제 발생.
* **해결:** 이벤트 발행 로직을 분리하고, `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`을 적용하여 **DB 커밋이 완벽하게 성공한 직후에만 Kafka 이벤트가 발행**되도록 보장했습니다.

### Issue 4. 비동기 이벤트 유실 방지 및 전송 보장 (Outbox Pattern)
* **상황:** 네트워크 지연이나 외부 시스템(Kafka, Slack) 장애 시 이벤트 발송이 누락되는 문제 발생. (`AFTER_COMMIT` 직후 서버 다운 시 이벤트 영구 유실)
* **해결:** **Transactional Outbox 패턴**을 도입. 메인 트랜잭션과 동일한 트랜잭션으로 이벤트 정보를 DB에 `PENDING` 상태로 우선 저장 후 발송. 실패 시 **Fallback 스케줄러**가 주기적으로 `PENDING` 상태의 이벤트를 폴링(Polling)하여 재시도함으로써 **최소 1회(At-Least-Once) 전송을 보장**하는 회복력 있는 아키텍처를 구축했습니다.

### Issue 5. MSA 환경에서의 에러 책임 소재 파악 및 디버깅
* **상황:** User Service에서 Hub Service로 Feign 요청 시 500 에러 발생. 다수의 마이크로서비스 중 어느 서버의 문제인지 특정하기 어려움.
* **해결:** 각 서비스 컨테이너의 로그 트레이싱(Zipkin)을 통해 Hub Service에서 `NoResourceFoundException`이 발생했음을 확인. 객관적 지표를 바탕으로 담당 팀원과 URL 매핑 스펙을 대조하여 병목 없이 신속하게 해결했습니다.

### Issue 6. 모노레포 CI 환경 빌드 캐시 충돌 및 단위 테스트 전환
* **상황:** CI 구동 시 삭제된 파일의 `.class` 캐시가 남아 `ConflictingBeanDefinitionException`을 유발하고, 깡통 서버에서 `@SpringBootTest`가 DB 연결을 시도해 CI 실패.
* **해결:** 테스트 스텝에 `./gradlew clean test` 명령어를 적용해 캐시 유령(Ghost Class) 문제를 해결하고, 무거운 컨텍스트 로딩 대신 **JUnit5 + Mockito 기반의 단위 테스트(Unit Test)**로 전면 개편하여 비즈니스 로직 엣지 케이스 커버리지를 끌어올렸습니다.

### Issue 7. (추가 작성 내용)

...
<br>
...
<br>
...

<br>

## 📚 API Documentation

프론트엔드 및 타 도메인 개발자와의 원활한 협업을 위해, 설계 초기부터 API Contract를 명확히 정의했습니다.
* [📘 BoxOffice 통합 API 명세서 보러가기 (Notion)](https://www.notion.so/teamsparta/BoxOffice-SA-3612dc3ef5148084ae4aefad9e37fca7?source=copy_link) <br>

<br>

## 🚀 서비스 구성 및 실행 방법
### Prerequisites
  * Java 21
  * Docker & Docker Compose

### 인프라 원클릭 셋업 (Infra Setup)
프로젝트 루트 디렉토리에서 아래 명령어를 통해 PostgreSQL(도메인별 스키마 분리), Redis, Kafka, Keycloak 인프라를 한 번에 기동합니다.
```bash
$ docker-compose up -d postgres-db redis-cache keycloak
```

### 서비스 실행 (Service Run)
Eureka Server와 API Gateway를 먼저 기동한 후, 비즈니스 마이크로서비스들을 순차적으로 실행합니다.
```bash
# 1. Service Discovery
$ ./gradlew :eureka-server:bootRun

# 2. API Gateway
$ ./gradlew :api-gateway:bootRun

# 3. Domain Services
$./gradlew :user-service:bootRun$ ./gradlew :delivery-manager-service:bootRun
# ... (다른 서비스들도 동일한 방식으로 실행)
```



