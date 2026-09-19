# dt_mobile — Dio API Katmanı Envanteri (api-contracts)

_Kaynak: `lib/` — son güncelleme 2026-09-19, commit `9ea7c0b`. Sadece mevcut hal
raporlanmıştır. Mimari/ekran özeti için `docs/architecture.md`'ye bak._

## 0. Altyapı

| Öğe | Değer |
|---|---|
| Base URL | `String.fromEnvironment('API_BASE_URL', default: 'http://198.51.100.10:30080')` |
| Sabit headers | `Content-Type: application/json`, `Accept: application/json`, `Accept-Language: <locale?.languageCode ?? 'tr'>` |
| connect/receive timeout | 30 sn / 30 sn |
| `dioClientProvider` | Base + `AuthInterceptor` (+ `LoggingInterceptor` yalnız debug) |
| `refreshDioProvider` | Base, interceptor YOK (login/register/refresh/logout ve retry için) |
| Auth header | `AuthInterceptor.onRequest` → `Authorization: Bearer <accessToken>` (önce in-memory auth state, yoksa 3 sn timeout'lu Keychain okuması) |

### Token yenileme (AuthInterceptor.onError)
- **Tetikleyici (`_isTokenError`)**: HTTP 401 → daima; HTTP 403 → yalnızca gövde `Map` ve `data['code'] ?? data['error'] ?? data['message']` string'i `"JWT"` içeriyorsa.
- Refresh çağrısı: `POST /auth/refresh-token`, header `Authorization: Bearer <refreshToken>`, gövde yok.
- Beklenen refresh yanıtı: `{ "token": string, "refreshToken": string? }` (`refreshToken` yoksa eski değer korunur).
- `requestOptions.extra['_retry'] == true` ise tekrar denenmez, logout edilmez.
- Logout yalnızca refresh POST'unun kendisi 400/401/403 dönerse yapılır.
- Eşzamanlı refresh koruması: in-memory access token, isteğin `Authorization`'ından farklıysa 2. refresh yapılmadan retry.

## 1. Hata (error) JSON parse — `ApiException.fromDioException(DioException e)`

Tüm datasource'lar `on DioException` yakalar → `ApiException.fromDioException`. Mantık (commit `e9b8d7c` sonrası — C2):

**Önce `data['code']` (status'ten bağımsız):**

| `code` değeri | Fırlatılan tip |
|---|---|
| `error.debt.limit_exceeded`, `error.category.limit_exceeded`, `error.category.premium_required`, `error.debt.premium_required` | `PremiumLimitException` (upsell sheet) |
| `error.debt.permission_denied`, `error.category.permission_denied`, `error.income.permission_denied` | `PermissionDeniedException` (düz hata, upsell YOK) |
| `error.user.not_found_in_context` | `UnauthorizedException` |
| `error.debt.already_completed` (409), `error.debt.overpayment` (400), `error.category.name_conflict` (409), `error.category.cannot_add_self` (409), `error.category.member_not_found` (404), + 2026-09-19'da eklenen 24 code (`error.category.{not_found,member_user_not_found,member_already_exists,migrate_same_category}`, `error.debt.{not_found,already_paid_this_period,auto_payment_enabled,concurrent_request,payment_before_start,period_already_recorded,installment_count_exceeded,missing_period_config}`, `error.income.not_found`, `error.reminder.date_or_time_missing`, `error.subscription.{verification_failed,not_found}`, `error.auth.{email_in_use,too_many_login_attempts,not_valid,token_revoked,token_invalid,current_password_invalid,token_already_used,too_many_password_reset_attempts}`) | `BusinessException(message, code: <code>)` — `exception_l10n.dart` lokalize kopyaya çevirir, yoksa backend `message`'a düşer |

**`code` yoksa / tanınmıyorsa — status fallback:**

| Koşul | Okunan alan(lar) | Fırlatılan tip |
|---|---|---|
| gövde `Map<String,dynamic>` & status 400 | `data['message']` | `BusinessException(message ?? 'Invalid request.')` |
| gövde `Map` & status 401 | `data['message']` | `UnauthorizedException` |
| gövde `Map` & status 403 | `data['message']` | `PremiumLimitException` (pre-C2 davranışı korunuyor) |
| gövde `Map` & status 409 | `data['message']` | `BusinessException(message ?? 'This operation cannot be completed right now.')` |
| gövde `Map` & status 422 | `data['errors'][0]['message']` yoksa `data['message']` | `BusinessException` |
| status 404 | — | `NotFoundException` |
| status 429 | — | `RateLimitException` |
| status >= 500 | `data['message']` (Map ise) | `ServerException` |
| `DioExceptionType.connectionTimeout/receiveTimeout/connectionError` | — | `NetworkException` |
| diğer | — | `UnknownException` |

> Not: Beklenen sözleşme hata gövdesi `{ "message": string, "code"?: string, "errors"?: [{ "message": string }] }`. AuthInterceptor ayrıca gateway formatı `{ "error": string, "status": int }` ve servis formatı `{ "code": string }` okur.
>
> **C2-optional (uygulandı + backend debt-service v0.0.20'ye karşı E2E doğrulandı):** 5 iş kodu (`error.debt.already_completed`, `error.debt.overpayment`, `error.category.name_conflict`, `error.category.cannot_add_self`, `error.category.member_not_found`) `_businessCodes` setinde tanınıyor → `BusinessException.code` taşıyor → `exception_l10n._businessCodeMessage` lokalize kopya döndürüyor (`errorDebtAlreadyCompleted` vb. arb anahtarları). NOT: backend kodu `already_paid` değil `already_completed` (orchestrator listesi yanlıştı, E2E'de yakalandı). `invalid_migration_target` / `income period_overlap` kullanıcı kararıyla eklenmedi.
>
> **2026-09-19 turu:** `error.auth.email_in_use` dahil 24 yeni code `_businessCodes`'a
> eklendi ve hepsi kendi lokalize mesajına kavuştu (önceki not — "email_in_use jenerik
> BusinessException" — artık geçersiz). Bkz. `HANDOFF.md` 2026-09-19 mobile-agent girişi.

---

## 2. AUTH — `AuthRemoteDatasource`

Dio: `login/register/refreshToken/logout/forgotPassword/resetPassword` → `refreshDioProvider` (interceptor'sız); `changePassword/userDetail` → `dioClientProvider`.

| Fonksiyon | Metot + Yol | Request body (alan: tip) | Query/Path | Yanıt modeli (alan: tip) |
|---|---|---|---|---|
| `login` | `POST /auth/login` | `email: String`, `password: String` | — | `AuthResponse { token→accessToken: String, refreshToken: String }` |
| `register` | `POST /auth/register` | `email: String`, `password: String` (`premium` alanı 2026-09-19'da kaldırıldı — backend DTO'sunda karşılığı yoktu) | — | `AuthResponse` (aynı) |
| `refreshToken` | `POST /auth/refresh-token` | gövde yok; header `Authorization: Bearer <refreshToken>` | — | `AuthResponse` (aynı) |
| `logout` | `POST /auth/logout` | gövde yok; header `Authorization: Bearer <refreshToken>` | — | — (void) |
| `forgotPassword` | `POST /auth/forget-password` | `email: String` | — | — (void) |
| `resetPassword` | `POST /auth/reset-password` | `token: String`, `newPassword: String` | — | — (void) |
| `changePassword` | `POST /auth/change-password` | `currentPassword: String`, `newPassword: String` | — | — (void) |
| `userDetail` | `GET /auth/user-detail` | — | — | elle parse: `email: String` (zorunlu), `premium: bool?` (default false), `createdAt: String` → `DateTime.parse` |

Tanımlı ama **kullanılmayan** sabit: `/auth` altında yok. `RefreshTokenRequest` modeli var ama datasource header kullanıyor, gövde değil.

---

## 3. DEBT — `DebtRemoteDatasource` (hepsi `dioClientProvider`)

| Fonksiyon | Metot + Yol | Request body | Query/Path | Yanıt |
|---|---|---|---|---|
| `getGrouped` | `GET /debt/get` | — | query: `month: String` (`YYYY-MM`), `isOwner: bool` | `{ categories: [ { categoryId: String?, categoryName: String?, owned: bool?(def true), ownerUserId: String?, debts: [DebtSummaryModel] } ] }` |
| `getSummary` | `GET /debt/summary` | — | query: `month: String` (`YYYY-MM`) | ham `Map<String,dynamic>` → `MonthlySummaryModel` (bkz. §7) |
| `getById` | `GET /debt/{id}` | — | path: `id` | `DebtModel` (sarmalayıcısız, direkt) |
| `create` | `POST /debt/save` | `CreateDebtRequest` (aşağıda) | — | `{ debt: DebtModel }` — `debt` yoksa `FormatException` |
| `update` | `POST /debt/update` | `CreateDebtRequest` (+ `id`) | — | `{ debt: DebtModel }` |
| `pay` | `POST /debt/pay/{id}` | `{ amountPaid?: double, paidTime?: String(`yyyy-MM-ddTHH:mm:ss`) }` — her ikisi de yoksa gövde `null` | path: `id` | `{ debt: DebtModel }` |
| `skip` | `POST /debt/skip/{id}` | gövde yok | path: `id` | `{ debt: DebtModel }` |
| `bulkPay` | `POST /debt/pay/{id}/bulk` | JSON **dizi**: `[ { amountPaid: double, paidTime: String(`yyyy-MM-ddTHH:mm:ss`) } ]` | path: `id` | `{ debt: DebtModel }` |
| `delete` | `DELETE /debt/delete/{id}` | — | path: `id` | — (void) |

### `CreateDebtRequest.toJson` (create/update body)
| Alan | Tip | Not |
|---|---|---|
| `id` | `String?` | null ise atlanır (update'te dolu) |
| `name` | `String` | zorunlu |
| `categoryId` | `String` | zorunlu |
| `description` | `String?` | null ise atlanır |
| `firstPaymentDate` | `String` | format `yyyy-MM-ddTHH:mm:ss` (zone/ms yok) |
| `installmentCount` | `int?` | null ise atlanır |
| `paymentPeriodType` | `String` enum | `ONE_TIME` `DAILY` `WEEKLY` `BI_WEEKLY` `MONTHLY` `QUARTERLY` `HALF_YEARLY` `YEARLY` |
| `amount` | `double` | zorunlu |
| `autoPayment` | `bool` | daima gönderilir (default false) |
| `currency` | `String` | daima gönderilir (yoksa `'TRY'`) |
| `reminder` | `object?` | yalnız `reminder != null && reminder.enabled` iken; kapalıysa alan komple çıkarılır |
| `reminder.enabled` | `bool` | |
| `reminder.channels` | `List<String>` | ör. `EMAIL`, `PUSH` (ReminderChannel.toJson) |
| `reminder.reminderDate` | `String?` | format `yyyy-MM-dd`; null ise atlanır |
| `reminder.reminderTime` | `String?` | `HH:mm` metni; null ise atlanır |

### `DebtModel.fromJson` (yanıt — `debt` sarmalayıcı içinden)
| Alan | JSON anahtarı | Tip |
|---|---|---|
| `id` | `id` | `String` (zorunlu) |
| `name` | `name` | `String` (zorunlu) |
| `ownerId` | `ownerId` | `String` (zorunlu) |
| `categoryId` | `categoryId` | `String?` |
| `description` | `description` | `String?` |
| `firstPaymentDate` | `firstPaymentDate` | `String?` (parse `DateTime`) |
| `installmentCount` | `installmentCount` | `int?` |
| `paidInstallmentCount` | `paidInstallmentCount` | `int?` |
| `paymentPeriodType` | `paymentPeriodType` | enum string (yukarıdaki set) — zorunlu |
| `amount` | `amount` | `num | String` → `double` (tolerant) |
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

### `DebtSummaryModel.fromJson` (getGrouped `categories[].debts[]`)
| Alan | Anahtar | Tip |
|---|---|---|
| `id` | `id` | `String` |
| `name` | `name` | `String` |
| `categoryId` | `categoryId` | `String` |
| `installmentCount` | `installmentCount` | `int?` |
| `paidCount` | `paidCount` | `int` (default 0) |
| `paymentPeriodType` | `paymentPeriodType` | enum string |
| `amount` | `amount` | `num|String` → `double` |
| `currency` | `currency` | `String` (zorunlu) |
| `status` | `status` | `ACTIVE`\|`COMPLETED` (def ACTIVE) |
| `paymentStatus` | `paymentStatus` | enum (def UNPAID) |
| `autoPayment` | `autoPayment` | `bool` (def false) |
| `paidCurrentPeriod` | `paidCurrentPeriod` | `bool` (def false) |
| `hasReminder` | `hasReminder` | `bool` (def false) |

Tanımlı ama kullanılmayan sabitler: `debtGetById`/`debtPayments` (=`/debt`), `debtBulkPay` (=`/debt/pay`).

---

## 4. CATEGORY — `CategoryRemoteDatasource` (`dioClientProvider`)

| Fonksiyon | Metot + Yol | Request body | Query/Path | Yanıt |
|---|---|---|---|---|
| `getAll` | `GET /category/getAll` | — | — | JSON **dizi** `[CategoryModel]` |
| `create` | `POST /category/save` | `{ name: String }` | — | `CategoryModel` (backend `ownerId` döndürmüyor → client `''` placeholder ekliyor, repo gerçek değeri yazıyor) |
| `update` | `POST /category/update` | `{ id: String, name: String }` | — | `CategoryModel` (aynı `ownerId` durumu) |
| `delete` | `DELETE /category/delete/{id}` | — | path: `id` | — (void) |
| `addMember` | `POST /category/{categoryId}/member` | `AddMemberRequest { email: String, accessLevel: String }` | path: `categoryId` | — (void) |
| `addMembersBulk` | `POST /category/members/bulk` | `AddMembersBulkRequest { categoryIds: List<String>, email: String, accessLevel: String }` | — | — (void) |
| `removeMember` | `DELETE /category/{categoryId}/member/{userId}` | — | path: `categoryId`, `userId` | — (void) |
| `migrateAndDelete` | `DELETE /category/{id}/migrate` | — | path: `id`; query: `targetCategoryId: String` | `{ migratedDebtCount: int }` |

`accessLevel` enum wire değerleri: `READ` `WRITE` `EDIT` `DELETE` (domain `admin` ↔ wire `DELETE`).

### `CategoryModel.fromJson`
| Alan | Anahtar | Tip |
|---|---|---|
| `id` | `id` | `String` (zorunlu) |
| `ownerId` | `ownerId` | `String` (zorunlu — backend save/update yanıtında YOK, client patch'liyor) |
| `name` | `name` | `String` (zorunlu) |
| `members` | `members` | `[ MemberEmbeddedModel ]` (default `[]`) |
| `createdAt` | `createdAt` | `String?` |
| `updatedAt` | `updatedAt` | `String?` |

### `MemberEmbeddedModel.fromJson`
| Alan | Anahtar | Tip |
|---|---|---|
| `userId` | `userId` | `String?` (**opsiyonel** — bilinen backend eksikliği, App Store blocker) |
| `accessLevel` | `accessLevel` | enum string `READ/WRITE/EDIT/DELETE` (zorunlu) |
| `email` | `email` | `String?` |

Tanımlı ama kullanılmayan sabitler: `categoryTransfer`, `categoryMemberRemove` (=`/category`; kod string'i elle kuruyor).

---

## 5. INCOME — `IncomeRemoteDatasource` (`dioClientProvider`)

| Fonksiyon | Metot + Yol | Request body | Query/Path | Yanıt |
|---|---|---|---|---|
| `save` | `POST /income/save` | `SaveIncomeRequest { amount: double, currency: String, effectiveFrom: String, note?: String }` | — | `IncomeModel` |
| `getHistory` | `GET /income/history` | — | — | JSON **dizi** `[IncomeModel]` |
| `getCurrent` | `GET /income/current` | — | — | `IncomeModel` |
| `delete` | `DELETE /income/delete/{id}` | — | path: `id` | — (void) |

### `IncomeModel.fromJson`
| Alan | Anahtar | Tip |
|---|---|---|
| `id` | `id` | `String` (zorunlu) |
| `amount` | `amount` | `num|String` → `double` |
| `currency` | `currency` | `String` (zorunlu) |
| `effectiveFrom` | `effectiveFrom` | `String` → `DateTime.parse` (zorunlu) |
| `note` | `note` | `String?` |
| `createdAt` | `createdDate` | `String?` |

> `effectiveFrom` request'te String olarak gönderiliyor (çağıran katman formatlıyor; muhtemelen `yyyy-MM-dd`).

---

## 6. SUBSCRIPTION — `SubscriptionRemoteDatasource` (`dioClientProvider`)

| Fonksiyon | Metot + Yol | Request body | Yanıt |
|---|---|---|---|
| `getStatus` | `GET /subscription/status` | — | `SubscriptionModel` |
| `verifyReceipt` | `POST /subscription/verify` | `VerifyReceiptRequest` (aşağıda) | `SubscriptionModel` |

### `VerifyReceiptRequest.toJson`
| Alan | Tip | Not |
|---|---|---|
| `receiptData` | `String` | zorunlu |
| `plan` | `String` | zorunlu |
| `productId` | `String` | zorunlu |
| `platform` | `String` | zorunlu — `IOS` \| `ANDROID` |
| `packageName` | `String?` | Android'de zorunlu, iOS'ta gönderilmez; null ise atlanır |

### `SubscriptionModel.fromJson`
| Alan | Anahtar | Tip |
|---|---|---|
| `plan` | `plan` | `String` (zorunlu) |
| `status` | `status` | `String` (zorunlu) |
| `expiresDate` | `expiryDate` | `String?` → `DateTime.parse` |
| `accessToken` | `accessToken` | `String?` |
| `refreshToken` | `refreshToken` | `String?` |

---

## 7. CHARTS — datasource yok

`ChartsRepositoryImpl.getMonthlySummary(month)` → `DebtRemoteDatasource.getSummary(month)` (`GET /debt/summary?month=`) ham map → `MonthlySummaryModel.fromJson`.

### `MonthlySummaryModel.fromJson` (tolerant parse, tüm sayısallar `num|String` kabul eder)
| Alan | Anahtar | Tip |
|---|---|---|
| `month` | `month` | `String` (def `''`) |
| `totalPaid` | `totalPaid` | `double` (def 0) |
| `paidCount` | `paidCount` | `int` (def 0) |
| `skippedCount` | `skippedCount` | `int` (def 0) |
| `skippedAmount` | `skippedAmount` | `double` (def 0) |
| `pendingCount` | `pendingCount` | `int` (def 0) |
| `pendingAmount` | `pendingAmount` | `double` (def 0) |
| `incomeAtMonth` | `incomeAtMonth` | `double?` |
| `debtToIncomeRatio` | `debtToIncomeRatio` | `double?` |
| `debts` | `debts` | `[ MonthlyDebtItem ]` (def `[]`) |
| `trend` | `trend` | `[ TrendPoint ]` (def `[]`) |
| `categoryBreakdown` | `categoryBreakdown` | `[ CategoryBreakdownItem ]` (def `[]`) |
| `debtProgress` | `debtProgress` | `[ DebtProgressItem ]` (def `[]`) |

**`debts[]` (MonthlyDebtItemModel)**: `debtId: String`, `debtName: String`, `currency: String`(def `TRY`), `amountPaid: double?`, `paid: bool`, `skipped: bool`, `pending: bool`, `paidTime: String?`→`DateTime`.

**`trend[]` (TrendPointModel)**: `month: String`, `totalPaid: double`, `paidCount: int`, `skippedCount: int`, `pendingCount: int`, `debtToIncomeRatio: double?`.

**`categoryBreakdown[]` (CategoryBreakdownItemModel)**: `categoryId: String`, `categoryName: String`, `totalPaid: double`, `debtCount: int`, `percentage: double`.

**`debtProgress[]` (DebtProgressItemModel)**: `debtId: String`, `debtName: String`, `currency: String`(def TRY), `totalAmount: double`, `totalPaid: double`, `remainingAmount: double?`, `paidInstallments: int`, `totalInstallments: int?`, `completionPercentage: double?`, `status: String`(def `ACTIVE`).

---

## 8. NOTIFICATION — `NotificationService` (datasource'suz, doğrudan `dioClientProvider`)

| Fonksiyon | Metot + Yol | Request body | Yanıt / hata |
|---|---|---|---|
| `registerToken` | `PATCH /notification/device-token` | `{ token: String, email?: String }` (`email` null ise atlanır) | yok sayılır; tüm hatalar `catch` → `debugPrint`, sessiz |
| `unregisterToken` | `DELETE /notification/device-token` | — | sessiz `catch` |

> Bu iki çağrı `ApiException` mapping **kullanmaz** — hata yutulur.
> Tanımlı ama kullanılmayan sabit: `notificationTest` (`/notification/test`).

---

## 9. EXCHANGE RATES — `debt_providers.dart` (datasource'suz, doğrudan `dioClientProvider`)

| Provider | Metot + Yol | Query/Path | Yanıt |
|---|---|---|---|
| `exchangeRatesProvider` | `GET /rates/{base}` | path: `base` (baz para birimi kodu, ör. `TRY`) | `{ rates: { <CUR>: num } }` veya doğrudan `{ <CUR>: num }` — `body['rates'] ?? body`, tüm değerler `num`→`double` |

> `ApiException` mapping **kullanmaz** — ham `dio.get`, hata Riverpod AsyncError'a düşer.

---

## 10. Özet — tüm endpoint listesi

| # | Metot | Yol | Kaynak |
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
