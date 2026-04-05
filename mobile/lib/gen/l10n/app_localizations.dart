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

  /// No description provided for @premiumMembership.
  ///
  /// In tr, this message translates to:
  /// **'Premium Üyelik'**
  String get premiumMembership;

  /// No description provided for @premiumMembershipDesc.
  ///
  /// In tr, this message translates to:
  /// **'Daha fazla özelliğe erişin'**
  String get premiumMembershipDesc;

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
  /// **'Üye Olduklarım'**
  String get joinedCategories;

  /// No description provided for @noCategoriesYet.
  ///
  /// In tr, this message translates to:
  /// **'Henüz kategori yok'**
  String get noCategoriesYet;

  /// No description provided for @noJoinedCategories.
  ///
  /// In tr, this message translates to:
  /// **'Üye olduğun kategori yok'**
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

  /// No description provided for @categoryName.
  ///
  /// In tr, this message translates to:
  /// **'Kategori Adı'**
  String get categoryName;

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
  /// **'Taksit Sayısı (İsteğe Bağlı)'**
  String get installmentCountOptional;

  /// No description provided for @installmentProgress.
  ///
  /// In tr, this message translates to:
  /// **'{paid}/{total} taksit'**
  String installmentProgress(int paid, int total);

  /// No description provided for @openEnded.
  ///
  /// In tr, this message translates to:
  /// **'Açık Uçlu'**
  String get openEnded;

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
