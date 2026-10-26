# BookSwap (북스왑) Backend

동네 기반 도서 맞교환 플랫폼의 백엔드 서버입니다.

## 기술 스택

- Java 21, Spring Boot 4.1
- Spring Data JPA, Spring Security
- PostgreSQL 17, Flyway
- Gradle, Testcontainers

## 로컬 실행

사전 준비: JDK 21, Docker

```bash
# 1. DB 실행 (호스트 포트 5434)
docker compose up -d

# 2. 서버 실행 (기본 프로필: local)
./gradlew bootRun

# 3. 확인
curl localhost:8080/actuator/health
```

테스트는 Testcontainers가 DB를 직접 띄우므로 Docker만 켜져 있으면 됩니다.

```bash
./gradlew test
```

## 프로필

| 프로필 | 용도 | DB 설정 |
| --- | --- | --- |
| `local` (기본) | 로컬 개발 | `application-local.yml` |
| `prod` | 운영 | 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |

## 패키지 구조

도메인별로 나누고, 공통 코드는 `global`에 둡니다.

```
com.bookswap
├── auth        # 로그인, 회원가입, 토큰
├── member      # 회원, 프로필, 동네 인증
├── book        # 도서, 내 서가
├── exchange    # 교환 제안, 약속
├── chat        # 교환 조율 채팅
└── global
    ├── config  # SecurityConfig 등 공통 설정
    └── error   # ErrorCode, BusinessException, GlobalExceptionHandler
```

각 도메인 패키지는 구현 시점에 만듭니다.

## 에러 응답

모든 에러는 아래 형식으로 응답합니다. 비즈니스 오류는 `BusinessException(ErrorCode)`를 던지면 됩니다.

```json
{
  "code": "INVALID_INPUT",
  "message": "입력값이 올바르지 않습니다."
}
```

## DB 마이그레이션

스키마는 Flyway로만 변경합니다 (`ddl-auto: validate`).
`src/main/resources/db/migration/V{번호}__{설명}.sql` 형식으로 추가하세요.
