# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository overview

Kola is a fintech app (mobile money / savings / micro-credit, currency XOF) with four active codebases in this monorepo:

- `backend/` — Spring Boot 3.5 (Java 17) REST API
- `mobile/` — Flutter app (the client)
- `frontend-web/` — Next.js 16, **the public site and the client space in one application**: marketing pages at `/`, `/a-propos`, `/etudes-de-cas`… and the authenticated space at `/mon-compte`, `/coffres`, `/credit`… One build, one port, one domain.
- `frontend-admin/` — minimal React 19 + Vite admin console, its own admin login, talks to `/api/v1/admin/console/**`

**`landing/` no longer runs on its own.** Its pages, components and content modules were merged into `frontend-web/`; the folder is kept only until the merge is validated, and nothing should be added to it. Both web apps are covered by `.github/workflows/frontend-admin-ci.yml` (Node 22 — Vite 8 needs ≥ 20.19) and `frontend-web-ci.yml` (Node 20), each `npm ci` → `npm run lint` → `npm run build`.

### Dev ports — they are not interchangeable

`SecurityConfig.corsConfigurationSource()` allows exactly three origins: `http://localhost:3000`, `http://localhost:3002` and `http://10.0.2.2:8081`. So:

| App | Port | Why |
|---|---|---|
| `frontend-admin/` | **3000** | a browser origin the API accepts; pinned with `strictPort` in `vite.config.js` (dev and preview) |
| `frontend-web/` | **3002** | public site **and** client space, one server; the other accepted origin |
| `backend/` | 8081 | `server.port` in `application.properties` |

Port 3001 is now free: the marketing site is served by `frontend-web` on 3002. Running it separately again would put the public pages on an origin the API rejects, and split one domain in two.

Running any of these apps on another port silently breaks every API request with a CORS failure. Adding a new browser origin means editing the Java CORS list (a wildcard is not an option — `setAllowCredentials(true)` forbids it).

## Backend (`backend/`)

### Commands

Run all commands from `backend/`.

```
mvn clean package -DskipTests    # build (what CI does)
mvn test                          # run all tests (uses H2, see src/test/resources/application.properties)
mvn test -Dtest=WalletServiceTest # run a single test class
mvn test -Dtest=WalletServiceTest#methodName  # run a single test method
mvn spring-boot:run                # run the app locally (needs Postgres + Redis, see below)
```

CI (`.github/workflows/backend-ci.yml`) only builds the jar with `-DskipTests` on push/PR to `main` — it does not currently run the test suite.

### Local dependencies

The app expects Postgres on `localhost:5432` (db `kola_db`) and Redis on `localhost:6379` (see `application.properties`). Tests use an in-memory H2 database instead (`src/test/resources/application.properties`), so `mvn test` does not need Postgres running.

### Secrets

Real secrets (JWT signing key, mail credentials) are **not** hardcoded in `application.properties` — they're read from environment variables (`JWT_SECRET_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD`, plus optional `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`MAIL_HOST`/`MAIL_PORT` overrides). `spring-dotenv` (added to `pom.xml`) auto-loads them from `backend/.env` at startup — copy `backend/.env.example` to `backend/.env` and fill in real values; `.env` is gitignored and must never be committed. Both `application.properties` files (main and test) **are** versioned — they hold only `${VAR}` placeholders, never a real secret, so keep it that way: anything sensitive goes in `.env`, not in a properties file.

### Architecture

Package-by-feature under `com.kola.backend`: `auth`, `user`, `wallet`, `vault`, `transaction`, `beneficiary`, `credit`, `scheduler`, `admin`, `role`, `token`, `email`, `ratelimit`, `security`, `config`, `exception`. Each feature package generally has its own entity, repository, service, controller, and request/response DTOs — there's no separate `dto`/`repository` layer split across packages.

Key flows to understand before making changes:

- **`POST /api/auth/forgot-password` answers 202 whether or not the address exists.** An unknown address used to raise a `RuntimeException` (500) while a known one returned 202 — the status code alone told an attacker who banks with Kola. The unknown case now returns silently after consuming its rate-limit quota (keeping the probe costly). Don't "improve" this by reporting a missing account; the client message ("si un compte existe pour cette adresse…") depends on it. A timing difference remains — the known path writes a token and dispatches an e-mail — so this closes the status-code oracle, not every side channel.
- **Auth** (`auth/AuthenticationService.java`): registration requires email confirmation via a 6-digit OTP token (`token/Token.java`, `TokenType.ACTIVATION`) before the account is usable — there is no auto-login after register. Login issues a JWT access token + refresh token (`security/JwtService.java`). Failed logins are tracked on the `User` entity (`failedLoginAttempts`, `accountLocked`, `lockedAt`) and auto-lock after 5 attempts for 30 minutes. New-device/IP logins trigger a notification email. All sensitive auth endpoints are throttled via `ratelimit/RateLimitingService.java` (Bucket4j + Redis, policies in `RateLimitPolicy`).
- **Security** (`config/SecurityConfig.java`): stateless JWT auth (`security/JwtAuthFilter.java` runs before `UsernamePasswordAuthenticationFilter`). `/api/auth/**` and `/api/test/**` are public, `/api/admin/**` requires `ADMIN` authority, `/api/credit/**` requires authentication, everything else defaults to `authenticated()`. CORS currently only allows `localhost:3000` and the Android emulator origin `10.0.2.2:8081`.
- **Money model**: a `User` owns multiple `Wallet`s, `Vault`s (locked savings), and `Beneficiary`s. Per a deliberate fintech policy documented in `User.java`, wallets/vaults/beneficiaries are never hard-deleted — they're deactivated (`active = false` / status enums) instead. `Transaction` is the ledger entity for deposits, withdrawals, transfers, and fees; `TransactionType`/`TransactionStatus` drive most business logic elsewhere (e.g. credit scoring).
- **Credit scoring — solvency and capacity are two different questions.**
  - `CreditScoringService` answers *will this person repay?* — 8 weighted rules totalling **exactly 100** (a test, `ScoringRuleWeightsTest`, enforces the sum; they used to declare 105 while the code could produce 115, which let a borrower top the scale while failing a whole rule). `CreditTier` derived from the score sets the **interest rate** and an absolute ceiling — never the amount.
  - `RepaymentCapacityService` answers *how much can this person actually repay?* — from the real cash flows of the last 90 days: inflow − outflow = disposable, discounted when income is irregular (`stabilityFactor`), committed at 40 %, inverted through the bullet-repayment formula `P = (disposable × n × 0.40) / (1 + r × n)`. Two borrowers on the same tier therefore get **different amounts** if their flows differ — that is the point, and `RepaymentCapacityTest` pins it.
  - The retained amount is `min(cash-flow capacity, tier ceiling, graduation ceiling)`, and `LoanCapacity.limitingFactor` says **which** bound applied, so the UI can explain the number instead of asserting it. Graduation: a first loan is capped at 50 000 XOF whatever the score, then doubles with each on-time repayment — how microfinance manages risk while the default history is still being built.
  - `LOAN_REPAYMENT_HISTORY` weighs the most (20) and a borrower with no history scores a neutral 8, which **caps the total at 88 — PREMIUM at best**. ELITE is earned by repaying, not by opening a tidy account.
  - **Knock-out conditions** (`LoanNotEligibleException`, 422) sit outside the score because a weighted score always compensates: account ≥ 90 days, KYC ≥ TIER_1. No transaction volume buys those.
  - `GET /api/credit/capacity` is what a loan form must read. **Never bound the amount field on `ScoreBreakdown.maxLoanAmount`** — that is the tier ceiling, usually unreachable.
