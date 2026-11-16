# kamci mobile: Dio API Layer Inventory (api-contracts)

_Source: `lib/`. Reports the current state only. For the architecture and screen
overview see `docs/architecture.md`._

## 0. Infrastructure

| Item | Value |
|---|---|
| Base URL | `String.fromEnvironment('API_BASE_URL', default: platform-dependent local gateway, port 8080)` |
| Fixed headers | `Content-Type: application/json`, `Accept: application/json`, `Accept-Language: <locale?.languageCode ?? 'tr'>` |
| connect / receive timeout | 30 s / 30 s |
| `dioClientProvider` | Base + `AuthInterceptor` (+ `LoggingInterceptor` in debug only) |
| `refreshDioProvider` | Base, NO interceptors (for login/register/refresh/logout and retries) |
| Auth header | `AuthInterceptor.onRequest` → `Authorization: Bearer <accessToken>` (in-memory auth state first, otherwise a Keychain read with a 3 s timeout) |

### Token refresh (AuthInterceptor.onError)
- **Trigger (`_isTokenError`)**: HTTP 401 always; HTTP 403 only when the body is a `Map` and the string `data['code'] ?? data['error'] ?? data['message']` contains `"JWT"`.
- Refresh call: `POST /auth/refresh-token`, header `Authorization: Bearer <refreshToken>`, no body.
- Expected refresh response: `{ "token": string, "refreshToken": string? }` (the old refresh token is kept if `refreshToken` is absent).
- If `requestOptions.extra['_retry'] == true`, the request is not retried and the user is not logged out.
- Logout happens only when the refresh POST itself returns 400/401/403.
- Concurrent refresh guard: if the in-memory access token differs from the request's `Authorization`, the request is retried without a second refresh.

## 1. Error JSON parsing: `ApiException.fromDioException(DioException e)`

All datasources catch `on DioException` and call `ApiException.fromDioException`.

**`data['code']` is checked first (regardless of status):**

| `code` value | Thrown type |
|---|---|
| `error.debt.limit_exceeded`, `error.category.limit_exceeded`, `error.category.premium_required`, `error.debt.premium_required` | `PremiumLimitException` (upsell sheet) |
| `error.debt.permission_denied`, `error.category.permission_denied`, `error.income.permission_denied` | `PermissionDeniedException` (plain error, NO upsell) |
| `error.user.not_found_in_context` | `UnauthorizedException` |
| Business codes: `error.debt.already_completed` (409), `error.debt.overpayment` (400), `error.category.name_conflict` (409), `error.category.cannot_add_self` (409), `error.category.member_not_found` (404), `error.category.{not_found,member_user_not_found,member_already_exists,migrate_same_category}`, `error.debt.{not_found,already_paid_this_period,auto_payment_enabled,concurrent_request,payment_before_start,period_already_recorded,installment_count_exceeded,missing_period_config}`, `error.income.not_found`, `error.reminder.date_or_time_missing`, `error.subscription.{verification_failed,not_found}`, `error.auth.{email_in_use,too_many_login_attempts,not_valid,token_revoked,token_invalid,current_password_invalid,token_already_used,too_many_password_reset_attempts}` | `BusinessException(message, code: <code>)`: `exception_l10n.dart` maps it to a localized message, falling back to the backend `message` |

**No `code`, or unrecognized: status fallback:**

| Condition | Field(s) read | Thrown type |
|---|---|---|
| body is `Map<String,dynamic>` and status 400 | `data['message']` | `BusinessException(message ?? 'Invalid request.')` |
| body is a `Map` and status 401 | `data['message']` | `UnauthorizedException` |
| body is a `Map` and status 403 | `data['message']` | `PremiumLimitException` |
| body is a `Map` and status 409 | `data['message']` | `BusinessException(message ?? 'This operation cannot be completed right now.')` |
| body is a `Map` and status 422 | `data['errors'][0]['message']`, else `data['message']` | `BusinessException` |
| status 404 | none | `NotFoundException` |
| status 429 | none | `RateLimitException` |
| status >= 500 | `data['message']` (if a Map) | `ServerException` |
| `DioExceptionType.connectionTimeout/receiveTimeout/connectionError` | none | `NetworkException` |
| other | none | `UnknownException` |

> Note: the expected error body is `{ "message": string, "code"?: string, "errors"?: [{ "message": string }] }`. `AuthInterceptor` additionally reads the gateway format `{ "error": string, "status": int }` and the service format `{ "code": string }`.

