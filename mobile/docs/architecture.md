# dt_mobile — Mimari Özet

_Amaç: yeni oturumda tüm repoyu baştan taramadan önce hızlı bilgilenme. Detaylar
için `.claude/CLAUDE.md` (klasör haritası, komutlar, uyarılar) ve
`docs/api-contracts.md` (backend sözleşmesi) ile birlikte oku._

## Katmanlar
Feature-first + `data`/`domain`/`presentation`. Her feature: `data/{datasources,models,repositories}`,
`domain/{entities,repositories,usecases}`, `presentation/{screens,widgets,providers}`.
Çoğu feature `usecases` katmanını atlayıp provider'dan doğrudan repository'yi çağırıyor
(auth `RegisterUsecase`/`LoginUsecase` var ama fiilen kullanılmıyor — provider repository'yi
direkt çağırıyor; silinmedi, dokunulmadı).

## Feature'lar ve ekranlar
| Feature | Ekranlar | Ana provider(lar) |
|---|---|---|
| auth | splash, login, register, forgot-password, reset-password, change-password | `auth_provider.dart` (authNotifierProvider — login/register/logout/refresh state), `change_password_provider.dart` |
| debt | debt_list, debt_detail, debt_form | `debt_providers.dart` (grouped list, create/update/pay/skip/bulkPay/delete) |
| category | category_list, category_detail | `category_providers.dart` (CRUD + member yönetimi) |
| income | income (liste + ekle) | `income_providers.dart` |
| subscription | premium_screen | `subscription_providers.dart` (status + IAP verify) |
| charts | charts_screen | `charts_providers.dart` → `DebtRemoteDatasource.getSummary` |
| settings | settings_screen | `app_settings_provider.dart` (core) — dil/tema |

Ortak: `core/providers/current_user_provider.dart` (JWT'den userId/email çıkarır),
`shared/providers/` (bottom nav vb. UI state).

## Routing (go_router, `core/router/app_router.dart` + `route_names.dart`)
`RouteNames`/`RoutePaths` sabitleri: splash, login, register, forgot-password,
reset-password, debts (+new/detail/edit), categories (+detail), settings,
change-password, income, charts, premium. Redirect mantığı auth state'e bakar
(`authNotifierProvider`). **Debug log'u `kDebugMode` guard'lı — prod'da kapalı,
bu guard'ı bozma** (bkz. CLAUDE.md).

## Network katmanı
`core/network/dio_client.dart`: `dioClientProvider` (AuthInterceptor + debug'da
LoggingInterceptor) ve `refreshDioProvider` (interceptor'sız — login/register/
refresh/logout için). `AuthInterceptor` 401/403 + gateway `TOKEN_EXPIRED`/
`TOKEN_INVALID`/`AUTH_HEADER_*` kodlarını yakalayıp refresh-then-retry veya
force-logout yapar (bkz. `auth_interceptor.dart` içindeki F5 yorumu).
`core/network/api_exception.dart` tüm hataları tipli exception'lara çevirir,
`core/l10n/exception_l10n.dart` bunları locale'e göre mesaja çevirir — yeni bir
backend error code'u eklerken **her ikisini de** güncelle + `l10n/app_*.arb`'a
key ekle + `flutter gen-l10n`.

## State/codegen notları
- Riverpod: `@riverpod` + `riverpod_generator` — provider eklerken/değiştirirken
  `dart run build_runner build --delete-conflicting-outputs` şart.
- Modeller: freezed + json_serializable, aynı komutla üretilir.
- i18n: `lib/l10n/app_tr.arb` (template) + `app_en.arb` → `flutter gen-l10n` →
  `lib/gen/l10n/` (elle düzenlenmez).

## Güncelleme kuralı
Önemli bir mimari/sözleşme değişikliğinden (yeni feature, provider yapısı
değişimi, route eklenmesi) sonra bu dosyayı ve `docs/api-contracts.md`'yi kısa
tut, güncelle. Uzun geçmiş/gerekçe için `HANDOFF.md`'ye bak, burayı şişirme.