- **Scheduling**: `scheduler/ScheduledTransferService` + `ScheduledTransferJob` handle recurring/future-dated transfers independent of the credit scoring scheduler.
- **Mobile-money deposits** (`payment/`, sandbox-only for now): `POST /api/transactions/deposit/mobile-money` answers **202** and credits nothing. It opens a `PENDING` ledger entry, then calls FedaPay (create transaction -> generate token -> `POST /{mode}` — the operator code sits at the API root, **not** under `/transactions`, which answers 404); the wallet is credited only when `POST /api/webhooks/fedapay` receives `transaction.approved`. Four rules hold this together:
  1. **The ledger entry is written before the provider is called.** The reverse order loses deposits: a crash after the operator accepted would leave money debited from the customer with no trace here. The worst case is now an orphan `PENDING` row — visible and repairable.
  2. **The provider call happens outside any DB transaction** (`MobileMoneyDepositService` is a separate bean on purpose). Holding a connection across a multi-second HTTP call exhausts the pool under load.
  3. **The webhook route is public and the HMAC signature is the only lock** (`WebhookSignatureVerifier`, `t=<ts>,s=<sig>` over `<ts>.<raw body>`, 300s tolerance). The body must be read as a raw `String` — re-serializing invalidates a genuine signature. With no `fedapay.webhook-secret` set, every notification is refused.
  4. **Settlement is idempotent twice over**: a pessimistic lock (`findByProviderTransactionIdForUpdate`) serializes concurrent retries, and a status guard ignores anything no longer `PENDING`. FedaPay retries up to 9 times. Non-matching events are acknowledged with 200 — returning an error would get the endpoint auto-disabled after 10 failures.
  5. **Two collection paths, one settlement.** Direct charge (`POST /{mode}`, the request pushed to the phone) requires a commercial authorisation FedaPay grants per merchant account; without it every operator answers `400 Opération non autorisée`. `fedapay.direct-charge` (`FEDAPAY_DIRECT_CHARGE`) is therefore **false by default** and the fallback is the provider's hosted page, whose URL comes back with the created transaction and is stored on the ledger entry (`provider_payment_url`, V4). Storing it is what makes an idempotent replay usable — a customer who closes the tab gets the same link back instead of an unpayable pending row. Both paths end at the same `transaction.approved` webhook: nothing downstream distinguishes them, and nothing should. `GET /api/payments/methods` exposes `directCharge` so the UI announces the right gesture before the form is filled.
  **The old `POST /api/transactions/deposit` still credits instantly** and is dev/manual-only. Before going live it must be locked down to ADMIN or removed — it is a self-service money printer otherwise.
- **Mobile-money withdrawals** (`payment/MobileMoneyWithdrawalService`, `PayoutReconciliationJob`): `POST /api/transactions/withdraw/mobile-money` answers **202**, debits the wallet **immediately** (otherwise the amount stays spendable while the operator processes it) and settles later. Three things hold it together:
  1. **Payouts are a separate FedaPay authorisation from collection.** `POST /v1/payouts` answers `403 Opération non autorisée` until the merchant account is cleared for it, and **there is no fallback** — a payout leaves the merchant balance, there is no hosted page where a customer could pay themselves. Hence `fedapay.payouts-enabled` (`FEDAPAY_PAYOUTS_ENABLED`), false by default, surfaced as `PaymentProvider.payoutsAvailable()` and asked **before the ledger entry is opened**: a closed channel must not make a balance dip and come back. Refusal is `PaymentMethodUnavailableException` → 503 `PAYMENT_METHOD_UNAVAILABLE`, logged WARN — it is a configuration, not an incident (`PaymentProviderException` stays 502/ERROR for real failures).
  2. **Every failure path refunds amount *and* fees.** Fees pay for a service that was not rendered. `abandonPendingWithdrawal` covers the provider refusing up front, `settleMobileMoneyWithdrawal(id, false)` covers a payout that failed later; both are guarded by status so a replayed reconciliation refunds once. `MobileMoneyWithdrawalTest` pins all of it.
  3. **No webhook covers payouts** — FedaPay only emits events for transactions and customers — so `PayoutReconciliationJob` re-reads statuses every 5 min and never concludes from silence: only `sent`/`failed` are verdicts. Provider ids are stored with a `payout_` prefix, since transaction and payout ids are separate sequences sharing one unique column.
- **Errors**: all exceptions funnel through `exception/GlobalExceptionHandler.java` into one `ErrorResponse` shape (`code`, `message`, `details`, `path`, `timestamp`). Custom domain exceptions (`InsufficientFundsException`, `VaultLockedException`, `WalletInactiveException`, `KycLimitExceededException`, `InsufficientCreditScoreException`, `ActiveLoanExistsException`, `InvalidTokenException` — a wrong/expired/already-used activation or reset code, 400 not 500; `InvalidRefreshTokenException` — a refresh token that no longer authenticates anyone, 401 not 500, etc.) map to specific HTTP statuses there — add new business-rule exceptions there rather than throwing generic ones.
- Swagger/OpenAPI UI is available at `/swagger-ui.html` (springdoc), permitted without auth. Actuator exposes `health`, `info`, `mappings` only.