---

## 2. AUTH: `AuthRemoteDatasource`

Dio: `login/register/refreshToken/logout/forgotPassword/resetPassword` use `refreshDioProvider` (no interceptors); `changePassword/userDetail` use `dioClientProvider`.

| Function | Method + Path | Request body (field: type) | Query/Path | Response model (field: type) |
|---|---|---|---|---|
| `login` | `POST /auth/login` | `email: String`, `password: String` | none | `AuthResponse { token→accessToken: String, refreshToken: String }` |
| `register` | `POST /auth/register` | `email: String`, `password: String` | none | `AuthResponse` (same) |
| `refreshToken` | `POST /auth/refresh-token` | no body; header `Authorization: Bearer <refreshToken>` | none | `AuthResponse` (same) |
| `logout` | `POST /auth/logout` | no body; header `Authorization: Bearer <refreshToken>` | none | none (void) |
| `forgotPassword` | `POST /auth/forget-password` | `email: String` | none | none (void) |
| `resetPassword` | `POST /auth/reset-password` | `token: String`, `newPassword: String` | none | none (void) |
| `changePassword` | `POST /auth/change-password` | `currentPassword: String`, `newPassword: String` | none | none (void) |
| `userDetail` | `GET /auth/user-detail` | none | none | parsed by hand: `email: String` (required), `premium: bool?` (default false), `createdAt: String` → `DateTime.parse` |

A `RefreshTokenRequest` model exists, but the datasource sends the refresh token as a header, not a body.

---

## 3. DEBT: `DebtRemoteDatasource` (all use `dioClientProvider`)

| Function | Method + Path | Request body | Query/Path | Response |
|---|---|---|---|---|
| `getGrouped` | `GET /debt/get` | none | query: `month: String` (`YYYY-MM`), `isOwner: bool` | `{ categories: [ { categoryId: String?, categoryName: String?, owned: bool?(def true), ownerUserId: String?, debts: [DebtSummaryModel] } ] }` |
| `getSummary` | `GET /debt/summary` | none | query: `month: String` (`YYYY-MM`) | raw `Map<String,dynamic>` → `MonthlySummaryModel` (see §7) |
| `getById` | `GET /debt/{id}` | none | path: `id` | `DebtModel` (unwrapped) |
| `create` | `POST /debt/save` | `CreateDebtRequest` (below) | none | `{ debt: DebtModel }`; `FormatException` if `debt` is missing |
| `update` | `POST /debt/update` | `CreateDebtRequest` (+ `id`) | none | `{ debt: DebtModel }` |
| `pay` | `POST /debt/pay/{id}` | `{ amountPaid?: double, paidTime?: String(yyyy-MM-ddTHH:mm:ss) }`; body is `null` if both are absent | path: `id` | `{ debt: DebtModel }` |
| `skip` | `POST /debt/skip/{id}` | no body | path: `id` | `{ debt: DebtModel }` |
| `bulkPay` | `POST /debt/pay/{id}/bulk` | JSON **array**: `[ { amountPaid: double, paidTime: String(yyyy-MM-ddTHH:mm:ss) } ]` | path: `id` | `{ debt: DebtModel }` |
| `delete` | `DELETE /debt/delete/{id}` | none | path: `id` | none (void) |

### `CreateDebtRequest.toJson` (create/update body)
| Field | Type | Note |
|---|---|---|
| `id` | `String?` | omitted if null (set on update) |
| `name` | `String` | required |
| `categoryId` | `String` | required |
| `description` | `String?` | omitted if null |
| `firstPaymentDate` | `String` | format `yyyy-MM-ddTHH:mm:ss` (no zone/ms) |
| `installmentCount` | `int?` | omitted if null |
| `paymentPeriodType` | `String` enum | `ONE_TIME` `DAILY` `WEEKLY` `BI_WEEKLY` `MONTHLY` `QUARTERLY` `HALF_YEARLY` `YEARLY` |
| `amount` | `double` | required |
| `autoPayment` | `bool` | always sent (default false) |
| `currency` | `String` | always sent (`'TRY'` if unset) |
| `reminder` | `object?` | only when `reminder != null && reminder.enabled`; otherwise the field is dropped entirely |
| `reminder.enabled` | `bool` | |
| `reminder.channels` | `List<String>` | e.g. `EMAIL`, `PUSH` (`ReminderChannel.toJson`) |
| `reminder.reminderDate` | `String?` | format `yyyy-MM-dd`; omitted if null |
| `reminder.reminderTime` | `String?` | `HH:mm` text; omitted if null |

