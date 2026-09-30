import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_tr.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('tr'),
  ];

  /// No description provided for @appName.
  ///
  /// In tr, this message translates to:
  /// **'Kamci'**
  String get appName;

  /// No description provided for @save.
  ///
  /// In tr, this message translates to:
  /// **'Kaydet'**
  String get save;

  /// No description provided for @cancel.
  ///
  /// In tr, this message translates to:
  /// **'İptal'**
  String get cancel;

  /// No description provided for @delete.
  ///
  /// In tr, this message translates to:
  /// **'Sil'**
  String get delete;

  /// No description provided for @edit.
  ///
  /// In tr, this message translates to:
  /// **'Düzenle'**
  String get edit;

  /// No description provided for @create.
  ///
  /// In tr, this message translates to:
  /// **'Oluştur'**
  String get create;

  /// No description provided for @update.
  ///
  /// In tr, this message translates to:
  /// **'Güncelle'**
  String get update;

  /// No description provided for @send.
  ///
  /// In tr, this message translates to:
  /// **'Gönder'**
  String get send;

  /// No description provided for @remove.
  ///
  /// In tr, this message translates to:
  /// **'Kaldır'**
  String get remove;

  /// No description provided for @add.
  ///
  /// In tr, this message translates to:
  /// **'Ekle'**
  String get add;

  /// No description provided for @select.
  ///
  /// In tr, this message translates to:
  /// **'Seçin'**
  String get select;

  /// No description provided for @retry.
  ///
  /// In tr, this message translates to:
  /// **'Tekrar Dene'**
  String get retry;

  /// No description provided for @welcome.
  ///
  /// In tr, this message translates to:
  /// **'Hoş Geldiniz!'**
  String get welcome;

  /// No description provided for @signIn.
  ///
  /// In tr, this message translates to:
  /// **'Giriş Yap'**
  String get signIn;

  /// No description provided for @signUp.
  ///
  /// In tr, this message translates to:
  /// **'Kayıt Ol'**
  String get signUp;

  /// No description provided for @signOut.
  ///
  /// In tr, this message translates to:
  /// **'Çıkış Yap'**
  String get signOut;

  /// No description provided for @signOutConfirm.
  ///
  /// In tr, this message translates to:
  /// **'Hesabınızdan çıkış yapmak istediğinize emin misiniz?'**
  String get signOutConfirm;

  /// No description provided for @email.
  ///
  /// In tr, this message translates to:
  /// **'E-posta Adresi'**
  String get email;

  /// No description provided for @password.
  ///
  /// In tr, this message translates to:
  /// **'Şifre'**
  String get password;

  /// No description provided for @confirmPassword.
  ///
  /// In tr, this message translates to:
  /// **'Şifre Tekrar'**
  String get confirmPassword;

  /// No description provided for @newPassword.
  ///
  /// In tr, this message translates to:
  /// **'Yeni Şifre'**
  String get newPassword;

  /// No description provided for @forgotPassword.
  ///
  /// In tr, this message translates to:
  /// **'Şifremi Unuttum?'**
  String get forgotPassword;

  /// No description provided for @forgotPasswordTitle.
  ///
  /// In tr, this message translates to:
  /// **'Şifremi Unuttum'**
  String get forgotPasswordTitle;

  /// No description provided for @resetPasswordTitle.
  ///
  /// In tr, this message translates to:
  /// **'Şifre Sıfırla'**
  String get resetPasswordTitle;

  /// No description provided for @updatePassword.
  ///
  /// In tr, this message translates to:
  /// **'Şifreyi Güncelle'**
  String get updatePassword;

  /// No description provided for @noAccount.
  ///
  /// In tr, this message translates to:
  /// **'Hesabınız yok mu?'**
  String get noAccount;

  /// No description provided for @hasAccount.
  ///
  /// In tr, this message translates to:
  /// **'Zaten hesabınız var mı?'**
  String get hasAccount;

  /// No description provided for @forgotPasswordDesc.
  ///
  /// In tr, this message translates to:
  /// **'Şifre sıfırlama bağlantısı e-posta adresinize gönderilecek.'**
  String get forgotPasswordDesc;

  /// No description provided for @resetPasswordDesc.
  ///
  /// In tr, this message translates to:
  /// **'Yeni şifrenizi belirleyin.'**
  String get resetPasswordDesc;

  /// No description provided for @checkEmail.
  ///
  /// In tr, this message translates to:
  /// **'E-postanızı Kontrol Edin'**
  String get checkEmail;

  /// No description provided for @checkEmailDesc.
  ///
  /// In tr, this message translates to:
  /// **'{email} adresine şifre sıfırlama bağlantısı gönderildi.'**
  String checkEmailDesc(String email);

  /// No description provided for @invalidResetLink.
  ///
  /// In tr, this message translates to:
  /// **'Geçersiz şifre sıfırlama bağlantısı.'**
  String get invalidResetLink;

  /// No description provided for @passwordUpdated.
  ///
  /// In tr, this message translates to:
  /// **'Şifreniz başarıyla güncellendi.'**
  String get passwordUpdated;

  /// No description provided for @debts.
  ///
  /// In tr, this message translates to:
  /// **'Borçlar'**
  String get debts;

  /// No description provided for @categories.
  ///
  /// In tr, this message translates to:
  /// **'Kategoriler'**
  String get categories;

  /// No description provided for @myDebts.
  ///
  /// In tr, this message translates to:
  /// **'Kendi Borçlarım'**
  String get myDebts;

  /// No description provided for @sharedDebts.
  ///
  /// In tr, this message translates to:
  /// **'Paylaşılan'**
  String get sharedDebts;

  /// No description provided for @noDebtsYet.
  ///
  /// In tr, this message translates to:
  /// **'Henüz borç yok'**
  String get noDebtsYet;

  /// No description provided for @noSharedDebts.
  ///
  /// In tr, this message translates to:
  /// **'Paylaşılan borç yok'**
  String get noSharedDebts;

  /// No description provided for @addDebtHint.
  ///
  /// In tr, this message translates to:
  /// **'Yeni borç eklemek için + düğmesine bas'**
  String get addDebtHint;

  /// No description provided for @joinSharedHint.
  ///
  /// In tr, this message translates to:
  /// **'Paylaşılan bir kategoriye üye ol'**
  String get joinSharedHint;

  /// No description provided for @noCategoryDebts.
  ///
  /// In tr, this message translates to:
  /// **'Bu kategoride henüz borç yok'**
  String get noCategoryDebts;

  /// No description provided for @noDebts.
  ///
  /// In tr, this message translates to:
  /// **'Borç yok'**
  String get noDebts;

  /// No description provided for @summaryTotal.
  ///
  /// In tr, this message translates to:
  /// **'Toplam'**
  String get summaryTotal;

  /// No description provided for @deleteDebtPermanentWarning.
  ///
  /// In tr, this message translates to:
  /// **'Bu borç kalıcı olarak silinecek, geri alınamaz. Emin misiniz?'**
  String get deleteDebtPermanentWarning;

  /// No description provided for @paymentRecorded.
  ///
  /// In tr, this message translates to:
  /// **'{name} ödemeniz kayıt edildi.'**
  String paymentRecorded(String name);

  /// No description provided for @deleteDebt.
  ///
  /// In tr, this message translates to:
  /// **'Borcu Sil'**
  String get deleteDebt;

  /// No description provided for @deleteDebtConfirm.
  ///
  /// In tr, this message translates to:
  /// **'\"{name}\" borcunu silmek istediğinize emin misiniz?'**
  String deleteDebtConfirm(String name);

  /// No description provided for @amount.
  ///
  /// In tr, this message translates to:
  /// **'Tutar'**
  String get amount;

  /// No description provided for @status.
  ///
  /// In tr, this message translates to:
  /// **'Durum'**
  String get status;

  /// No description provided for @paymentPeriod.
  ///
  /// In tr, this message translates to:
  /// **'Ödeme Periyodu'**
  String get paymentPeriod;

  /// No description provided for @startDate.
  ///
  /// In tr, this message translates to:
  /// **'Başlangıç'**
  String get startDate;

  /// No description provided for @firstPaymentDate.
  ///
  /// In tr, this message translates to:
  /// **'İlk Ödeme Tarihi'**
  String get firstPaymentDate;

  /// No description provided for @endDate.
  ///
  /// In tr, this message translates to:
  /// **'Bitiş'**
  String get endDate;

  /// No description provided for @description.
  ///
  /// In tr, this message translates to:
  /// **'Açıklama'**
  String get description;

  /// No description provided for @nextReminder.
  ///
  /// In tr, this message translates to:
  /// **'Sonraki Hatırlatma'**
  String get nextReminder;

  /// No description provided for @paymentHistory.
  ///
  /// In tr, this message translates to:
  /// **'Ödeme Geçmişi ({count})'**
  String paymentHistory(int count);

  /// No description provided for @noPaymentsYet.
  ///
  /// In tr, this message translates to:
  /// **'Henüz ödeme yapılmamış'**
  String get noPaymentsYet;

  /// No description provided for @paid.
  ///
  /// In tr, this message translates to:
  /// **'Ödendi'**
  String get paid;

  /// No description provided for @pendingPayment.
  ///
  /// In tr, this message translates to:
  /// **'Bekliyor'**
  String get pendingPayment;

  /// No description provided for @newDebt.
  ///
  /// In tr, this message translates to:
  /// **'Yeni Borç'**
  String get newDebt;

  /// No description provided for @editDebt.
  ///
  /// In tr, this message translates to:
  /// **'Borcu Düzenle'**
  String get editDebt;

  /// No description provided for @debtName.
  ///
  /// In tr, this message translates to:
  /// **'Borç Adı'**
  String get debtName;

  /// No description provided for @selectDate.
  ///
  /// In tr, this message translates to:
  /// **'Seçin'**
  String get selectDate;

  /// No description provided for @selectStartDate.
  ///
  /// In tr, this message translates to:
  /// **'Başlangıç tarihini seçin'**
  String get selectStartDate;

  /// No description provided for @noCategory.
  ///
  /// In tr, this message translates to:
  /// **'Kategori Yok'**
  String get noCategory;

  /// No description provided for @category.
  ///
  /// In tr, this message translates to:
  /// **'Kategori'**
  String get category;

  /// No description provided for @autoPayment.
  ///
  /// In tr, this message translates to:
  /// **'Otomatik Ödeme'**
  String get autoPayment;

  /// No description provided for @categoryOptional.
  ///
  /// In tr, this message translates to:
  /// **'Kategori (İsteğe Bağlı)'**
  String get categoryOptional;

  /// No description provided for @descriptionOptional.
  ///
  /// In tr, this message translates to:
  /// **'Açıklama (İsteğe Bağlı)'**
  String get descriptionOptional;

  /// No description provided for @myCategories.
  ///
  /// In tr, this message translates to:
  /// **'Sahip Olduklarım'**
  String get myCategories;

  /// No description provided for @joinedCategories.
  ///
  /// In tr, this message translates to:
  /// **'Paylaşılanlar'**
  String get joinedCategories;

  /// No description provided for @noCategoriesYet.
  ///
  /// In tr, this message translates to:
  /// **'Henüz kategori yok'**
  String get noCategoriesYet;

  /// No description provided for @noJoinedCategories.
  ///
  /// In tr, this message translates to:
  /// **'Paylaşılan kategori yok'**
  String get noJoinedCategories;

  /// No description provided for @addCategoryHint.
  ///
  /// In tr, this message translates to:
  /// **'Yeni kategori oluşturmak için + düğmesine bas'**
  String get addCategoryHint;

  /// No description provided for @waitingInvitation.
  ///
  /// In tr, this message translates to:
  /// **'Bir kategoriye davet bekleniyor'**
  String get waitingInvitation;

  /// No description provided for @memberCount.
  ///
  /// In tr, this message translates to:
  /// **'{count} üye'**
  String memberCount(int count);

  /// No description provided for @deleteCategory.
  ///
  /// In tr, this message translates to:
  /// **'Kategoriyi Sil'**
  String get deleteCategory;

  /// No description provided for @deleteCategoryConfirm.
  ///
  /// In tr, this message translates to:
  /// **'\"{name}\" kategorisini silmek istediğinize emin misiniz?'**
  String deleteCategoryConfirm(String name);

  /// No description provided for @memberCountTitle.
  ///
  /// In tr, this message translates to:
  /// **'{count} Üye'**
  String memberCountTitle(int count);

  /// No description provided for @noMembersYet.
  ///
  /// In tr, this message translates to:
  /// **'Henüz üye yok'**
  String get noMembersYet;

  /// No description provided for @addMember.
  ///
  /// In tr, this message translates to:
  /// **'Üye Ekle'**
  String get addMember;

  /// No description provided for @removeMember.
  ///
  /// In tr, this message translates to:
  /// **'Üyeyi Kaldır'**
  String get removeMember;

  /// No description provided for @removeMemberConfirm.
  ///
  /// In tr, this message translates to:
  /// **'Bu üyeyi kategoriden kaldırmak istediğinize emin misiniz?'**
  String get removeMemberConfirm;

  /// No description provided for @newCategory.
  ///
  /// In tr, this message translates to:
  /// **'Yeni Kategori'**
  String get newCategory;

  /// No description provided for @editCategory.
  ///
  /// In tr, this message translates to:
  /// **'Kategoriyi Düzenle'**
  String get editCategory;

  /// No description provided for @transferAndDeleteCategory.
  ///
  /// In tr, this message translates to:
  /// **'Borçları Aktarıp Kategoriyi Sil'**
  String get transferAndDeleteCategory;

  /// No description provided for @deleteWithDebts.
  ///
  /// In tr, this message translates to:
  /// **'Borçları ile Kategoriyi Sil'**
  String get deleteWithDebts;

  /// No description provided for @selectTargetCategory.
  ///
  /// In tr, this message translates to:
  /// **'Hedef Kategori Seç'**
  String get selectTargetCategory;

  /// No description provided for @transferAndDeleteConfirm.
  ///
  /// In tr, this message translates to:
  /// **'\"{name}\" kategorisindeki borçlar seçilen kategoriye aktarılacak, ardından kategori silinecek.'**
  String transferAndDeleteConfirm(String name);

  /// No description provided for @deleteWithDebtsConfirm.
  ///
  /// In tr, this message translates to:
  /// **'\"{name}\" kategorisi ve içindeki tüm borçlar silinecek. Bu işlem geri alınamaz.'**
  String deleteWithDebtsConfirm(String name);

  /// No description provided for @migratedDebtCount.
  ///
  /// In tr, this message translates to:
  /// **'{count} borç hedef kategoriye taşındı'**
  String migratedDebtCount(int count);

  /// No description provided for @categoryName.
  ///
  /// In tr, this message translates to:
  /// **'Kategori Adı'**
  String get categoryName;

  /// No description provided for @periodOneTime.
  ///
  /// In tr, this message translates to:
  /// **'Tek Seferlik'**
  String get periodOneTime;

  /// No description provided for @periodDaily.
  ///
  /// In tr, this message translates to:
  /// **'Günlük'**
  String get periodDaily;

  /// No description provided for @periodWeekly.
  ///
  /// In tr, this message translates to:
  /// **'Haftalık'**
  String get periodWeekly;

  /// No description provided for @periodBiWeekly.
  ///
  /// In tr, this message translates to:
  /// **'İki Haftalık'**
  String get periodBiWeekly;

  /// No description provided for @periodMonthly.
  ///
  /// In tr, this message translates to:
  /// **'Aylık'**
  String get periodMonthly;

  /// No description provided for @periodQuarterly.
  ///
  /// In tr, this message translates to:
  /// **'Üç Aylık'**
  String get periodQuarterly;

  /// No description provided for @periodHalfYearly.
  ///
  /// In tr, this message translates to:
  /// **'Altı Aylık'**
  String get periodHalfYearly;

  /// No description provided for @periodYearly.
  ///
  /// In tr, this message translates to:
  /// **'Yıllık'**
  String get periodYearly;

  /// No description provided for @statusActive.
  ///
  /// In tr, this message translates to:
  /// **'Aktif'**
  String get statusActive;

  /// No description provided for @statusInactive.
  ///
  /// In tr, this message translates to:
  /// **'Pasif'**
  String get statusInactive;

  /// No description provided for @statusCompleted.
  ///
  /// In tr, this message translates to:
  /// **'Tamamlandı'**
  String get statusCompleted;

  /// No description provided for @currentPassword.
  ///
  /// In tr, this message translates to:
  /// **'Mevcut Şifre'**
  String get currentPassword;

  /// No description provided for @settings.
  ///
  /// In tr, this message translates to:
  /// **'Ayarlar'**
  String get settings;

  /// No description provided for @account.
  ///
  /// In tr, this message translates to:
  /// **'Hesap'**
  String get account;

  /// No description provided for @changePassword.
  ///
  /// In tr, this message translates to:
  /// **'Şifre Değiştir'**
  String get changePassword;

  /// No description provided for @changePasswordDesc.
  ///
  /// In tr, this message translates to:
  /// **'Şifre sıfırlama bağlantısı e-postanıza gönderilecek'**
  String get changePasswordDesc;

  /// No description provided for @feedback.
  ///
  /// In tr, this message translates to:
  /// **'Geri Bildirim'**
  String get feedback;

  /// No description provided for @sendFeedback.
  ///
  /// In tr, this message translates to:
  /// **'Öneri / Şikayet Gönder'**
  String get sendFeedback;

  /// No description provided for @feedbackDesc.
  ///
  /// In tr, this message translates to:
  /// **'Görüş ve önerilerinizi bizimle paylaşın'**
  String get feedbackDesc;

  /// No description provided for @appearance.
  ///
  /// In tr, this message translates to:
  /// **'Uygulama Ayarları'**
  String get appearance;

  /// No description provided for @language.
  ///
  /// In tr, this message translates to:
  /// **'Dil'**
  String get language;

  /// No description provided for @theme.
  ///
  /// In tr, this message translates to:
  /// **'Tema'**
  String get theme;

  /// No description provided for @themeSystem.
  ///
  /// In tr, this message translates to:
  /// **'Sistem'**
  String get themeSystem;

  /// No description provided for @themeLight.
  ///
  /// In tr, this message translates to:
  /// **'Açık'**
  String get themeLight;

  /// No description provided for @themeDark.
  ///
  /// In tr, this message translates to:
  /// **'Koyu'**
  String get themeDark;

  /// No description provided for @langSystem.
  ///
  /// In tr, this message translates to:
  /// **'Sistem'**
  String get langSystem;

  /// No description provided for @langTurkish.
  ///
  /// In tr, this message translates to:
  /// **'Türkçe'**
  String get langTurkish;

  /// No description provided for @langEnglish.
  ///
  /// In tr, this message translates to:
  /// **'English'**
  String get langEnglish;

  /// No description provided for @visibility.
  ///
  /// In tr, this message translates to:
  /// **'Borç Görünürlüğü'**
  String get visibility;

  /// No description provided for @filterAll.
  ///
  /// In tr, this message translates to:
  /// **'Hepsi'**
  String get filterAll;

  /// No description provided for @filterActive.
  ///
  /// In tr, this message translates to:
  /// **'Aktif'**
  String get filterActive;

  /// No description provided for @filterInactive.
  ///
  /// In tr, this message translates to:
  /// **'Pasif'**
  String get filterInactive;

  /// No description provided for @developerCredit.
  ///
  /// In tr, this message translates to:
  /// **'Developed by {name}'**
  String developerCredit(String name);

  /// No description provided for @currency.
  ///
  /// In tr, this message translates to:
  /// **'Para Birimi'**
  String get currency;

  /// No description provided for @installmentCount.
  ///
  /// In tr, this message translates to:
  /// **'Taksit Sayısı'**
  String get installmentCount;

  /// No description provided for @installmentCountOptional.
  ///
  /// In tr, this message translates to:
  /// **'Toplam Taksit Sayısı (İsteğe Bağlı)'**
  String get installmentCountOptional;

  /// No description provided for @installmentProgress.
  ///
  /// In tr, this message translates to:
  /// **'{paid} / {total}'**
  String installmentProgress(int paid, int total);

  /// No description provided for @openEnded.
  ///
  /// In tr, this message translates to:
  /// **'Açık Uçlu'**
  String get openEnded;

  /// No description provided for @openEndedProgress.
  ///
  /// In tr, this message translates to:
  /// **'{count}'**
  String openEndedProgress(int count);

  /// No description provided for @memberEmail.
  ///
  /// In tr, this message translates to:
  /// **'Üye E-posta Adresi'**
  String get memberEmail;

  /// No description provided for @reminder.
  ///
  /// In tr, this message translates to:
  /// **'Hatırlatma'**
  String get reminder;

  /// No description provided for @reminderDate.
  ///
  /// In tr, this message translates to:
  /// **'Hatırlatma Tarihi'**
  String get reminderDate;

  /// No description provided for @reminderTime.
  ///
  /// In tr, this message translates to:
  /// **'Hatırlatma Saati'**
  String get reminderTime;

  /// No description provided for @reminderChannels.
  ///
  /// In tr, this message translates to:
  /// **'Bildirim Kanalları'**
  String get reminderChannels;

  /// No description provided for @channelEmail.
  ///
  /// In tr, this message translates to:
  /// **'E-posta'**
  String get channelEmail;

  /// No description provided for @channelInApp.
  ///
  /// In tr, this message translates to:
  /// **'Uygulama İçi'**
  String get channelInApp;

  /// No description provided for @premiumRequiredTitle.
  ///
  /// In tr, this message translates to:
  /// **'Premium Gerekli'**
  String get premiumRequiredTitle;

  /// No description provided for @premiumLimitReached.
  ///
  /// In tr, this message translates to:
  /// **'Ücretsiz sürümde en fazla {limit} {feature} ekleyebilirsiniz. Devam etmek için Premium\'a geçin.'**
  String premiumLimitReached(int limit, String feature);

  /// No description provided for @premiumFeatureRequired.
  ///
  /// In tr, this message translates to:
  /// **'{feature} özelliğini kullanmak için Premium\'a geçin.'**
  String premiumFeatureRequired(String feature);

  /// No description provided for @maybeLater.
  ///
  /// In tr, this message translates to:
  /// **'Belki Daha Sonra'**
  String get maybeLater;

  /// No description provided for @premium.
  ///
  /// In tr, this message translates to:
  /// **'Premium'**
  String get premium;

  /// No description provided for @getPremium.
  ///
  /// In tr, this message translates to:
  /// **'Premium Al'**
  String get getPremium;

  /// No description provided for @goPremium.
  ///
  /// In tr, this message translates to:
  /// **'Premium\'a Geç'**
  String get goPremium;

  /// No description provided for @premiumActive.
  ///
  /// In tr, this message translates to:
  /// **'Aktif'**
  String get premiumActive;

  /// No description provided for @premiumUnlockDesc.
  ///
  /// In tr, this message translates to:
  /// **'Kamci\'nin tüm potansiyelinin kilidini aç'**
  String get premiumUnlockDesc;

  /// No description provided for @premiumUnlimitedDesc.
  ///
  /// In tr, this message translates to:
  /// **'Sınırsız borç, kategori ve daha fazlası'**
  String get premiumUnlimitedDesc;

  /// No description provided for @renewsOn.
  ///
  /// In tr, this message translates to:
  /// **'Yenileme: {date}'**
  String renewsOn(String date);

  /// No description provided for @bestValue.
  ///
  /// In tr, this message translates to:
  /// **'En İyi Değer'**
  String get bestValue;

  /// No description provided for @autoRenewInfo.
  ///
  /// In tr, this message translates to:
  /// **'Otomatik yenilenir. İstediğinizde iptal edin.'**
  String get autoRenewInfo;

  /// No description provided for @restorePurchases.
  ///
  /// In tr, this message translates to:
  /// **'Satın Alımları Geri Yükle'**
  String get restorePurchases;

  /// No description provided for @featureUnlimitedDebts.
  ///
  /// In tr, this message translates to:
  /// **'Sınırsız borç'**
  String get featureUnlimitedDebts;

  /// No description provided for @featureUnlimitedCategories.
  ///
  /// In tr, this message translates to:
  /// **'Sınırsız kategori'**
  String get featureUnlimitedCategories;

  /// No description provided for @featureReminders.
  ///
  /// In tr, this message translates to:
  /// **'Borç hatırlatıcıları ve bildirimler'**
  String get featureReminders;

  /// No description provided for @featureCategorySharing.
  ///
  /// In tr, this message translates to:
  /// **'Arkadaşlarla kategori paylaşımı'**
  String get featureCategorySharing;

  /// No description provided for @perMonth.
  ///
  /// In tr, this message translates to:
  /// **'/ ay'**
  String get perMonth;

  /// No description provided for @perYear.
  ///
  /// In tr, this message translates to:
  /// **'/ yıl'**
  String get perYear;

  /// No description provided for @sharedPremiumDesc.
  ///
  /// In tr, this message translates to:
  /// **'Paylaşılan borçları görmek ve arkadaşlarınla ortak borç yönetmek için Premium üye olmalısın.'**
  String get sharedPremiumDesc;

  /// No description provided for @userId.
  ///
  /// In tr, this message translates to:
  /// **'Kullanıcı ID'**
  String get userId;

  /// No description provided for @userIdHint.
  ///
  /// In tr, this message translates to:
  /// **'Kullanıcı ID\'sini girin'**
  String get userIdHint;

  /// No description provided for @accessLevel.
  ///
  /// In tr, this message translates to:
  /// **'Erişim Seviyesi'**
  String get accessLevel;

  /// No description provided for @skipPayment.
  ///
  /// In tr, this message translates to:
  /// **'Ödemeyi Atla'**
  String get skipPayment;

  /// No description provided for @skipPaymentConfirm.
  ///
  /// In tr, this message translates to:
  /// **'Bu ödeme dönemini atlamak istediğinize emin misiniz?'**
  String get skipPaymentConfirm;

  /// No description provided for @paymentSkipped.
  ///
  /// In tr, this message translates to:
  /// **'{name} ödeme dönemi atlandı.'**
  String paymentSkipped(String name);

  /// No description provided for @income.
  ///
  /// In tr, this message translates to:
  /// **'Gelir'**
  String get income;

  /// No description provided for @incomeHistory.
  ///
  /// In tr, this message translates to:
  /// **'Gelir Geçmişi'**
  String get incomeHistory;

  /// No description provided for @incomeDesc.
  ///
  /// In tr, this message translates to:
  /// **'Aylık gelirini kaydet, borç/gelir oranını takip et'**
  String get incomeDesc;

  /// No description provided for @addIncome.
  ///
  /// In tr, this message translates to:
  /// **'Gelir Ekle'**
  String get addIncome;

  /// No description provided for @noIncomeYet.
  ///
  /// In tr, this message translates to:
  /// **'Henüz gelir kaydı yok'**
  String get noIncomeYet;

  /// No description provided for @incomeAmount.
  ///
  /// In tr, this message translates to:
  /// **'Gelir Tutarı'**
  String get incomeAmount;

  /// No description provided for @incomeNote.
  ///
  /// In tr, this message translates to:
  /// **'Not (İsteğe Bağlı)'**
  String get incomeNote;

  /// No description provided for @effectiveFrom.
  ///
  /// In tr, this message translates to:
  /// **'Geçerlilik Tarihi'**
  String get effectiveFrom;

  /// No description provided for @deleteIncome.
  ///
  /// In tr, this message translates to:
  /// **'Geliri Sil'**
  String get deleteIncome;

  /// No description provided for @deleteIncomeConfirm.
  ///
  /// In tr, this message translates to:
  /// **'Bu gelir kaydını silmek istediğinize emin misiniz?'**
  String get deleteIncomeConfirm;

  /// No description provided for @reminderChannelRequired.
  ///
  /// In tr, this message translates to:
  /// **'En az bir bildirim kanalı seçilmelidir'**
  String get reminderChannelRequired;

  /// No description provided for @reminderDateRequired.
  ///
  /// In tr, this message translates to:
  /// **'Hatırlatma tarihi seçilmelidir'**
  String get reminderDateRequired;

  /// No description provided for @reminderTimeRequired.
  ///
  /// In tr, this message translates to:
  /// **'Hatırlatma saati seçilmelidir'**
  String get reminderTimeRequired;

  /// No description provided for @paymentActions.
  ///
  /// In tr, this message translates to:
  /// **'Ödeme İşlemleri'**
  String get paymentActions;

  /// No description provided for @partialPayment.
  ///
  /// In tr, this message translates to:
  /// **'Parçalı Ödeme'**
  String get partialPayment;

  /// No description provided for @fullPayment.
  ///
  /// In tr, this message translates to:
  /// **'Tam Ödeme'**
  String get fullPayment;

  /// No description provided for @bulkPayment.
  ///
  /// In tr, this message translates to:
  /// **'Toplu Ödeme'**
  String get bulkPayment;

  /// No description provided for @setPaidInstallments.
  ///
  /// In tr, this message translates to:
  /// **'Ödenmiş Taksitleri Ayarla'**
  String get setPaidInstallments;

  /// No description provided for @setPaidInstallmentsDesc.
  ///
  /// In tr, this message translates to:
  /// **'Başka bir uygulamadan geçiyorsan, bu borç için daha önce ödediğin taksit sayısını gir.'**
  String get setPaidInstallmentsDesc;

  /// No description provided for @paidInstallmentsCountLabel.
  ///
  /// In tr, this message translates to:
  /// **'Ödenmiş taksit sayısı'**
  String get paidInstallmentsCountLabel;

  /// No description provided for @paidInstallmentsCountInvalid.
  ///
  /// In tr, this message translates to:
  /// **'1 veya daha büyük bir sayı girin'**
  String get paidInstallmentsCountInvalid;

  /// No description provided for @setPaidInstallmentsSuccess.
  ///
  /// In tr, this message translates to:
  /// **'{count} taksit ödenmiş olarak işaretlendi'**
  String setPaidInstallmentsSuccess(int count);

  /// No description provided for @enterAmount.
  ///
  /// In tr, this message translates to:
  /// **'Tutar Girin'**
  String get enterAmount;

  /// No description provided for @partialPaymentHint.
  ///
  /// In tr, this message translates to:
  /// **'Boş bırakılırsa tam tutar kullanılır'**
  String get partialPaymentHint;

  /// No description provided for @bulkPayTitle.
  ///
  /// In tr, this message translates to:
  /// **'Toplu Ödeme Seç'**
  String get bulkPayTitle;

  /// No description provided for @bulkPayConfirm.
  ///
  /// In tr, this message translates to:
  /// **'{count} ödeme kaydedildi.'**
  String bulkPayConfirm(int count);

  /// No description provided for @charts.
  ///
  /// In tr, this message translates to:
  /// **'Grafikler'**
  String get charts;

  /// No description provided for @manageMembers.
  ///
  /// In tr, this message translates to:
  /// **'Üyeleri Yönet'**
  String get manageMembers;

  /// No description provided for @leaveCategory.
  ///
  /// In tr, this message translates to:
  /// **'Bu Kategoriden Ayrıl'**
  String get leaveCategory;

  /// No description provided for @leaveCategoryConfirm.
  ///
  /// In tr, this message translates to:
  /// **'\"{name}\" kategorisinden ayrılmak istediğinize emin misiniz?'**
  String leaveCategoryConfirm(String name);

  /// No description provided for @bulkShare.
  ///
  /// In tr, this message translates to:
  /// **'Toplu Paylaş'**
  String get bulkShare;

  /// No description provided for @shareCategory.
  ///
  /// In tr, this message translates to:
  /// **'Paylaş'**
  String get shareCategory;

  /// No description provided for @shareAllSelected.
  ///
  /// In tr, this message translates to:
  /// **'Seçilenleri Paylaş ({count})'**
  String shareAllSelected(int count);

  /// No description provided for @selectCategoriesToShare.
  ///
  /// In tr, this message translates to:
  /// **'Kategorileri Seç'**
  String get selectCategoriesToShare;

  /// No description provided for @addToCategories.
  ///
  /// In tr, this message translates to:
  /// **'{count} kategoriye ekle'**
  String addToCategories(int count);

  /// No description provided for @shareAccessLevel.
  ///
  /// In tr, this message translates to:
  /// **'Erişim Seviyesi'**
  String get shareAccessLevel;

  /// No description provided for @selectAccessLevel.
  ///
  /// In tr, this message translates to:
  /// **'Erişim seviyesi seçin'**
  String get selectAccessLevel;

  /// No description provided for @categoriesSelected.
  ///
  /// In tr, this message translates to:
  /// **'{count} kategori seçildi'**
  String categoriesSelected(int count);

  /// No description provided for @clearSelection.
  ///
  /// In tr, this message translates to:
  /// **'Seçimi Temizle'**
  String get clearSelection;

  /// No description provided for @memberAddedSuccess.
  ///
  /// In tr, this message translates to:
  /// **'{email} üye olarak eklendi.'**
  String memberAddedSuccess(String email);

  /// No description provided for @memberRemovedSuccess.
  ///
  /// In tr, this message translates to:
  /// **'Üye kaldırıldı.'**
  String get memberRemovedSuccess;

  /// No description provided for @bulkShareSuccess.
  ///
  /// In tr, this message translates to:
  /// **'{email} {count} kategoriye eklendi.'**
  String bulkShareSuccess(String email, int count);

  /// No description provided for @memberLeftCategory.
  ///
  /// In tr, this message translates to:
  /// **'Kategoriden ayrıldınız.'**
  String get memberLeftCategory;

  /// No description provided for @changeAccessLevel.
  ///
  /// In tr, this message translates to:
  /// **'Erişim Seviyesini Değiştir'**
  String get changeAccessLevel;

  /// No description provided for @membersList.
  ///
  /// In tr, this message translates to:
  /// **'Üyeler Listesi'**
  String get membersList;

  /// No description provided for @accessRead.
  ///
  /// In tr, this message translates to:
  /// **'Okuma'**
  String get accessRead;

  /// No description provided for @accessReadDesc.
  ///
  /// In tr, this message translates to:
  /// **'Sadece görüntüleyebilir'**
  String get accessReadDesc;

  /// No description provided for @accessWrite.
  ///
  /// In tr, this message translates to:
  /// **'Yazma'**
  String get accessWrite;

  /// No description provided for @accessWriteDesc.
  ///
  /// In tr, this message translates to:
  /// **'Görüntüleyebilir ve ödeme yapabilir'**
  String get accessWriteDesc;

  /// No description provided for @accessEdit.
  ///
  /// In tr, this message translates to:
  /// **'Düzenleme'**
  String get accessEdit;

  /// No description provided for @accessEditDesc.
  ///
  /// In tr, this message translates to:
  /// **'Görüntüleyebilir, ödeme yapabilir ve düzenleyebilir'**
  String get accessEditDesc;

  /// No description provided for @accessDelete.
  ///
  /// In tr, this message translates to:
  /// **'Admin'**
  String get accessDelete;

  /// No description provided for @accessDeleteDesc.
  ///
  /// In tr, this message translates to:
  /// **'Tam yetki: görüntüleme, ödeme, düzenleme ve silme'**
  String get accessDeleteDesc;

  /// No description provided for @noPermission.
  ///
  /// In tr, this message translates to:
  /// **'Bu operasyon için yetkiniz yok.'**
  String get noPermission;

  /// No description provided for @errorWithMessage.
  ///
  /// In tr, this message translates to:
  /// **'Hata: {message}'**
  String errorWithMessage(String message);

  /// No description provided for @validationEmailRequired.
  ///
  /// In tr, this message translates to:
  /// **'Email gerekli'**
  String get validationEmailRequired;

  /// No description provided for @validationEmailInvalid.
  ///
  /// In tr, this message translates to:
  /// **'Geçerli bir email girin'**
  String get validationEmailInvalid;

  /// No description provided for @validationPasswordRequired.
  ///
  /// In tr, this message translates to:
  /// **'Şifre gerekli'**
  String get validationPasswordRequired;

  /// No description provided for @validationPasswordMinLength.
  ///
  /// In tr, this message translates to:
  /// **'Şifre en az 8 karakter olmalı'**
  String get validationPasswordMinLength;

  /// No description provided for @validationPasswordMaxLength.
  ///
  /// In tr, this message translates to:
  /// **'Şifre en fazla 72 karakter olabilir'**
  String get validationPasswordMaxLength;

  /// No description provided for @validationConfirmPasswordRequired.
  ///
  /// In tr, this message translates to:
  /// **'Şifre tekrarı gerekli'**
  String get validationConfirmPasswordRequired;

  /// No description provided for @validationPasswordMismatch.
  ///
  /// In tr, this message translates to:
  /// **'Şifreler eşleşmiyor'**
  String get validationPasswordMismatch;

  /// No description provided for @validationFieldRequired.
  ///
  /// In tr, this message translates to:
  /// **'{fieldName} gerekli'**
  String validationFieldRequired(String fieldName);

  /// No description provided for @validationAmountRequired.
  ///
  /// In tr, this message translates to:
  /// **'Tutar gerekli'**
  String get validationAmountRequired;

  /// No description provided for @validationAmountInvalid.
  ///
  /// In tr, this message translates to:
  /// **'Geçerli bir tutar girin'**
  String get validationAmountInvalid;

  /// No description provided for @validationAmountPositive.
  ///
  /// In tr, this message translates to:
  /// **'Tutar 0\'dan büyük olmalı'**
  String get validationAmountPositive;

  /// No description provided for @errorInvalidCredentials.
  ///
  /// In tr, this message translates to:
  /// **'E-posta veya şifre hatalı.'**
  String get errorInvalidCredentials;

  /// No description provided for @errorUnexpected.
  ///
  /// In tr, this message translates to:
  /// **'Beklenmeyen bir hata oluştu.'**
  String get errorUnexpected;

  /// No description provided for @errorNetwork.
  ///
  /// In tr, this message translates to:
  /// **'İnternet bağlantısı yok. Bağlantınızı kontrol edin.'**
  String get errorNetwork;

  /// No description provided for @errorServer.
  ///
  /// In tr, this message translates to:
  /// **'Sunucu hatası oluştu. Lütfen tekrar deneyin.'**
  String get errorServer;

  /// No description provided for @errorNotFound.
  ///
  /// In tr, this message translates to:
  /// **'İstenen kaynak bulunamadı.'**
  String get errorNotFound;

  /// No description provided for @errorRateLimit.
  ///
  /// In tr, this message translates to:
  /// **'Çok fazla istek gönderildi. Lütfen bir süre bekleyin.'**
  String get errorRateLimit;

  /// No description provided for @errorSessionExpired.
  ///
  /// In tr, this message translates to:
  /// **'Oturumunuz sona erdi. Lütfen tekrar giriş yapın.'**
  String get errorSessionExpired;

  /// No description provided for @errorDebtAlreadyCompleted.
  ///
  /// In tr, this message translates to:
  /// **'Bu borç zaten tamamlanmış.'**
  String get errorDebtAlreadyCompleted;

  /// No description provided for @errorDebtOverpayment.
  ///
  /// In tr, this message translates to:
  /// **'Ödeme tutarı, kalan borç tutarını aşamaz.'**
  String get errorDebtOverpayment;

  /// No description provided for @errorCategoryNameConflict.
  ///
  /// In tr, this message translates to:
  /// **'Bu isimde bir kategori zaten var.'**
  String get errorCategoryNameConflict;

  /// No description provided for @errorCategoryCannotAddSelf.
  ///
  /// In tr, this message translates to:
  /// **'Kendinizi üye olarak ekleyemezsiniz.'**
  String get errorCategoryCannotAddSelf;

  /// No description provided for @errorCategoryMemberNotFound.
  ///
  /// In tr, this message translates to:
  /// **'Üye bulunamadı.'**
  String get errorCategoryMemberNotFound;

  /// No description provided for @errorCategoryNotFound.
  ///
  /// In tr, this message translates to:
  /// **'Kategori bulunamadı.'**
  String get errorCategoryNotFound;

  /// No description provided for @errorCategoryMemberUserNotFound.
  ///
  /// In tr, this message translates to:
  /// **'Bu e-posta ile kayıtlı kullanıcı bulunamadı.'**
  String get errorCategoryMemberUserNotFound;

  /// No description provided for @errorCategoryMemberAlreadyExists.
  ///
  /// In tr, this message translates to:
  /// **'Bu kullanıcı zaten kategori üyesi.'**
  String get errorCategoryMemberAlreadyExists;

  /// No description provided for @errorCategoryMigrateSameCategory.
  ///
  /// In tr, this message translates to:
  /// **'Hedef kategori, kaynak kategoriyle aynı olamaz.'**
  String get errorCategoryMigrateSameCategory;

  /// No description provided for @errorDebtNotFound.
  ///
  /// In tr, this message translates to:
  /// **'Borç bulunamadı.'**
  String get errorDebtNotFound;

  /// No description provided for @errorDebtAlreadyPaidThisPeriod.
  ///
  /// In tr, this message translates to:
  /// **'Bu dönem için ödeme zaten yapılmış.'**
  String get errorDebtAlreadyPaidThisPeriod;

  /// No description provided for @errorDebtAutoPaymentEnabled.
  ///
  /// In tr, this message translates to:
  /// **'Otomatik ödeme açıkken bu işlem yapılamaz.'**
  String get errorDebtAutoPaymentEnabled;

  /// No description provided for @errorDebtConcurrentRequest.
  ///
  /// In tr, this message translates to:
  /// **'Bu borç için başka bir işlem sürüyor, lütfen tekrar deneyin.'**
  String get errorDebtConcurrentRequest;

  /// No description provided for @errorDebtPaymentBeforeStart.
  ///
  /// In tr, this message translates to:
  /// **'Ödeme, borcun başlangıç tarihinden önce olamaz.'**
  String get errorDebtPaymentBeforeStart;

  /// No description provided for @errorDebtPeriodAlreadyRecorded.
  ///
  /// In tr, this message translates to:
  /// **'Bu dönem zaten kaydedilmiş.'**
  String get errorDebtPeriodAlreadyRecorded;

  /// No description provided for @errorDebtInstallmentCountExceeded.
  ///
  /// In tr, this message translates to:
  /// **'Taksit sayısı sınırı aşıldı.'**
  String get errorDebtInstallmentCountExceeded;

  /// No description provided for @errorDebtMissingPeriodConfig.
  ///
  /// In tr, this message translates to:
  /// **'Ödeme periyodu bilgisi eksik.'**
  String get errorDebtMissingPeriodConfig;

  /// No description provided for @errorIncomeNotFound.
  ///
  /// In tr, this message translates to:
  /// **'Gelir kaydı bulunamadı.'**
  String get errorIncomeNotFound;

  /// No description provided for @errorReminderDateOrTimeMissing.
  ///
  /// In tr, this message translates to:
  /// **'Hatırlatma tarihi veya saati eksik.'**
  String get errorReminderDateOrTimeMissing;

  /// No description provided for @errorSubscriptionVerificationFailed.
  ///
  /// In tr, this message translates to:
  /// **'Abonelik doğrulaması başarısız oldu.'**
  String get errorSubscriptionVerificationFailed;

  /// No description provided for @errorSubscriptionNotFound.
  ///
  /// In tr, this message translates to:
  /// **'Abonelik bulunamadı.'**
  String get errorSubscriptionNotFound;

  /// No description provided for @errorAuthEmailInUse.
  ///
  /// In tr, this message translates to:
  /// **'Bu e-posta adresi zaten kullanımda.'**
  String get errorAuthEmailInUse;

  /// No description provided for @errorAuthTooManyLoginAttempts.
  ///
  /// In tr, this message translates to:
  /// **'Çok fazla giriş denemesi yapıldı. Lütfen bir süre bekleyin.'**
  String get errorAuthTooManyLoginAttempts;

  /// No description provided for @errorAuthNotValid.
  ///
  /// In tr, this message translates to:
  /// **'E-posta veya şifre hatalı.'**
  String get errorAuthNotValid;

  /// No description provided for @errorAuthTokenRevoked.
  ///
  /// In tr, this message translates to:
  /// **'Oturumunuz sonlandırıldı. Lütfen tekrar giriş yapın.'**
  String get errorAuthTokenRevoked;

  /// No description provided for @errorAuthTokenInvalid.
  ///
  /// In tr, this message translates to:
  /// **'Geçersiz oturum. Lütfen tekrar giriş yapın.'**
  String get errorAuthTokenInvalid;

  /// No description provided for @errorAuthCurrentPasswordInvalid.
  ///
  /// In tr, this message translates to:
  /// **'Mevcut şifreniz hatalı.'**
  String get errorAuthCurrentPasswordInvalid;

  /// No description provided for @errorAuthTokenAlreadyUsed.
  ///
  /// In tr, this message translates to:
  /// **'Bu bağlantı zaten kullanılmış. Lütfen tekrar deneyin.'**
  String get errorAuthTokenAlreadyUsed;

  /// No description provided for @errorAuthTooManyPasswordResetAttempts.
  ///
  /// In tr, this message translates to:
  /// **'Çok fazla şifre sıfırlama denemesi yapıldı. Lütfen bir süre bekleyin.'**
  String get errorAuthTooManyPasswordResetAttempts;

  /// No description provided for @errorHouseholdInviteeNotFound.
  ///
  /// In tr, this message translates to:
  /// **'Bu e-posta ile kayıtlı bir kullanıcı bulunamadı.'**
  String get errorHouseholdInviteeNotFound;

  /// No description provided for @errorHouseholdCannotInviteSelf.
  ///
  /// In tr, this message translates to:
  /// **'Kendinizi davet edemezsiniz.'**
  String get errorHouseholdCannotInviteSelf;

  /// No description provided for @errorHouseholdAlreadyLinked.
  ///
  /// In tr, this message translates to:
  /// **'Zaten aktif veya bekleyen bir haneniz var.'**
  String get errorHouseholdAlreadyLinked;

  /// No description provided for @errorHouseholdInviteeAlreadyLinked.
  ///
  /// In tr, this message translates to:
  /// **'Bu kullanıcının zaten aktif veya bekleyen bir hanesi var.'**
  String get errorHouseholdInviteeAlreadyLinked;

  /// No description provided for @errorHouseholdNotFound.
  ///
  /// In tr, this message translates to:
  /// **'Hane bulunamadı.'**
  String get errorHouseholdNotFound;

  /// No description provided for @errorHouseholdNotPending.
  ///
  /// In tr, this message translates to:
  /// **'Bu davet artık bekleyen durumda değil.'**
  String get errorHouseholdNotPending;

  /// No description provided for @household.
  ///
  /// In tr, this message translates to:
  /// **'Hane'**
  String get household;

  /// No description provided for @householdDesc.
  ///
  /// In tr, this message translates to:
  /// **'Eşinizle geliri birleştirip ortak borç/gelir oranını görün'**
  String get householdDesc;

  /// No description provided for @householdInviteTitle.
  ///
  /// In tr, this message translates to:
  /// **'Hane Daveti Gönder'**
  String get householdInviteTitle;

  /// No description provided for @householdInviteEmailLabel.
  ///
  /// In tr, this message translates to:
  /// **'E-posta'**
  String get householdInviteEmailLabel;

  /// No description provided for @householdInviteButton.
  ///
  /// In tr, this message translates to:
  /// **'Davet Gönder'**
  String get householdInviteButton;

  /// No description provided for @householdInviteSent.
  ///
  /// In tr, this message translates to:
  /// **'Davet gönderildi'**
  String get householdInviteSent;

  /// No description provided for @householdNoHousehold.
  ///
  /// In tr, this message translates to:
  /// **'Henüz bir haneniz yok'**
  String get householdNoHousehold;

  /// No description provided for @householdPendingAsOwner.
  ///
  /// In tr, this message translates to:
  /// **'{email} için davetiniz onay bekliyor'**
  String householdPendingAsOwner(String email);

  /// No description provided for @householdPendingAsPartner.
  ///
  /// In tr, this message translates to:
  /// **'{email} sizi hanesine davet etti'**
  String householdPendingAsPartner(String email);

  /// No description provided for @householdActiveWith.
  ///
  /// In tr, this message translates to:
  /// **'{email} ile ortak hane'**
  String householdActiveWith(String email);

  /// No description provided for @householdAccept.
  ///
  /// In tr, this message translates to:
  /// **'Kabul Et'**
  String get householdAccept;

  /// No description provided for @householdDecline.
  ///
  /// In tr, this message translates to:
  /// **'Reddet'**
  String get householdDecline;

  /// No description provided for @householdLeave.
  ///
  /// In tr, this message translates to:
  /// **'Haneden Ayrıl'**
  String get householdLeave;

  /// No description provided for @householdLeaveConfirmTitle.
  ///
  /// In tr, this message translates to:
  /// **'Haneden ayrıl?'**
  String get householdLeaveConfirmTitle;

  /// No description provided for @householdLeaveConfirmBody.
  ///
  /// In tr, this message translates to:
  /// **'Ortak gelir birleştirme durdurulacak.'**
  String get householdLeaveConfirmBody;

  /// No description provided for @householdIncomeLabel.
  ///
  /// In tr, this message translates to:
  /// **'Hane Geliri'**
  String get householdIncomeLabel;

  /// No description provided for @householdRatioLabel.
  ///
  /// In tr, this message translates to:
  /// **'Hane B/G Oranı'**
  String get householdRatioLabel;

  /// No description provided for @notifications.
  ///
  /// In tr, this message translates to:
  /// **'Bildirimler'**
  String get notifications;

  /// No description provided for @notificationsDesc.
  ///
  /// In tr, this message translates to:
  /// **'Hatırlatıcılar, hane davetleri ve haftalık özet'**
  String get notificationsDesc;

  /// No description provided for @notificationsEmpty.
  ///
  /// In tr, this message translates to:
  /// **'Henüz bildirim yok'**
  String get notificationsEmpty;

  /// No description provided for @retryButton.
  ///
  /// In tr, this message translates to:
  /// **'Tekrar Dene'**
  String get retryButton;

  /// No description provided for @personCount.
  ///
  /// In tr, this message translates to:
  /// **'{count} kişi'**
  String personCount(int count);

  /// No description provided for @baseCurrency.
  ///
  /// In tr, this message translates to:
  /// **'Baz Para Birimi'**
  String get baseCurrency;

  /// No description provided for @monthlySummary.
  ///
  /// In tr, this message translates to:
  /// **'Aylık Özet'**
  String get monthlySummary;

  /// No description provided for @totalPaidAmount.
  ///
  /// In tr, this message translates to:
  /// **'Toplam Ödendi'**
  String get totalPaidAmount;

  /// No description provided for @trendLast6Months.
  ///
  /// In tr, this message translates to:
  /// **'Son 6 Ay Trendi'**
  String get trendLast6Months;

  /// No description provided for @categoryBreakdown.
  ///
  /// In tr, this message translates to:
  /// **'Kategori Dağılımı'**
  String get categoryBreakdown;

  /// No description provided for @debtProgressTitle.
  ///
  /// In tr, this message translates to:
  /// **'Borç İlerlemesi'**
  String get debtProgressTitle;

  /// No description provided for @noDataForMonth.
  ///
  /// In tr, this message translates to:
  /// **'Bu ay için veri yok'**
  String get noDataForMonth;

  /// No description provided for @debtToIncomeRatioLabel.
  ///
  /// In tr, this message translates to:
  /// **'Borç/Gelir Oranı'**
  String get debtToIncomeRatioLabel;

  /// No description provided for @pendingLabel.
  ///
  /// In tr, this message translates to:
  /// **'Bekleyen'**
  String get pendingLabel;

  /// No description provided for @skippedLabel.
  ///
  /// In tr, this message translates to:
  /// **'Atlanan'**
  String get skippedLabel;

  /// No description provided for @noIncomeSet.
  ///
  /// In tr, this message translates to:
  /// **'Gelir tanımlanmamış'**
  String get noIncomeSet;

  /// No description provided for @installmentsLabel.
  ///
  /// In tr, this message translates to:
  /// **'{paid} / {total} taksit'**
  String installmentsLabel(int paid, int total);

  /// No description provided for @openEndedInstallments.
  ///
  /// In tr, this message translates to:
  /// **'{count} taksit (açık uçlu)'**
  String openEndedInstallments(int count);
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'tr'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'tr':
      return AppLocalizationsTr();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