## Mobile (`mobile/`)

### Commands

Run all commands from `mobile/`.

```
flutter pub get
dart format --output=none --set-exit-if-changed .   # formatting check (CI)
flutter analyze                                       # lints (CI)
flutter test --coverage                               # all tests (CI)
flutter test test/path/to/some_test.dart               # single test file
flutter run                                            # run on device/emulator
```

CI (`.github/workflows/mobile-ci.yml`) runs format check, analyze, and tests on every push to `main` under `mobile/**`, using Flutter 3.44.2 stable.

### Architecture

State management is `provider` (`ChangeNotifier`-based providers in `lib/providers/`, e.g. `AuthProvider`, `WalletProvider`). Screens live under `lib/screens/<feature>/`, shared design-system widgets under `lib/core/widgets/`, theme constants under `lib/core/theme/`. Networking goes through `lib/services/` (`auth_service.dart`, `wallet_service.dart`) which call the backend at `AppConstants.baseUrl` (`lib/core/constants/app_constants.dart`) — note this defaults to `http://10.0.2.2:8081/api`, the special Android-emulator alias for the host machine's localhost, so it must be changed for physical devices, iOS simulator, or non-local backends. Tokens are persisted via `flutter_secure_storage` behind `StorageService`.

Routing is centralized in `lib/routes/app_routes.dart` as a static named-route map (`Navigator.pushNamed`), registered in `app.dart`/`main.dart`.

Two behaviors that are easy to get wrong because they diverge from typical auth flows (see comments in `auth_provider.dart` / `app_routes.dart`):
- `POST /api/auth/register` returns 202 and does **not** log the user in — the account stays disabled until the user clicks the emailed confirmation link. `AuthProvider.register()` intentionally leaves status as `unauthenticated` on success.
- Login is email + password only; there is no OTP login route wired up currently (`otp_verification_screen.dart` exists but is unused/reserved for future 2FA).

## Web app (`frontend-web/`) — public site + client space

One Next application serves two publics since the merge of `landing/`:

| Group | Routes | Layout | Indexed |
|---|---|---|---|
| `(marketing)` | `/`, `/a-propos`, `/etudes-de-cas` (+ `[slug]`), `/contact`, `/merci`, `/cgu`, `/confidentialite`, `/cookies` | header, footer, Lenis smooth scroll, cookie banner, GA | yes |
| `(private)/(app)` | `/mon-compte` (dashboard), `/operations/**`, `/transactions`, `/coffres`, `/credit`, `/virements-programmes`, `/beneficiaires`, `/notifications`, `/comptes`, `/profil` | app shell (sidebar + bottom nav), auth guard | **no** |
| `(private)/(auth)` | `/connexion`, `/inscription`, `/confirmation`, `/mot-de-passe-oublie`, `/reinitialiser` | centered auth shell | **no** |

**`/` belongs to the marketing site**; the dashboard lives at `/mon-compte`. Route groups add no path segment, so two groups cannot both define `/` — that is the one collision the merge had to resolve.

Four rules the merge introduced, each easy to break:

1. **The root layout carries only what both publics share** — language, fonts, entity JSON-LD, skip link. Header/footer/Lenis/consent live in `(marketing)/layout.tsx`; the session provider lives in `(private)/layout.tsx`. Hoisting the provider back to the root would make the public homepage call `/users/me` for every visitor still holding a token.
2. **`(private)/layout.tsx` is a server component** so it can export `metadata: { robots: { index: false } }`. Private routes would otherwise inherit the marketing metadata — and its indexing. `robots.ts` disallows them as well: `noindex` stops indexing, `Disallow` stops crawling, and one without the other leaves a gap.
3. **Google Analytics is mounted in `(marketing)/layout.tsx` and nowhere else.** Session tokens live in the `localStorage` of an origin now shared by both publics. Confining the tag keeps third-party scripts off authenticated pages — the residual risk (same origin) is documented there and only `HttpOnly` cookies would close it.
4. **Two radius families on purpose**: `rounded-card`/`rounded-panel` (1.25/1.75rem) belong to the marketing sections; `rounded-surface`/`rounded-sheet`/`rounded-field` (20/24/12px) belong to the product UI and mirror Flutter's `AppRadius`. A dashboard built at editorial radii reads as a brochure.

