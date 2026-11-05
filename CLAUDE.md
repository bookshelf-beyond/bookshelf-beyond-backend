# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

BookSwap (북스왑) backend: a neighborhood 1:1 book-swap service. Spring Boot 4.1, Java 21, Gradle, PostgreSQL 17, Flyway. The product spec is `docs/prd.md` (Korean); use it as the reference for all domain and feature work, and build Phase 1 (MVP) scope first.

## Commands

```bash
docker compose up -d          # local Postgres on host port 5434 (not 5432; other projects use 5432/5433)
./gradlew bootRun             # runs with the default `local` profile
./gradlew test                # needs Docker running; Testcontainers starts its own Postgres
./gradlew test --tests com.bookswap.SecurityConfigTests            # single test class
./gradlew test --tests 'com.bookswap.SecurityConfigTests.someTest' # single test method
curl localhost:8080/actuator/health
```

No lint or formatter is configured.

## Architecture

- **Package layout**: `com.bookswap.<domain>` with domains `auth`, `member`, `book`, `exchange`, `chat`, plus `global` (`config`, `error`). Only `global` exists so far; create a domain package when implementing it.
- **Schema is Flyway-only**: `spring.jpa.hibernate.ddl-auto=validate`. Add `src/main/resources/db/migration/V{n}__{description}.sql`; a JPA entity without a matching migration fails at startup. JDBC time zone is UTC.
- **Profiles**: `local` is the default (`application-local.yml`, hard-coded `bookshelf/bookshelf` DB creds). `prod` reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from the environment. `.env` is intentionally unused because Spring Boot does not read it.
- **Error handling**: throw `BusinessException(ErrorCode)`; `GlobalExceptionHandler` turns it into `{ "code", "message" }` (`ErrorResponse`). Add new error cases to the `ErrorCode` enum (status + default message). Validation and unreadable-body errors map to `INVALID_INPUT`; anything unhandled maps to `INTERNAL_ERROR`.
- **Security** (`global/config/SecurityConfig.java`): stateless, CSRF disabled. Only `/actuator/health/**` and `/api/auth/**` are public; everything else requires authentication. 401/403 responses are written as the same `ErrorResponse` JSON by the entry point and access-denied handler, not by the `@RestControllerAdvice`, because those run in the filter chain.
- **Jackson 3**: `ObjectMapper` comes from `tools.jackson.databind`, not `com.fasterxml.jackson`. Spring Boot 4 starters are also split per feature (for example `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`).
- **Tests**: integration tests `@Import(TestcontainersConfiguration.class)`, which provides a `postgres:17` container through `@ServiceConnection`. Keep the image in sync with `docker-compose.yml`.

## Code style

Tabs for indentation, Lombok available (`@Slf4j` is used), and record-style accessors on enums (`errorCode.status()`, not `getStatus()`).