### `DebtModel.fromJson` (response, from inside the `debt` wrapper)
| Field | JSON key | Type |
|---|---|---|
| `id` | `id` | `String` (required) |
| `name` | `name` | `String` (required) |
| `ownerId` | `ownerId` | `String` (required) |
| `categoryId` | `categoryId` | `String?` |
| `description` | `description` | `String?` |
| `firstPaymentDate` | `firstPaymentDate` | `String?` (parsed to `DateTime`) |
| `installmentCount` | `installmentCount` | `int?` |
| `paidInstallmentCount` | `paidInstallmentCount` | `int?` |
| `paymentPeriodType` | `paymentPeriodType` | enum string (set above), required |
| `amount` | `amount` | `num \| String` → `double` (tolerant) |
| `status` | `status` | `ACTIVE` \| `COMPLETED` (default `ACTIVE`) |
| `paymentStatus` | `paymentStatus` | `PAID` \| `UNPAID` \| `OVERDUE` \| `PARTIALLY_PAID` (default `UNPAID`) |
| `currency` | `currency` | `String?` |
| `autoPayment` | `autoPayment` | `bool` (default false) |
| `payments` | `payments` | `[ { paid: bool, paidTime: String? } ]` (default `[]`) |
| `reminder` | `reminder` | `ReminderEmbeddedModel?` (enabled, channels, reminderDate, reminderTime) |
| `nextReminderDate` | `nextReminderDate` | `String?` |
| `nextAutoPaymentDate` | `nextAutoPaymentDate` | `String?` |
| `createdAt` | `createdDate` | `String?` |
| `updatedAt` | `updatedDate` | `String?` |

### `DebtSummaryModel.fromJson` (`getGrouped` → `categories[].debts[]`)
| Field | Key | Type |
|---|---|---|
| `id` | `id` | `String` |
| `name` | `name` | `String` |
| `categoryId` | `categoryId` | `String` |
| `installmentCount` | `installmentCount` | `int?` |
| `paidCount` | `paidCount` | `int` (default 0) |
| `paymentPeriodType` | `paymentPeriodType` | enum string |
| `amount` | `amount` | `num\|String` → `double` |
| `currency` | `currency` | `String` (required) |
| `status` | `status` | `ACTIVE`\|`COMPLETED` (default ACTIVE) |
| `paymentStatus` | `paymentStatus` | enum (default UNPAID) |
| `autoPayment` | `autoPayment` | `bool` (default false) |
| `paidCurrentPeriod` | `paidCurrentPeriod` | `bool` (default false) |
| `hasReminder` | `hasReminder` | `bool` (default false) |

---

## 4. CATEGORY: `CategoryRemoteDatasource` (`dioClientProvider`)

| Function | Method + Path | Request body | Query/Path | Response |
|---|---|---|---|---|
| `getAll` | `GET /category/getAll` | none | none | JSON **array** `[CategoryModel]` |
| `create` | `POST /category/save` | `{ name: String }` | none | `CategoryModel` (the backend does not return `ownerId`, so the client inserts an `''` placeholder and the repository fills in the real value) |
| `update` | `POST /category/update` | `{ id: String, name: String }` | none | `CategoryModel` (same `ownerId` handling) |
| `delete` | `DELETE /category/delete/{id}` | none | path: `id` | none (void) |
| `addMember` | `POST /category/{categoryId}/member` | `AddMemberRequest { email: String, accessLevel: String }` | path: `categoryId` | none (void) |
| `addMembersBulk` | `POST /category/members/bulk` | `AddMembersBulkRequest { categoryIds: List<String>, email: String, accessLevel: String }` | none | none (void) |
| `removeMember` | `DELETE /category/{categoryId}/member/{userId}` | none | path: `categoryId`, `userId` | none (void) |
| `migrateAndDelete` | `DELETE /category/{id}/migrate` | none | path: `id`; query: `targetCategoryId: String` | `{ migratedDebtCount: int }` |

`accessLevel` wire values: `READ` `WRITE` `EDIT` `DELETE` (domain `admin` ↔ wire `DELETE`).
`removeMember` builds its path string by hand.

