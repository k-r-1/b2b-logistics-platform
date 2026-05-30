# 🍗 MSA 기반 B2B 물류 운영 플랫폼, "*BoxOffice*"
- 📌 전국 17개 허브를 기반으로 B2B 물류의 주문·배송·업체·상품 관리를 처리하는 MSA 기반 물류 운영 플랫폼입니다. 주요 도메인을 독립 서비스로 분리하고, 주문 및 배송 상태 변경을 Kafka 이벤트로 발행하여 Slack 알림과 AI 기반 발송 시한 산출 기능이 비동기적으로 연동되도록 설계했습니다.

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

## 🚀 서비스 구성 및 실행 방법
Prerequisites
Java 21

Docker & Docker Compose

인프라 원클릭 셋업 (Infra Setup)
프로젝트 루트 디렉토리에서 아래 명령어를 통해 PostgreSQL(도메인별 스키마 분리), Redis, Kafka, Keycloak 인프라를 한 번에 기동합니다.
<img width="453" height="75" alt="image" src="https://github.com/user-attachments/assets/058f64c6-d9a9-4a38-bff2-93e6c71c6b38" />

서비스 실행 (Service Run)
Eureka Server와 API Gateway를 먼저 기동한 후, 비즈니스 마이크로서비스들을 순차적으로 실행합니다.
<img width="364" height="237" alt="image" src="https://github.com/user-attachments/assets/37424a44-6ac9-4641-ac4a-4b5987bc8a74" />


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

### 📑 Technical Documentation

#### 🧬 ERD Diagram
<p>
  <img src="https://github.com/user-attachments/assets/dc2c8022-e769-467a-ae30-b861995f3d95" width="800">
</p>

#### 🧩 Conceptual Architecture

<p>
  <img src="https://github.com/user-attachments/assets/b54479b3-acb8-426b-b442-923a2630f356" width="700">
</p>

#### 💻 Infra Diagram

<p>
  <img src="https://github.com/user-attachments/assets/f789fcb7-cd2a-48fd-bcba-da32a74efcc6" width="600">
</p>


| Layer             | 요소                                       | 서브넷          |
| ----------------- | ---------------------------------------- | ------------ |
| Ingress           | ALB → API Gateway                        | Public       |
| Service Discovery | Eureka Server                            | Public       |
| Application       | MSA Container (6 Services)               | Private      |
| Data              | Kafka Cluster, Redis, PostgreSQL (6 DBs) | Private Data |
| Authentication    | Keycloak                                 |       |

#### 🔥Network | Firewall

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