The client space remains the **web version of the mobile app** — same users, same JWT, same endpoints as `mobile/`, minus the admin surface. When a behaviour differs between this app and `mobile/`, one of the two is wrong.

### Commands

Run from `frontend-web/`. Same stack as `landing/` (Next 16 App Router, React 19, Tailwind v4 with tokens in `@theme`), no component library, no charting library, **no third-party script at all**.

```
npm run dev      # :3002 — must be this port, see CORS table above
npm run build    # production build + TypeScript check
npm run lint     # what CI runs
```

Stack: Next 16 App Router, React 19, Tailwind v4 (tokens in `@theme`), plus GSAP + Lenis inherited from the marketing site — the animation libraries are imported by `(marketing)` components only, so client-space pages never load them.

`landing/AGENTS.md`'s rule applies here too: consult `node_modules/next/dist/docs/` rather than older App Router memory. `params` is a `Promise` — in client components unwrap it with `use()`.

### Layout

- `src/app/(app)/` — authenticated screens (`/`, `/operations/{depot,retrait,envoi,paiement}`, `/transactions` + `[reference]`, `/coffres` + `[id]`, `/credit` + `/demande` + `/prets/[id]`, `/virements-programmes`, `/beneficiaires`, `/notifications`, `/comptes`, `/profil` + `/modifier` + `/mot-de-passe`). Its layout holds the auth guard and the shell.
- `src/app/(auth)/` — `/connexion`, `/inscription`, `/confirmation`, `/mot-de-passe-oublie`, `/reinitialiser`.
- `src/lib/` — `api.ts` (fetch + single-flight refresh + `ApiError`/`SessionExpiredError`/`NetworkError` + `newIdempotencyKey`), `services.ts` (**one function per endpoint — no `apiFetch` call lives anywhere else**), `types.ts` (mirrors the Java DTOs), `session.tsx`, `use-resource.ts`, `labels.ts`, `format.ts`, `countries.ts`.
- `src/components/ui/primitives.tsx` — all base UI in one module; `icons.tsx` — hand-drawn SVG set (no icon package).

`useResource` is the same derived-`loading` hook as the admin console, plus `describeActionError` for form submissions.

### Four things to keep true

1. **Every money operation carries an idempotency key created with the *intention*, not at submit time.** It lives in component state from mount and is only regenerated after a success or a change of amount/recipient. Generating it inside the submit handler would produce a fresh key per retry — hence a double debit.
2. **After any money operation, call `reloadWallets()`** (from `useSession`). Wallets live in the session context precisely so one deposit updates every balance on screen; without the call, the home page keeps showing the pre-operation balance.
3. **A successful operation replaces the form** (`OperationResult`) instead of stacking a banner above a still-filled form — that is how a transfer gets sent twice.
4. **Transfers only go to a registered beneficiary.** `TransferRequest` takes a `beneficiaryId`, never a raw number; the phone number is verified once, cold, on `/beneficiaires`.

### Notable decisions

- **Not every 200 is JSON.** `POST /auth/confirm` and `POST /auth/reset-password` are declared `ResponseEntity<String>` and answer with a plain-text sentence. `apiFetch` therefore branches on the response `Content-Type` instead of calling `JSON.parse` unconditionally — doing the latter threw a `SyntaxError` on a *successful* activation, so the sign-up flow ended on an imaginary error while the account had in fact been activated. Any new endpoint returning `ResponseEntity<String>` is covered by the same branch.
- **The activation e-mail points at `/confirmation` on this app** (`application.mailing.frontend.activation-url`, overridable via `FRONTEND_ACTIVATION_URL`). The link carries no code — the OTP is printed in the e-mail body only, never in a URL.
- **Tokens in `localStorage`**, same trade-off as the admin console (`AuthController.logout` documents it; the API sets no cookie). The mitigation is that the app loads zero third-party resources — fonts are self-hosted by `next/font`.
- **Refresh distinguishes *rejected* from *unreachable*.** A 401 that fails to refresh because the server is down throws `NetworkError` and **keeps** the tokens; only an actual refusal clears them. Losing the network for three seconds must not log someone out (same rule as `SessionManager` on mobile).
- **QR scanning is progressive enhancement.** `MerchantScanner` uses the browser's native `BarcodeDetector` (Chromium only) read through `useSyncExternalStore`; where it is absent the button simply doesn't render and manual merchant-code entry — always available — is the main path. No JS QR decoding library is bundled.
- **Design tokens are copied from `landing/`** (`kola-*`, `ink-*`, canvas/surface, Sora + Inter) so the marketing site, the mobile app and this app read as one product.

