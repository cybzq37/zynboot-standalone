# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (dev profile is default)
mvn clean package
mvn clean package -P prod

# Run a single module's tests
mvn test -pl zyn-infra/zyn-infra-redis

# Run a single test class
mvn test -pl zyn-app/zyn-app-system -Dtest=UserAggregateTest

# Skip tests during build
mvn clean package -DskipTests

# Run middleware (PostgreSQL, Redis, Kafka, ES, SeaweedFS)
cd deploy/middleware
bash scripts/start.sh              # all services
bash scripts/start.sh postgres     # single service
bash scripts/start.sh -f redis     # force rebuild a service
bash scripts/healthcheck.sh        # health check

# Integration tests (Python pytest — requires apps running)
mvn clean package -pl zyn-app/zyn-app-system,zyn-app/zyn-app-demo -am -P prod -DskipTests
bash tests/deploy.sh                          # deploy in WSL
python -m pytest tests/test_system_api.py -v  # run against live services

# Deploy an app as Docker container
cd deploy/app
./build.sh --jar zyn-app-demo.jar --name demo --port 28080
```

## Architecture Overview

This is a Java 21 / Spring Boot 3.5.x multi-module framework. Dependencies flow strictly downward — each layer only depends on layers below it:

```
zyn-kit          (utilities: Jackson, OkHttp, tree builders, ApiResponse, exceptions)
   ↓
zyn-conf         (shared YAML config, no code — imported via spring.config.import)
   ↓
zyn-infra        (infrastructure: exchange, redis, kafka, es, mybatis, storage, geo, web, satoken)
   ↓
zyn-api          (API contracts: ExchangeClient interfaces, Cmd/Query/Res DTOs)
   ↓
zyn-app          (deployable Spring Boot apps: zyn-app-system, zyn-app-demo)
```

The reactor modules are defined in the root `pom.xml` and `zyn-infra/pom.xml`. Infra sub-modules are independent of each other. The base package for all code is `com.zynboot`.

> Note: the authoritative infra module list is the `<modules>` block in `zyn-infra/pom.xml`. If stray `target/`-only directories for non-listed modules reappear (e.g. from switching branches), they are stale build artifacts and can be safely deleted.

### zyn-app-system DDD layout

Each app follows a CQRS + DDD package structure under `com.zynboot.<app>`:

```
sys/
├── controller/           ← REST controllers (implement the zyn-api *Api interfaces)
├── handler/command/      ← write-side command handlers
├── handler/query/        ← read-side query handlers
├── domain/aggregate/     ← aggregate roots (*Aggregate) — business logic
├── domain/repository/    ← domain repository interfaces
├── domain/enums/
├── infrastructure/entity/     ← MyBatis-Plus entities
├── infrastructure/mapper/     ← MyBatis mappers
├── infrastructure/repository/ ← domain repository implementations (*RepositoryImpl)
└── config/
```

## Key Patterns

**CQRS + DDD**: API layer uses `Cmd` objects for writes, `Query` for reads. App layer has aggregate roots (`*Aggregate`) that encapsulate business logic, with domain repositories abstracting persistence from MyBatis mappers/entities.

**Global response wrapping**: `GlobalResponseBodyAdvice` wraps all controller responses in `ApiResponse<T>`. Use `@IgnoreResponseWrap` to opt out. `GlobalExceptionHandler` catches all exceptions and returns `ErrorResponse`.

**Declarative HTTP clients**: Interfaces annotated with `@ExchangeClient("serviceName")` + `@HttpExchange` are auto-scanned from the `com.zynboot` base package (see `ExchangeClientRegistrar`) and registered as Spring beans. Service URLs configured under `zyn.exchange.services.<name>`. Use `@HttpQuery` to expand POJOs into query parameters (Spring 6.x workaround, registered via SPI; native in Spring 7.1+).

**Shared config**: Applications import `zyn-conf-common.yml` and `zyn-conf-{profile}.yml` via `spring.config.import`. Logging uses Log4j2 + Disruptor (Logback is globally excluded).

**Auto-configuration**: All infra modules use Spring Boot `@AutoConfiguration` registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, conditionally enabled via `@ConditionalOnProperty(prefix = "zyn.<module>", name = "enabled", matchIfMissing = true)`.

## Local Development

Middleware is deployed in WSL (already set up). Default dev connection addresses (from `zyn-conf-dev.yml`):

| Service | Address | Default Credentials |
|---------|---------|-------------------|
| PostgreSQL + PostGIS | `localhost:5432/zyn_base` | `postgres` / `Zyn@secure#99` |
| Redis | `localhost:6379` | password: `Zyn@secure#99` |
| Elasticsearch | `localhost:9200` | `elastic` / `Zyn@secure#99` |
| Kafka | `localhost:9092` | — |
| SeaweedFS (S3) | `localhost:8333` | — |

