# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Dogaa backend — a mobile-money wallet / programmed-savings / algorithmic-microcredit platform targeting the UEMOA zone (Togo, Senegal, Côte d'Ivoire, Ghana). The functional spec lives in `../DOGAA.md` (French); read it before implementing any domain feature — it is the source of truth for business rules (fee percentages, KYC tiers, scoring signals, scheduler semantics).

Current state: a freshly generated Spring Boot skeleton. Only `BackendApplication` and the default context-loads test exist. Essentially every domain package still has to be created, so architectural decisions made here set the precedent for the rest of the codebase.

## Commands

Maven wrapper (Maven 3.9.16) — use `./mvnw` in Bash, `.\mvnw.cmd` in PowerShell.

```bash
./mvnw spring-boot:run                     # run the app (devtools hot reload is on the classpath)
./mvnw clean package                       # build the jar into target/
./mvnw test                                # all tests
./mvnw test -Dtest=BackendApplicationTests # a single test class
./mvnw test -Dtest=ClassName#methodName    # a single test method
```

No lint/format plugin is configured.

## Authentication

Phone + PIN, no passwords. Implemented across `modules/auth`, `modules/user` and `config/SecurityConfig`.

- **Identifier**: the phone number, normalised to E.164 by `common/util/PhoneNumbers` before it ever
  reaches the database. `90123456`, `+228 90 12 34 56` and `0022890123456` are the same account.
  Normalise at the edge of every new feature that accepts a phone number.
- **Secret**: a 4-6 digit PIN, BCrypt strength 12, rejected by `PinPolicy` when trivial (repeated or
  consecutive digits). Because the key space is tiny, `app.security.auth.max-pin-attempts` wrong
  attempts lock the account for `lock-duration`.
- **Recording a failed attempt must not join the caller's transaction.** `UserService.registerFailedPinAttempt`
  is `REQUIRES_NEW` precisely because the login path aborts by throwing; in the same transaction the
  rollback would erase the counter and the lockout would never fire. Any future "count the failure,
  then reject" path needs the same treatment.
- **Tokens**: a short-lived HS256 access JWT (`JwtService`) plus an opaque refresh token stored as a
  SHA-256 hash and rotated on every use (`RefreshTokenService`). Access tokens are not revocable by
  design — revocation happens on the refresh token, and a PIN change revokes every one of them.
- Endpoints: `POST /api/v1/auth/{register,login,refresh,logout,logout-all,change-pin}`,
  `GET|PATCH /api/v1/users/me`. Only the first four are public — see `PUBLIC_ENDPOINTS` in `SecurityConfig`.
- Controllers read the caller with `@AuthenticationPrincipal CurrentUser`; never trust a user id from
  the request body.

## Stack notes

- Spring Boot **4.1.1**, Java release target **17** (the installed JDK is 25 — do not assume language features above 17 compile).
- Boot 4 splits the old starters: this project uses `spring-boot-starter-webmvc` (not `spring-boot-starter-web`) and the matching `*-test` starters (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-security-test`, `-thymeleaf-test`) instead of the single `spring-boot-starter-test`. Keep to that convention when adding dependencies.
- **Boot 4 ships Jackson 3**: the autoconfigured `ObjectMapper` bean is `tools.jackson.databind.ObjectMapper`. Jackson 2 (`com.fasterxml.jackson.databind`) is on the classpath only as a jjwt transitive dependency and has no bean — injecting it fails at startup. Annotations (`@JsonInclude`, ...) still come from `com.fasterxml.jackson.annotation`.
- **Boot 4 moved the test autoconfigurations**: `@AutoConfigureMockMvc` is `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`, not `...boot.test.autoconfigure.web.servlet`.
- Persistence: Spring Data JPA + PostgreSQL; every entity extends `common/audit/BaseEntity` (UUID id, created/updated timestamps, `@Version`). `application.properties` is **gitignored** — copy `application.properties.example` and fill it in. Tests run against H2 via `src/test/resources/application-test.properties` and `@ActiveProfiles("test")`, so no local database is needed.
- Security: Spring Security + `thymeleaf-extras-springsecurity6`. Server-rendered Thymeleaf views coexist with the REST API — the admin back-office (spec §4.5) is the likely consumer of the Thymeleaf side.
- API docs: springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) → Swagger UI at `/swagger-ui.html` once controllers exist.
- Lombok is an optional dependency wired explicitly into `annotationProcessorPaths` for both `default-compile` and `default-testCompile`; if you add another annotation processor (MapStruct, etc.) it must be added to *both* executions or compilation breaks.

## Package layout

Vertical slices under `com.dogaa.backend.modules.<module>`, each with the same six sub-packages:

```
modules/<module>/{entity,dto,mapper,repository,service,controller}
```

`modules/auth` additionally has a `security/` package (JWT issuing/parsing, the servlet filter, the
`CurrentUser` principal, the PIN policy) — framework plumbing that is neither a service nor a controller.

Modules: `auth`, `user`, `kyc`, `wallet`, `transaction`, `vault`, `scheduling`, `credit`, `scoring`, `notification`, `admin`.

Everything cross-cutting stays **outside** `modules`:

```
config/                  Spring config (security, JPA, OpenAPI, scheduling enablement)
exception/               domain exception types
exception/handler/       @RestControllerAdvice / global handlers
common/enums             shared enums (Currency, KycTier, TransactionStatus, ...)
common/dto               shared response envelopes / pagination
common/util              shared helpers
common/audit             JPA auditing base entities & listeners
```

Rules that follow from this: a module never reaches into another module's `repository` — cross-module access goes through the owning module's `service`. Referencing another module's entity type is fine (auth reads `User`), but cross-module *ownership* is not: `RefreshToken` stores a plain `userId` rather than a JPA relation to `User`. Shared enums used by more than one module belong in `common/enums`, not in one module's `entity` package. The `.gitkeep` files exist only to hold empty directories; delete them as real classes land.

## Domain shape to expect

The spec implies these subsystems, mapped onto the modules above:

- **Wallet** — multi-currency (XOF/GHS/NGN/USD) balances split into *available* vs *locked*. Fees are dynamic and depend on the user's KYC tier; cash-in is free, P2P ~1.5%, cash-out ~1%.
- **Vaults** — savings goals. Depositing moves money from available to locked balance; it must become unspendable by ordinary transactions.
- **Scheduled transactions** — one engine covering vault deposits, P2P transfers, merchant payments and bill payments. A midnight scheduled job checks funds + KYC tier per due item, executes, writes a transaction record, and reschedules the next occurrence. Failures are recorded, notified, and optionally retried (~24h) rather than silently dropped.
- **Credit scoring** — a scheduled job scoring 0–100 over the last 30 days from deposit regularity, savings discipline, transaction volume/diversity, balance stability, and honoured scheduled transactions. The score gates loan tiers; repayment (principal + interest) is debited automatically at maturity.
- **KYC** — progressive tiers TIER_0 (phone) → TIER_1 (email) → TIER_2 (ID document) → TIER_3 (validated), each raising transaction limits; TIER_2 is the credit gate. Tier checks belong at the transaction-execution boundary, since the scheduler must enforce them too.
- **Admin back-office** — live metrics, dispute/fraud handling including chargebacks that reverse funds between the involved accounts, manual account unblocking, forced vault closure.

Note the groupId is `com.doga` while the Java package is `com.dogaa.backend` — the package name is the one to follow.