## Landing site (`landing/`)

### Commands

Run all commands from `landing/`.

```
npm run dev      # dev server on :3000
npm run build    # production build — also runs the TypeScript type check
npm run start    # serve the production build
npm run lint     # eslint (flat config, eslint.config.mjs)
```

**Read `landing/AGENTS.md` before writing code here.** It pins a hard rule: this Next.js version (16.2) has breaking changes vs. older training data, and the authoritative docs are bundled in `landing/node_modules/next/dist/docs/` — consult them rather than recalling older App Router conventions. Notably: `params` in dynamic routes is a `Promise` and must be awaited.

### Stack

Next 16 App Router (Turbopack), React 19, Tailwind **v4** (no `tailwind.config.ts` — design tokens live in `@theme` inside `src/app/globals.css`), GSAP + ScrollTrigger + SplitText for motion, Lenis for smooth scroll. No component library.

### Architecture

- `src/app/` — routes. Pages: `/`, `/a-propos`, `/etudes-de-cas` (+ `[slug]`), `/contact`, `/merci`, `/cgu`, `/confidentialite`, `/cookies`, plus `not-found.tsx`. Metadata files `robots.ts`, `sitemap.ts`, `opengraph-image.tsx`, `twitter-image.tsx` are generated, never static assets.
- `src/lib/` — all copy and data as typed constants (`content.ts`, `faq.ts`, `reviews.ts`, `case-studies.ts`, `legal.ts`, `privacy.ts`, `team.ts`), plus `business.ts` (company identity) and `schema.ts` (JSON-LD builders). **Text lives here, not inline in components** — the same string usually feeds a page, a metadata field and a structured-data node.
- `src/components/sections/` — page sections; `src/components/ui/` — design-system primitives; `src/components/layout/`, `providers/`, `motion/`, `analytics/`.

### Three invariants that are easy to break

1. **Consent precedes loading.** `components/analytics/google-analytics.tsx` renders nothing until `useConsent()` returns `"granted"` — no request to Google, no `_ga` cookie. Any new third-party script must go through the same gate *and* be listed on `/cookies` before it ships. Same reason `CoverageMap` only injects its OpenStreetMap iframe after a click.
2. **Only mark up what is true.** `lib/schema.ts` emits `Organization` + `areaServed` rather than `FinancialService` + `postalAddress` because `OFFICE` is `null` in `business.ts` (Kola has no public office), and withholds `Review`/`AggregateRating` because `REVIEWS_ARE_REAL` is `false`. Both flags flip the schema automatically — change the constant, not the schema.
3. **Illustrative content is labelled as such.** `lib/reviews.ts` and `lib/case-studies.ts` hold written-by-the-team scenarios, not customer testimonials. The visible disclaimer is load-bearing, not decoration.

### Environment

`landing/.env.example` documents `NEXT_PUBLIC_GA_ID` (GA4 measurement ID; unset = no analytics at all) and `CONTACT_WEBHOOK_URL` (server-side only; unset = `/api/contact` returns 503 and the UI falls back to the direct email address). Copy to `.env.local`; only `.env.example` is versioned.

## Admin console (`frontend-admin/`)

A deliberately small React 19 + Vite + react-router console, rewritten from scratch (2026-09) after the Dogaa `FrontendWeb` import. Contract: `backend/BACKEND.md` §17 (`/api/v1/admin/console/**`) plus `/auth/*` (§4.1) and the KYC file download.

### Commands

Run from `frontend-admin/`.

```
npm run dev      # :3000 (strictPort) — must be this port, see CORS table above
npm run build
npm run lint     # what CI runs
```

### Two rules, and what breaks them