Credentials are overridable via environment variables (`POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `ELASTIC_PASSWORD`). Most infra modules can be individually disabled via `zyn.<module>.enabled: false` — the app will start without them (e.g., disable Kafka/ES if not needed).

## Configuration System

Each app's `application.yml` pulls in shared config via:
```yaml
spring.config.import:
  - classpath:zyn-conf-common.yml       # shared defaults (all envs)
  - classpath:zyn-conf-@spring.profiles.active@.yml  # profile-specific (Maven resource filtering injects dev/prod)
```

Key `zyn.*` configuration switches (all default to `true` / `matchIfMissing`):

| Property | Controls |
|----------|----------|
| `zyn.satoken.enabled` | Sa-Token authentication |
| `zyn.mybatis.enabled` | MyBatis-Plus auto-config |
| `zyn.storage.enabled` | File storage (type: `LOCAL` or `S3`) |
| `zyn.exchange.enabled` | Declarative HTTP client scanning |
| `zyn.web.cors.enabled` | CORS configuration |
| `zyn.web.exception.enabled` | Global exception handler |
| `zyn.web.response.enabled` | Auto response wrapping (`ApiResponse`) |
| `zyn.exchange.services.<name>` | Service base URLs for `@ExchangeClient` |

## Database

PostgreSQL + PostGIS with MyBatis-Plus. Uses `dynamic-datasource` for multi-datasource support (master datasource configured in dev/prod YAML).

Schema and seed data are in `zyn-app-system/src/main/resources/sql/`:
- `schema.sql` — table DDL
- `data.sql` — initial data (including the `root` user)

SQL init is configured as `mode: never` — must be run manually or via `spring.sql.init.mode=always`.

## Adding a New Infra Module

When creating `zyn-infra-<name>`:

1. Create auto-configuration class with `@AutoConfiguration` and `@ConditionalOnProperty(prefix = "zyn.<name>", name = "enabled", havingValue = "true", matchIfMissing = true)`
2. Register it in `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (one FQCN per line)
3. Add default config to `zyn-conf-common.yml` under `zyn.<name>.*`
4. Add the module to `zyn-infra/pom.xml` as a `<module>` entry
5. Add the dependency to app modules that need it

## Apps

| App | Port | Context Path | Purpose |
|-----|------|-------------|---------|
| zyn-app-system | 28081 | /sys | System management (users, roles, permissions, orgs) |
| zyn-app-demo | 28080 | /demo | Showcase of all infra modules |

## Naming Conventions

| Type | Suffix | Example |
|------|--------|---------|
| Response DTO | `Res` | `UserRes`, `LoginRes` |
| GET query param | `Query` | `UserPageQuery` |
| Write command | `Cmd` | `UserSaveCmd` |
| HTTP client interface | `*Api` | `SysUserApi` |
| Aggregate root | `*Aggregate` | `UserAggregate` |
| Domain repository | `*Repository` / `*RepositoryImpl` | `UserRepository` |
| MyBatis mapper | `*Mapper` | `SysUserMapper` |
| Auto-configuration | `*AutoConfiguration` | `RedisAutoConfiguration` |

## Coding Conventions

- Lombok everywhere: `@Data`, `@RequiredArgsConstructor`, `@Getter`, `@Slf4j`
- Constructor injection via `@RequiredArgsConstructor`
- UUID primary keys: `@TableId(type = IdType.ASSIGN_UUID)` with `VARCHAR(64)` columns
- Audit fields (`createBy`, `createTime`, `updateBy`, `updateTime`) auto-filled by `AuditMetaObjectHandler`
- Optimistic locking via `@Version`, soft delete via `@TableLogic` (entities extend `BaseEntity`)
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `refactor:`, `build:`, `chore:`, `docs:`
- `dev` profile (default): Swagger enabled, debug logging. `prod` profile: Swagger off, error details hidden.
