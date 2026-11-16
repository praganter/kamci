# Kamci — mobile app

Flutter client for **Kamci**, a personal and household debt tracker: recurring
and one-off debts, instalments, partial/bulk payments, reminders, shared
categories, multi-currency support and monthly charts. It talks to the Kamci
backend (Spring Boot microservices behind an API gateway) over REST.

## Screens

| Area | Screens |
|---|---|
| Auth | splash / session restore, sign in, register, forgot + reset password (deep link), change password, delete account |
| Debts | grouped list with month filter and "mine / shared with me" tabs, detail with payment history, create/edit form (period types, instalments, auto-payment, reminders, multi-currency), pay / partial pay / skip / bulk pay |
| Categories | list, detail, member management with access levels (read / write / admin) |
| Income | current income, history, add entry |
| Charts | monthly summary, trend, category breakdown, debt-to-income ratio |
| Household | invite / accept / leave, shared view |
| Notifications | in-app list with deep-link routing (push optional, see below) |
| Settings | language (tr/en), theme, premium paywall (RevenueCat, optional), feedback |

## Architecture

Feature-first, with `data / domain / presentation` layers per feature:

```
lib/
├── core/          constants, network (Dio + interceptors), router, storage, theme, l10n helpers
├── features/      auth · debt · category · income · charts · household · notifications · settings · subscription
│   └── <feature>/{data,domain,presentation}
├── shared/        reusable widgets and providers
└── l10n/          app_tr.arb (template) + app_en.arb  → lib/gen/l10n
```

- **State:** Riverpod with `riverpod_generator` (`@riverpod` providers and notifiers).
- **Networking:** Dio. `AuthInterceptor` attaches the bearer token, and on an
  expired/invalid token refreshes it once and retries (concurrent requests are
  serialised; a rejected refresh ends the session). Credential-check endpoints
  (change/reset password) are excluded so a wrong password is not mistaken for
  an expired session. Errors are mapped to a typed `ApiException` hierarchy and
  localised from the backend's `error.*` codes.
- **Routing:** `go_router` with an auth-aware redirect and deep links
  (`kamci://` scheme for password reset and notification targets).
- **Models:** `freezed` + `json_serializable`. Money and dates tolerate the
  backend's number/ISO-string variants (including nanosecond `LocalDateTime`).
- **i18n:** ARB files, Turkish template + English, generated with `flutter gen-l10n`.
- **Storage:** `flutter_secure_storage` for tokens, `shared_preferences` for settings.

More detail: [`docs/architecture.md`](docs/architecture.md), backend contract: [`docs/api-contracts.md`](docs/api-contracts.md).

## Running it

### Toolchain

Flutter **3.41.x** (Dart 3.11). The code-generation stack (`riverpod_generator`
2.x → `analyzer` 7) does not yet work with newer Flutter SDKs whose framework
sources use newer Dart syntax, so use 3.41.6 (what CI uses) to run `build_runner`.

```bash
flutter pub get
dart run build_runner build --delete-conflicting-outputs   # *.g.dart / *.freezed.dart (not committed)
flutter gen-l10n
```

### Backend

Start the backend stack (see the backend README; a single
`docker compose up -d --wait` brings up MongoDB, RabbitMQ, Redis, the services
and the gateway). The app expects the **gateway on port 8080**.

The base URL defaults to the local gateway and is platform-aware:

| Target | Default `API_BASE_URL` | Why |
|---|---|---|
| Android emulator | `http://10.0.2.2:8080` | `10.0.2.2` is the emulator's alias for the host machine |
| iOS simulator, desktop | `http://localhost:8080` | shares the host network |
| Physical device | pass it yourself | e.g. `--dart-define=API_BASE_URL=http://192.168.x.y:8080` |

```bash
flutter run                                              # local gateway
flutter run --dart-define=API_BASE_URL=http://host:8080  # anything else
```

Cleartext HTTP is allowed only for local development (debug-only Android
manifest, iOS `NSAllowsLocalNetworking`). Password-reset mail lands in the
backend's Mailpit UI (`http://localhost:8025`).

### Firebase / push notifications (optional)

`google-services.json` and `GoogleService-Info.plist` are **not** in the repo
and the app does not need them: Firebase initialisation is attempted at start-up
and, if it fails, push notifications are simply disabled. To enable push, create
your own Firebase project and drop
`android/app/google-services.json` / `ios/Runner/GoogleService-Info.plist` in
place (both are git-ignored). The Gradle Google-services plugin is applied only
when the Android file exists. The backend side (FCM service account) is likewise
optional — in its `local` profile pushes are logged and skipped.

### Premium / RevenueCat (optional)

Purchases go through RevenueCat. Without keys the paywall shows "store
unavailable" and everything else works:

```bash
flutter run --dart-define=RC_API_KEY_ANDROID=goog_xxx --dart-define=RC_API_KEY_IOS=appl_xxx
```

For local testing, grant premium to an account through the backend's admin
endpoint (`POST /subscription/premium/{userId}` with `X-Admin-Key`, see
`maestro/scripts/setup_test_accounts.sh`).

## Tests

```bash
flutter analyze
flutter test test/core test/features test/shared            # unit + widget tests, no backend needed
tool/coverage.sh 60                                       # same tests with lcov; fails below 60% lib line coverage (generated code excluded)
flutter test test/integration \
  --dart-define=TEST_A_EMAIL=… --dart-define=TEST_A_PASSWORD=…   # hits a running backend
```

What the unit/widget suite covers: the `AuthInterceptor` refresh/retry/logout
state machine against a scripted backend; `AuthNotifier` login/logout and JWT
claim parsing; the login screen's validation and error flow; the grouped debt
providers (month selection, empty owned categories, refresh after payment);
model parsing against **responses captured from the real backend**
(`test/fixtures/`, Spring Boot 4 / Jackson 3 date and number formats); API
error mapping; notification routing; date/time pickers.

Integration tests are skipped unless credentials are provided.

### End-to-end (Maestro)

`maestro/flows/` holds UI flows per feature (auth, debts, categories, income,
charts, sharing, settings, deep links, premium gates).

```bash
cp maestro/.env.example maestro/.env        # or generate accounts on your local backend:
API_BASE_URL=http://localhost:8080 maestro/scripts/setup_test_accounts.sh
maestro/scripts/run_all_by_folder.sh
```

`maestro/.env` is git-ignored. The flows target the Android emulator / iOS
simulator build with `--dart-define=MAESTRO=true` (speeds up animations).
Flows under `premium/` that exercise the store sheet need a store sandbox and
cannot pass against a plain local backend.