1. **Nothing the mobile app does not offer, bar what the owner asked for.** Seven sections: Accueil, Utilisateurs (detail = wallets, vaults, loans, scheduled payments, recent transactions, KYC documents; actions: unblock, accept/refuse a KYC document — hidden for the Support role), KYC (pending / accepted / refused), Transactions, Crédits, Épargne (savers list + totals) and Paramètres (own account; fee grid and credit ladder editable by the Super-admin only). Disputes, support tickets, finance, merchants, audit log and admin roles were **removed on purpose** — their backend routes still exist, the console does not call them. Adding a section means checking `mobile/lib/services` first.
2. **The API serves raw values; the console formats them**, in `src/lib/format.js` only (amounts, dates, enum labels, badge tones). Do not reintroduce server-side display strings.

- **No mock mode.** Without `VITE_API_BASE_URL` (`.env.development` is versioned) the app shows an error instead of inventing data — the old mock login accepted any password.
- **Auth**: e-mail + password on `/auth/login`, one 8 h JWT in `localStorage['kola_admin_token']`, no refresh. A 401 dispatches `kola:unauthorized` and signs out; a **403** (module not granted to the role) shows "Accès refusé" and keeps the session. `ROLE_MODULES` in `src/lib/session.jsx` mirrors `AdminRole` only to hide links.
- **KYC files are previewed in the page as images, never opened in a tab.** `useDocumentImage` fetches the file with the admin token, reads its first bytes and only accepts JPEG/PNG/GIF/WebP — the type the client declared at upload proves nothing, and SVG (script) and PDF (viewer running in the console's origin) are refused; the result goes into an `<img>` and nowhere else (no tab, no `<iframe>`, no `<object>`). PDFs and unknown formats keep the `download()` button (`attachment`, saved to disk). `DocumentPreview` is remounted per dossier (`key`) so one dossier never shows another's document while it loads.
- **All styling lives in `src/styles.css`** as tokens (only computed chart heights are inline). Manrope is bundled via `@fontsource-variable/manrope` — no request to a third party.
- **Form rules, kept on purpose** (the user rejected the usual "vibe-coded" tells): mobile-app colours only (navy `#002353`, blue `#0047BA`), flat — no gradient, no purple, no dark mode; separation by hairlines — no drop shadow, no dot grid, no blurred orb; no emoji, no star, no check-mark bullets; **a hover changes a colour, nothing moves**. Corners are 10 px (controls) and 14 px (cards, dialogs, tiles).
- **Motion and material (the `apple-design` pass, branch `admin-apple-design`).** Three things move, and only these:
  1. *Surfaces that present themselves* — side panel (from the right), dialog (scale, born where the pointer pressed), the mobile « Plus » sheet (from the bottom bar). They are springs (`lib/spring.js`, driven by `lib/presenter.js` through `usePresentation`), critically damped (no bounce unless a flick carried momentum), **interruptible** (grabbing one mid-flight freezes it where it is), and exit by the same path they entered. The panel header and the sheet are drag handles: 1:1 tracking after 10 px, rubber-band past the stop, release velocity handed to the spring, destination chosen from the *projected* resting point. Drag handles must stay `user-select: none` (a selected title started a native drag-and-drop that cancelled the gesture) and links inside them `-webkit-user-drag: none`.
  2. *Press feedback* — `:active` shrinks controls to 0.96 in 70 ms and releases over `--settle`; wide rows only change background. It happens on press, not on release.
  3. *Nothing else.* No decorative animation; the skeleton pulse is the only loop.
  Material: the bottom bar and the « Plus » sheet are translucent (`backdrop-filter`), the dialog/panel scrim blurs what is behind (blur radius fixed, only its opacity follows the progress variable `--p` — animating the radius cost frames). Everything else is a solid surface. Tracking and leading depend on size (large titles tighten, small text opens slightly); font sizes are in `rem` so the browser's text-size setting scales them. `prefers-reduced-motion` → no movement, a ~160 ms fade, drag disabled; `prefers-reduced-transparency` → solid chrome, no blur; `prefers-contrast: more` → stronger hairlines. A confirm dialog closed by the parent (success) does not play its exit — only cancel / Esc / scrim do.
- **Paramètres** (`/parametres`, `pages/Settings.jsx`): « Mon compte » for every role; « Frais » (`GET/PUT /config/fees`, module `config`, Super-admin only) and « Barème de crédit » (`GET/PUT /credit/tier-config`, read-only for the credit analyst, editable by the Super-admin). Nothing is sent before a before/after recap is confirmed. A rung without ceiling travels as `"Aucun plafond"` — never `0`, which the server refuses because it would cap every loan at 0.
- **Light visual load.** The dashboard is a small bento: the day's volume vs yesterday with the 14-day chart (the big tile), four day-vs-yesterday KPIs (new customers, loans granted, fees earned, repayments received), and an "À traiter" strip — the user wants few boxes, not no bento. Add a KPI only on request. List toolbars keep only the essentials (search, a single period `<select>`, export); every secondary filter (type, status, KYC tier, exact dates) sits behind a "Filtres" button that shows how many are active. Totals go on one line under the page title; the per-type breakdown lives in the Excel export, not on screen.
- **KYC views.** The KYC screen has three views — pending, accepted, refused — each sortable by date, in the URL (`?statut=refuses&tri=anciens&dossier=…`). Pending comes from `GET /console/kyc` (whole queue, sorted in the browser); accepted and refused come from `GET /console/kyc/history` (paginated, sorted by decision date on the server). That route, the `reviewedAt` field of `ConsoleKycDocument`, and the two savings routes (`GET /console/savings` and `/console/savings/summary`, behind the Épargne screen) are the **only backend additions of the console redesign**, made at the owner's explicit request: before them the server exposed no list of decided documents and no list or total of savings.
- **Lists are paginated server-side** (15 rows by default, numbered pages, size 10/15/25) and filters live in the URL (`useFilters`), so a filtered view is a shareable link. Periods use `PeriodFilter` (presets + two dates, UTC days, both bounds inclusive).

## Admin API surface (`/api/admin/**`, ADMIN authority enforced in `SecurityConfig`)

| Endpoint | Notes |
|---|---|
| `GET /overview` | dashboard analytics — 10 scalars + 8 breakdowns |
| `GET /users` | paginated; filters `q`, `kycLevel`, `enabled`, `locked` |
| `GET /users/{id}` | profile + wallets + vaults + credit score + loans |
| `PATCH /users/{id}/kyc` | `{kycLevel, reason}` — reason 10–500 chars, mandatory |
| `POST /users/{id}/lock` | `{reason}` — refuses self-lock |
| `POST /users/{id}/unlock` | resets the failed-attempt counter |
| `GET /loans`, `/loans/{id}`, `/loans/overview` | read-only |
| `POST /loans/{id}/approve`, `POST /loans/{id}/reject` | decide a loan left `PENDING` by the manual-review threshold; both delegate to `LoanService` |
| `GET /aml/alerts`, `/aml/alerts/{id}`, `/aml/overview`, `POST /aml/alerts/{id}/review` | compliance console |

Three invariants worth keeping:

1. **An admin lock sets `lockedAt = null`.** `AuthenticationService.checkAndAutoUnlockIfExpired()` releases any lock older than 30 minutes *when `lockedAt` is non-null*. Leaving it null is what makes an administrative lock hold until a human lifts it, with no change to the auth service. Don't "fix" it by stamping a timestamp.
2. **No admin endpoint *writes* a loan's status.** Disbursement moves money, repayment debits a wallet and updates the score, default is pronounced by a batch — all in `LoanService`. The two decision routes are not an exception: they call `LoanService.approve` / `LoanService.reject`, which credit the wallet, write the ledger entry and notify the borrower in one transaction. A `setStatus(APPROVED)` in an admin service would desynchronise the displayed status from the real balance — that is what the invariant forbids, and what the delegation preserves.
3. **Loans above `LoanService.manualReviewThreshold()` (200 000 XOF) are not auto-approved.** They are created `PENDING`, **with no `dueDate` and no disbursement** — the money has not moved. The due date is set at approval, so the review delay is not charged to the borrower. Approving twice is refused (`IllegalStateException`); `ManualLoanReviewTest` pins the fact that a pending loan credits nothing.

**Known gap:** KYC changes and locks are traced only via SLF4J `WARN` lines carrying the acting admin's email and the reason. There is no persistent audit table — it would need a Flyway migration (`ddl-auto=validate`). That is the next thing to build here.

## Cross-cutting notes

- French is the working language for code comments, commit messages, log messages, and user-facing error/email text throughout the backend — match that convention when touching existing files.
- The backend and mobile CI workflows are path-filtered (`paths: backend/**` / `mobile/**`), so changes outside those directories won't trigger them.