### `CategoryModel.fromJson`
| Field | Key | Type |
|---|---|---|
| `id` | `id` | `String` (required) |
| `ownerId` | `ownerId` | `String` (required; absent from the backend save/update response, patched in by the client) |
| `name` | `name` | `String` (required) |
| `members` | `members` | `[ MemberEmbeddedModel ]` (default `[]`) |
| `createdAt` | `createdAt` | `String?` |
| `updatedAt` | `updatedAt` | `String?` |

### `MemberEmbeddedModel.fromJson`
| Field | Key | Type |
|---|---|---|
| `userId` | `userId` | `String?` (**optional**: the backend does not always return it) |
| `accessLevel` | `accessLevel` | enum string `READ/WRITE/EDIT/DELETE` (required) |
| `email` | `email` | `String?` |

---

## 5. INCOME: `IncomeRemoteDatasource` (`dioClientProvider`)

| Function | Method + Path | Request body | Query/Path | Response |
|---|---|---|---|---|
| `save` | `POST /income/save` | `SaveIncomeRequest { amount: double, currency: String, effectiveFrom: String, note?: String }` | none | `IncomeModel` |
| `getHistory` | `GET /income/history` | none | none | JSON **array** `[IncomeModel]` |
| `getCurrent` | `GET /income/current` | none | none | `IncomeModel` |
| `delete` | `DELETE /income/delete/{id}` | none | path: `id` | none (void) |

### `IncomeModel.fromJson`
| Field | Key | Type |
|---|---|---|
| `id` | `id` | `String` (required) |
| `amount` | `amount` | `num\|String` → `double` |
| `currency` | `currency` | `String` (required) |
| `effectiveFrom` | `effectiveFrom` | `String` → `DateTime.parse` (required) |
| `note` | `note` | `String?` |
| `createdAt` | `createdDate` | `String?` |

> `effectiveFrom` is sent as a String in the request (the calling layer formats it, most likely `yyyy-MM-dd`).

---

## 6. SUBSCRIPTION: `SubscriptionRemoteDatasource` (`dioClientProvider`)

| Function | Method + Path | Request body | Response |
|---|---|---|---|
| `getStatus` | `GET /subscription/status` | none | `SubscriptionModel` |
| `verifyReceipt` | `POST /subscription/verify` | `VerifyReceiptRequest` (below) | `SubscriptionModel` |

### `VerifyReceiptRequest.toJson`
| Field | Type | Note |
|---|---|---|
| `receiptData` | `String` | required |
| `plan` | `String` | required |
| `productId` | `String` | required |
| `platform` | `String` | required: `IOS` \| `ANDROID` |
| `packageName` | `String?` | required on Android, not sent on iOS; omitted if null |

### `SubscriptionModel.fromJson`
| Field | Key | Type |
|---|---|---|
| `plan` | `plan` | `String` (required) |
| `status` | `status` | `String` (required) |
| `expiresDate` | `expiryDate` | `String?` → `DateTime.parse` |
| `accessToken` | `accessToken` | `String?` |
| `refreshToken` | `refreshToken` | `String?` |

---

## 7. CHARTS: no datasource of its own

`ChartsRepositoryImpl.getMonthlySummary(month)` → `DebtRemoteDatasource.getSummary(month)` (`GET /debt/summary?month=`), raw map → `MonthlySummaryModel.fromJson`.

### `MonthlySummaryModel.fromJson` (tolerant parsing: every numeric field accepts `num|String`)
| Field | Key | Type |
|---|---|---|
| `month` | `month` | `String` (default `''`) |
| `totalPaid` | `totalPaid` | `double` (default 0) |
| `paidCount` | `paidCount` | `int` (default 0) |
| `skippedCount` | `skippedCount` | `int` (default 0) |
| `skippedAmount` | `skippedAmount` | `double` (default 0) |
| `pendingCount` | `pendingCount` | `int` (default 0) |
| `pendingAmount` | `pendingAmount` | `double` (default 0) |
| `incomeAtMonth` | `incomeAtMonth` | `double?` |
| `debtToIncomeRatio` | `debtToIncomeRatio` | `double?` |
| `debts` | `debts` | `[ MonthlyDebtItem ]` (default `[]`) |
| `trend` | `trend` | `[ TrendPoint ]` (default `[]`) |
| `categoryBreakdown` | `categoryBreakdown` | `[ CategoryBreakdownItem ]` (default `[]`) |
| `debtProgress` | `debtProgress` | `[ DebtProgressItem ]` (default `[]`) |

