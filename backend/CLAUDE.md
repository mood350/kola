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

## Stack notes

- Spring Boot **4.1.1**, Java release target **17** (the installed JDK is 25 — do not assume language features above 17 compile).
- Boot 4 splits the old starters: this project uses `spring-boot-starter-webmvc` (not `spring-boot-starter-web`) and the matching `*-test` starters (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-security-test`, `-thymeleaf-test`) instead of the single `spring-boot-starter-test`. Keep to that convention when adding dependencies.
- Persistence: Spring Data JPA + PostgreSQL (runtime driver only). `application.properties` currently declares nothing but the app name — no datasource, no JPA config. A `.env` / `.env.example` pair exists but is empty and gitignored; wire configuration through it rather than hardcoding credentials.
- Security: Spring Security + `thymeleaf-extras-springsecurity6`. Server-rendered Thymeleaf views coexist with the REST API — the admin back-office (spec §4.5) is the likely consumer of the Thymeleaf side.
- API docs: springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) → Swagger UI at `/swagger-ui.html` once controllers exist.
- Lombok is an optional dependency wired explicitly into `annotationProcessorPaths` for both `default-compile` and `default-testCompile`; if you add another annotation processor (MapStruct, etc.) it must be added to *both* executions or compilation breaks.

## Package layout

Vertical slices under `com.dogaa.backend.modules.<module>`, each with the same six sub-packages:

```
modules/<module>/{entity,dto,mapper,repository,service,controller}
```

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

Rules that follow from this: a module never reaches into another module's `repository` or `entity` — cross-module access goes through the owning module's `service`. Shared enums used by more than one module belong in `common/enums`, not in one module's `entity` package. The `.gitkeep` files exist only to hold empty directories; delete them as real classes land.

## Domain shape to expect

The spec implies these subsystems, mapped onto the modules above:

- **Wallet** — multi-currency (XOF/GHS/NGN/USD) balances split into *available* vs *locked*. Fees are dynamic and depend on the user's KYC tier; cash-in is free, P2P ~1.5%, cash-out ~1%.
- **Vaults** — savings goals. Depositing moves money from available to locked balance; it must become unspendable by ordinary transactions.
- **Scheduled transactions** — one engine covering vault deposits, P2P transfers, merchant payments and bill payments. A midnight scheduled job checks funds + KYC tier per due item, executes, writes a transaction record, and reschedules the next occurrence. Failures are recorded, notified, and optionally retried (~24h) rather than silently dropped.
- **Credit scoring** — a scheduled job scoring 0–100 over the last 30 days from deposit regularity, savings discipline, transaction volume/diversity, balance stability, and honoured scheduled transactions. The score gates loan tiers; repayment (principal + interest) is debited automatically at maturity.
- **KYC** — progressive tiers TIER_0 (phone) → TIER_1 (email) → TIER_2 (ID document) → TIER_3 (validated), each raising transaction limits; TIER_2 is the credit gate. Tier checks belong at the transaction-execution boundary, since the scheduler must enforce them too.
- **Admin back-office** — live metrics, dispute/fraud handling including chargebacks that reverse funds between the involved accounts, manual account unblocking, forced vault closure.

Note the groupId is `com.doga` while the Java package is `com.dogaa.backend` — the package name is the one to follow.