**`debts[]` (MonthlyDebtItemModel)**: `debtId: String`, `debtName: String`, `currency: String` (default `TRY`), `amountPaid: double?`, `paid: bool`, `skipped: bool`, `pending: bool`, `paidTime: String?` → `DateTime`.

**`trend[]` (TrendPointModel)**: `month: String`, `totalPaid: double`, `paidCount: int`, `skippedCount: int`, `pendingCount: int`, `debtToIncomeRatio: double?`.

**`categoryBreakdown[]` (CategoryBreakdownItemModel)**: `categoryId: String`, `categoryName: String`, `totalPaid: double`, `debtCount: int`, `percentage: double`.

**`debtProgress[]` (DebtProgressItemModel)**: `debtId: String`, `debtName: String`, `currency: String` (default TRY), `totalAmount: double`, `totalPaid: double`, `remainingAmount: double?`, `paidInstallments: int`, `totalInstallments: int?`, `completionPercentage: double?`, `status: String` (default `ACTIVE`).

---

## 8. NOTIFICATION: `NotificationService` (no datasource, uses `dioClientProvider` directly)

| Function | Method + Path | Request body | Response / errors |
|---|---|---|---|
| `registerToken` | `PATCH /notification/device-token` | `{ token: String, email?: String }` (`email` omitted if null) | ignored; all errors are caught, logged with `debugPrint`, and otherwise silent |
| `unregisterToken` | `DELETE /notification/device-token` | none | silent `catch` |

> These two calls do **not** use `ApiException` mapping; errors are swallowed.

---

## 9. EXCHANGE RATES: `debt_providers.dart` (no datasource, uses `dioClientProvider` directly)

| Provider | Method + Path | Query/Path | Response |
|---|---|---|---|
| `exchangeRatesProvider` | `GET /rates/{base}` | path: `base` (base currency code, e.g. `TRY`) | `{ rates: { <CUR>: num } }` or directly `{ <CUR>: num }`: `body['rates'] ?? body`, every value `num`→`double` |

> Does **not** use `ApiException` mapping: a raw `dio.get`, errors surface as a Riverpod `AsyncError`.

---

## 10. Summary: all endpoints

| # | Method | Path | Source |
|---|---|---|---|
| 1 | POST | `/auth/login` | auth |
| 2 | POST | `/auth/register` | auth |
| 3 | POST | `/auth/refresh-token` | auth + AuthInterceptor |
| 4 | POST | `/auth/logout` | auth |
| 5 | POST | `/auth/forget-password` | auth |
| 6 | POST | `/auth/reset-password` | auth |
| 7 | POST | `/auth/change-password` | auth |
| 8 | GET | `/auth/user-detail` | auth |
| 9 | GET | `/debt/get?month&isOwner` | debt |
| 10 | GET | `/debt/summary?month` | debt / charts |
| 11 | GET | `/debt/{id}` | debt |
| 12 | POST | `/debt/save` | debt |
| 13 | POST | `/debt/update` | debt |
| 14 | POST | `/debt/pay/{id}` | debt |
| 15 | POST | `/debt/skip/{id}` | debt |
| 16 | POST | `/debt/pay/{id}/bulk` | debt |
| 17 | DELETE | `/debt/delete/{id}` | debt |
| 18 | GET | `/category/getAll` | category |
| 19 | POST | `/category/save` | category |
| 20 | POST | `/category/update` | category |
| 21 | DELETE | `/category/delete/{id}` | category |
| 22 | POST | `/category/{categoryId}/member` | category |
| 23 | POST | `/category/members/bulk` | category |
| 24 | DELETE | `/category/{categoryId}/member/{userId}` | category |
| 25 | DELETE | `/category/{id}/migrate?targetCategoryId` | category |
| 26 | POST | `/income/save` | income |
| 27 | GET | `/income/history` | income |
| 28 | GET | `/income/current` | income |
| 29 | DELETE | `/income/delete/{id}` | income |
| 30 | GET | `/subscription/status` | subscription |
| 31 | POST | `/subscription/verify` | subscription |
| 32 | PATCH | `/notification/device-token` | notification |
| 33 | DELETE | `/notification/device-token` | notification |
| 34 | GET | `/rates/{base}` | exchange rates |
