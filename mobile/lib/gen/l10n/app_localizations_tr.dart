// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Turkish (`tr`).
class AppLocalizationsTr extends AppLocalizations {
  AppLocalizationsTr([String locale = 'tr']) : super(locale);

  @override
  String get appName => 'Kamci';

  @override
  String get save => 'Kaydet';

  @override
  String get cancel => 'İptal';

  @override
  String get delete => 'Sil';

  @override
  String get edit => 'Düzenle';

  @override
  String get create => 'Oluştur';

  @override
  String get update => 'Güncelle';

  @override
  String get send => 'Gönder';

  @override
  String get remove => 'Kaldır';

  @override
  String get add => 'Ekle';

  @override
  String get select => 'Seçin';

  @override
  String get retry => 'Tekrar Dene';

  @override
  String get welcome => 'Hoş Geldiniz!';

  @override
  String get signIn => 'Giriş Yap';

  @override
  String get signUp => 'Kayıt Ol';

  @override
  String get signOut => 'Çıkış Yap';

  @override
  String get signOutConfirm =>
      'Hesabınızdan çıkış yapmak istediğinize emin misiniz?';

  @override
  String get deleteAccount => 'Hesabı Sil';

  @override
  String get deleteAccountConfirmTitle =>
      'Hesabınız kalıcı olarak silinsin mi?';

  @override
  String get deleteAccountConfirm =>
      'Hesabınız ve tüm borç, kategori, gelir ve bildirim verileriniz kalıcı olarak silinecek. Bu işlem geri alınamaz.\n\nAktif bir aboneliğiniz varsa, ücretlendirmenin durması için App Store veya Google Play üzerinden ayrıca iptal etmeniz gerekir.';

  @override
  String get deleteAccountConfirmButton => 'Kalıcı Olarak Sil';

  @override
  String get email => 'E-posta Adresi';

  @override
  String get password => 'Şifre';

  @override
  String get confirmPassword => 'Şifre Tekrar';

  @override
  String get newPassword => 'Yeni Şifre';

  @override
  String get forgotPassword => 'Şifremi Unuttum?';

  @override
  String get forgotPasswordTitle => 'Şifremi Unuttum';

  @override
  String get resetPasswordTitle => 'Şifre Sıfırla';

  @override
  String get updatePassword => 'Şifreyi Güncelle';

  @override
  String get noAccount => 'Hesabınız yok mu?';

  @override
  String get hasAccount => 'Zaten hesabınız var mı?';

  @override
  String get forgotPasswordDesc =>
      'Şifre sıfırlama bağlantısı e-posta adresinize gönderilecek.';

  @override
  String get resetPasswordDesc => 'Yeni şifrenizi belirleyin.';

  @override
  String get checkEmail => 'E-postanızı Kontrol Edin';

  @override
  String checkEmailDesc(String email) {
    return '$email adresine şifre sıfırlama bağlantısı gönderildi.';
  }

  @override
  String get invalidResetLink => 'Geçersiz şifre sıfırlama bağlantısı.';

  @override
  String get passwordUpdated => 'Şifreniz başarıyla güncellendi.';

  @override
  String get debts => 'Borçlar';

  @override
  String get categories => 'Kategoriler';

  @override
  String get myDebts => 'Kendi Borçlarım';

  @override
  String get sharedDebts => 'Paylaşılan';

  @override
  String get noDebtsYet => 'Henüz borç yok';

  @override
  String get noSharedDebts => 'Paylaşılan borç yok';

  @override
  String get addDebtHint => 'Yeni borç eklemek için + düğmesine bas';

  @override
  String get joinSharedHint => 'Paylaşılan bir kategoriye üye ol';

  @override
  String get noCategoryDebts => 'Bu kategoride henüz borç yok';

  @override
  String get noDebts => 'Borç yok';

  @override
  String get summaryTotal => 'Toplam';

  @override
  String get deleteDebtPermanentWarning =>
      'Bu borç kalıcı olarak silinecek, geri alınamaz. Emin misiniz?';

  @override
  String paymentRecorded(String name) {
    return '$name ödemeniz kayıt edildi.';
  }

  @override
  String get deleteDebt => 'Borcu Sil';

  @override
  String deleteDebtConfirm(String name) {
    return '\"$name\" borcunu silmek istediğinize emin misiniz?';
  }

  @override
  String get amount => 'Tutar';

  @override
  String get status => 'Durum';

  @override
  String get paymentPeriod => 'Ödeme Periyodu';

  @override
  String get startDate => 'Başlangıç';

  @override
  String get firstPaymentDate => 'İlk Ödeme Tarihi';

  @override
  String get endDate => 'Bitiş';

  @override
  String get description => 'Açıklama';

  @override
  String get nextReminder => 'Sonraki Hatırlatma';

  @override
  String paymentHistory(int count) {
    return 'Ödeme Geçmişi ($count)';
  }

  @override
  String get noPaymentsYet => 'Henüz ödeme yapılmamış';

  @override
  String get paid => 'Ödendi';

  @override
  String get pendingPayment => 'Bekliyor';

  @override
  String get newDebt => 'Yeni Borç';

  @override
  String get editDebt => 'Borcu Düzenle';

  @override
  String get debtName => 'Borç Adı';

  @override
  String get selectDate => 'Seçin';

  @override
  String get selectStartDate => 'Başlangıç tarihini seçin';

  @override
  String get noCategory => 'Kategori Yok';

  @override
  String get category => 'Kategori';

  @override
  String get autoPayment => 'Otomatik Ödeme';

  @override
  String get categoryOptional => 'Kategori (İsteğe Bağlı)';

  @override
  String get descriptionOptional => 'Açıklama (İsteğe Bağlı)';

  @override
  String get myCategories => 'Sahip Olduklarım';

  @override
  String get joinedCategories => 'Paylaşılanlar';

  @override
  String get noCategoriesYet => 'Henüz kategori yok';

  @override
  String get noJoinedCategories => 'Paylaşılan kategori yok';

  @override
  String get addCategoryHint => 'Yeni kategori oluşturmak için + düğmesine bas';

  @override
  String get waitingInvitation => 'Bir kategoriye davet bekleniyor';

  @override
  String memberCount(int count) {
    return '$count üye';
  }

  @override
  String get deleteCategory => 'Kategoriyi Sil';

  @override
  String deleteCategoryConfirm(String name) {
    return '\"$name\" kategorisini silmek istediğinize emin misiniz?';
  }

  @override
  String memberCountTitle(int count) {
    return '$count Üye';
  }

  @override
  String get noMembersYet => 'Henüz üye yok';

  @override
  String get addMember => 'Üye Ekle';

  @override
  String get removeMember => 'Üyeyi Kaldır';

  @override
  String get removeMemberConfirm =>
      'Bu üyeyi kategoriden kaldırmak istediğinize emin misiniz?';

  @override
  String get newCategory => 'Yeni Kategori';

  @override
  String get editCategory => 'Kategoriyi Düzenle';

  @override
  String get transferAndDeleteCategory => 'Borçları Aktarıp Kategoriyi Sil';

  @override
  String get deleteWithDebts => 'Borçları ile Kategoriyi Sil';

  @override
  String get selectTargetCategory => 'Hedef Kategori Seç';

  @override
  String transferAndDeleteConfirm(String name) {
    return '\"$name\" kategorisindeki borçlar seçilen kategoriye aktarılacak, ardından kategori silinecek.';
  }

  @override
  String deleteWithDebtsConfirm(String name) {
    return '\"$name\" kategorisi ve içindeki tüm borçlar silinecek. Bu işlem geri alınamaz.';
  }

  @override
  String migratedDebtCount(int count) {
    return '$count borç hedef kategoriye taşındı';
  }

  @override
  String get categoryName => 'Kategori Adı';

  @override
  String get periodOneTime => 'Tek Seferlik';

  @override
  String get periodDaily => 'Günlük';

  @override
  String get periodWeekly => 'Haftalık';

  @override
  String get periodBiWeekly => 'İki Haftalık';

  @override
  String get periodMonthly => 'Aylık';

  @override
  String get periodQuarterly => 'Üç Aylık';

  @override
  String get periodHalfYearly => 'Altı Aylık';

  @override
  String get periodYearly => 'Yıllık';

  @override
  String get statusActive => 'Aktif';

  @override
  String get statusInactive => 'Pasif';

  @override
  String get statusCompleted => 'Tamamlandı';

  @override
  String get currentPassword => 'Mevcut Şifre';

  @override
  String get settings => 'Ayarlar';

  @override
  String get account => 'Hesap';

  @override
  String get changePassword => 'Şifre Değiştir';

  @override
  String get changePasswordDesc =>
      'Şifre sıfırlama bağlantısı e-postanıza gönderilecek';

  @override
  String get feedback => 'Geri Bildirim';

  @override
  String get sendFeedback => 'Öneri / Şikayet Gönder';

  @override
  String get feedbackDesc => 'Görüş ve önerilerinizi bizimle paylaşın';

  @override
  String get appearance => 'Uygulama Ayarları';

  @override
  String get language => 'Dil';

  @override
  String get theme => 'Tema';

  @override
  String get themeSystem => 'Sistem';

  @override
  String get themeLight => 'Açık';

  @override
  String get themeDark => 'Koyu';

  @override
  String get langSystem => 'Sistem';

  @override
  String get langTurkish => 'Türkçe';

  @override
  String get langEnglish => 'English';

  @override
  String get visibility => 'Borç Görünürlüğü';

  @override
  String get filterAll => 'Hepsi';

  @override
  String get filterActive => 'Aktif';

  @override
  String get filterInactive => 'Pasif';

  @override
  String developerCredit(String name) {
    return 'Developed by $name';
  }

  @override
  String get currency => 'Para Birimi';

  @override
  String get installmentCount => 'Taksit Sayısı';

  @override
  String get installmentCountOptional => 'Toplam Taksit Sayısı (İsteğe Bağlı)';

  @override
  String installmentProgress(int paid, int total) {
    return '$paid / $total';
  }

  @override
  String get openEnded => 'Açık Uçlu';

  @override
  String openEndedProgress(int count) {
    return '$count';
  }

  @override
  String get memberEmail => 'Üye E-posta Adresi';

  @override
  String get reminder => 'Hatırlatma';

  @override
  String get reminderDate => 'Hatırlatma Tarihi';

  @override
  String get reminderTime => 'Hatırlatma Saati';

  @override
  String get reminderChannels => 'Bildirim Kanalları';

  @override
  String get channelEmail => 'E-posta';

  @override
  String get channelInApp => 'Uygulama İçi';

  @override
  String get premiumRequiredTitle => 'Premium Gerekli';

  @override
  String premiumLimitReached(int limit, String feature) {
    return 'Ücretsiz sürümde en fazla $limit $feature ekleyebilirsiniz. Devam etmek için Premium\'a geçin.';
  }

  @override
  String premiumFeatureRequired(String feature) {
    return '$feature özelliğini kullanmak için Premium\'a geçin.';
  }

  @override
  String get maybeLater => 'Belki Daha Sonra';

  @override
  String get premium => 'Premium';

  @override
  String get getPremium => 'Premium Al';

  @override
  String get goPremium => 'Premium\'a Geç';

  @override
  String get premiumActive => 'Aktif';

  @override
  String get premiumUnlockDesc => 'Kamci\'nin tüm potansiyelinin kilidini aç';

  @override
  String get premiumUnlimitedDesc => 'Sınırsız borç, kategori ve daha fazlası';

  @override
  String renewsOn(String date) {
    return 'Yenileme: $date';
  }

  @override
  String get bestValue => 'En İyi Değer';

  @override
  String get autoRenewInfo => 'Otomatik yenilenir. İstediğinizde iptal edin.';

  @override
  String get restorePurchases => 'Satın Alımları Geri Yükle';

  @override
  String get featureUnlimitedDebts => 'Sınırsız borç';

  @override
  String get featureUnlimitedCategories => 'Sınırsız kategori';

  @override
  String get featureReminders => 'Borç hatırlatıcıları ve bildirimler';

  @override
  String get featureCategorySharing => 'Arkadaşlarla kategori paylaşımı';

  @override
  String get perMonth => '/ ay';

  @override
  String get perYear => '/ yıl';

  @override
  String get sharedPremiumDesc =>
      'Paylaşılan borçları görmek ve arkadaşlarınla ortak borç yönetmek için Premium üye olmalısın.';

  @override
  String get userId => 'Kullanıcı ID';

  @override
  String get userIdHint => 'Kullanıcı ID\'sini girin';

  @override
  String get accessLevel => 'Erişim Seviyesi';

  @override
  String get skipPayment => 'Ödemeyi Atla';

  @override
  String get skipPaymentConfirm =>
      'Bu ödeme dönemini atlamak istediğinize emin misiniz?';

  @override
  String paymentSkipped(String name) {
    return '$name ödeme dönemi atlandı.';
  }

  @override
  String get income => 'Gelir';

  @override
  String get incomeHistory => 'Gelir Geçmişi';

  @override
  String get incomeDesc => 'Aylık gelirini kaydet, borç/gelir oranını takip et';

  @override
  String get addIncome => 'Gelir Ekle';

  @override
  String get noIncomeYet => 'Henüz gelir kaydı yok';

  @override
  String get incomeAmount => 'Gelir Tutarı';

  @override
  String get incomeNote => 'Not (İsteğe Bağlı)';

  @override
  String get effectiveFrom => 'Geçerlilik Tarihi';

  @override
  String get deleteIncome => 'Geliri Sil';

  @override
  String get deleteIncomeConfirm =>
      'Bu gelir kaydını silmek istediğinize emin misiniz?';

  @override
  String get reminderChannelRequired =>
      'En az bir bildirim kanalı seçilmelidir';

  @override
  String get reminderDateRequired => 'Hatırlatma tarihi seçilmelidir';

  @override
  String get reminderTimeRequired => 'Hatırlatma saati seçilmelidir';

  @override
  String get paymentActions => 'Ödeme İşlemleri';

  @override
  String get partialPayment => 'Parçalı Ödeme';

  @override
  String get fullPayment => 'Tam Ödeme';

  @override
  String get bulkPayment => 'Toplu Ödeme';

  @override
  String get setPaidInstallments => 'Ödenmiş Taksitleri Ayarla';

  @override
  String get setPaidInstallmentsDesc =>
      'Başka bir uygulamadan geçiyorsan, bu borç için daha önce ödediğin taksit sayısını gir.';

  @override
  String get paidInstallmentsCountLabel => 'Ödenmiş taksit sayısı';

  @override
  String get paidInstallmentsCountInvalid => '1 veya daha büyük bir sayı girin';

  @override
  String setPaidInstallmentsSuccess(int count) {
    return '$count taksit ödenmiş olarak işaretlendi';
  }

  @override
  String get enterAmount => 'Tutar Girin';

  @override
  String get partialPaymentHint => 'Boş bırakılırsa tam tutar kullanılır';

  @override
  String get bulkPayTitle => 'Toplu Ödeme Seç';

  @override
  String bulkPayConfirm(int count) {
    return '$count ödeme kaydedildi.';
  }

  @override
  String get charts => 'Grafikler';

  @override
  String get manageMembers => 'Üyeleri Yönet';

  @override
  String get leaveCategory => 'Bu Kategoriden Ayrıl';

  @override
  String leaveCategoryConfirm(String name) {
    return '\"$name\" kategorisinden ayrılmak istediğinize emin misiniz?';
  }

  @override
  String get bulkShare => 'Toplu Paylaş';

  @override
  String get shareCategory => 'Paylaş';

  @override
  String shareAllSelected(int count) {
    return 'Seçilenleri Paylaş ($count)';
  }

  @override
  String get selectCategoriesToShare => 'Kategorileri Seç';

  @override
  String addToCategories(int count) {
    return '$count kategoriye ekle';
  }

  @override
  String get shareAccessLevel => 'Erişim Seviyesi';

  @override
  String get selectAccessLevel => 'Erişim seviyesi seçin';

  @override
  String categoriesSelected(int count) {
    return '$count kategori seçildi';
  }

  @override
  String get clearSelection => 'Seçimi Temizle';

  @override
  String memberAddedSuccess(String email) {
    return '$email üye olarak eklendi.';
  }

  @override
  String get memberRemovedSuccess => 'Üye kaldırıldı.';

  @override
  String bulkShareSuccess(String email, int count) {
    return '$email $count kategoriye eklendi.';
  }

  @override
  String get memberLeftCategory => 'Kategoriden ayrıldınız.';

  @override
  String get changeAccessLevel => 'Erişim Seviyesini Değiştir';

  @override
  String get membersList => 'Üyeler Listesi';

  @override
  String get accessRead => 'Okuma';

  @override
  String get accessReadDesc => 'Sadece görüntüleyebilir';

  @override
  String get accessWrite => 'Yazma';

  @override
  String get accessWriteDesc => 'Görüntüleyebilir ve ödeme yapabilir';

  @override
  String get accessEdit => 'Düzenleme';

  @override
  String get accessEditDesc =>
      'Görüntüleyebilir, ödeme yapabilir ve düzenleyebilir';

  @override
  String get accessDelete => 'Admin';

  @override
  String get accessDeleteDesc =>
      'Tam yetki: görüntüleme, ödeme, düzenleme ve silme';

  @override
  String get noPermission => 'Bu operasyon için yetkiniz yok.';

  @override
  String errorWithMessage(String message) {
    return 'Hata: $message';
  }

  @override
  String get validationEmailRequired => 'Email gerekli';

  @override
  String get validationEmailInvalid => 'Geçerli bir email girin';

  @override
  String get validationPasswordRequired => 'Şifre gerekli';

  @override
  String get validationPasswordMinLength => 'Şifre en az 8 karakter olmalı';

  @override
  String get validationPasswordMaxLength =>
      'Şifre en fazla 72 karakter olabilir';

  @override
  String get validationConfirmPasswordRequired => 'Şifre tekrarı gerekli';

  @override
  String get validationPasswordMismatch => 'Şifreler eşleşmiyor';

  @override
  String validationFieldRequired(String fieldName) {
    return '$fieldName gerekli';
  }

  @override
  String get validationAmountRequired => 'Tutar gerekli';

  @override
  String get validationAmountInvalid => 'Geçerli bir tutar girin';

  @override
  String get validationAmountPositive => 'Tutar 0\'dan büyük olmalı';

  @override
  String get errorInvalidCredentials => 'E-posta veya şifre hatalı.';

  @override
  String get errorUnexpected => 'Beklenmeyen bir hata oluştu.';

  @override
  String get errorNetwork =>
      'İnternet bağlantısı yok. Bağlantınızı kontrol edin.';

  @override
  String get errorServer => 'Sunucu hatası oluştu. Lütfen tekrar deneyin.';

  @override
  String get errorNotFound => 'İstenen kaynak bulunamadı.';

  @override
  String get errorRateLimit =>
      'Çok fazla istek gönderildi. Lütfen bir süre bekleyin.';

  @override
  String get errorSessionExpired =>
      'Oturumunuz sona erdi. Lütfen tekrar giriş yapın.';

  @override
  String get errorDebtAlreadyCompleted => 'Bu borç zaten tamamlanmış.';

  @override
  String get errorDebtOverpayment =>
      'Ödeme tutarı, kalan borç tutarını aşamaz.';

  @override
  String get errorCategoryNameConflict => 'Bu isimde bir kategori zaten var.';

  @override
  String get errorCategoryCannotAddSelf =>
      'Kendinizi üye olarak ekleyemezsiniz.';

  @override
  String get errorCategoryMemberNotFound => 'Üye bulunamadı.';

  @override
  String get errorCategoryNotFound => 'Kategori bulunamadı.';

  @override
  String get errorCategoryMemberUserNotFound =>
      'Bu e-posta ile kayıtlı kullanıcı bulunamadı.';

  @override
  String get errorCategoryMemberAlreadyExists =>
      'Bu kullanıcı zaten kategori üyesi.';

  @override
  String get errorCategoryMigrateSameCategory =>
      'Hedef kategori, kaynak kategoriyle aynı olamaz.';

  @override
  String get errorDebtNotFound => 'Borç bulunamadı.';

  @override
  String get errorDebtAlreadyPaidThisPeriod =>
      'Bu dönem için ödeme zaten yapılmış.';

  @override
  String get errorDebtAutoPaymentEnabled =>
      'Otomatik ödeme açıkken bu işlem yapılamaz.';

  @override
  String get errorDebtConcurrentRequest =>
      'Bu borç için başka bir işlem sürüyor, lütfen tekrar deneyin.';

  @override
  String get errorDebtPaymentBeforeStart =>
      'Ödeme, borcun başlangıç tarihinden önce olamaz.';

  @override
  String get errorDebtPeriodAlreadyRecorded => 'Bu dönem zaten kaydedilmiş.';

  @override
  String get errorDebtInstallmentCountExceeded =>
      'Taksit sayısı sınırı aşıldı.';

  @override
  String get errorDebtMissingPeriodConfig => 'Ödeme periyodu bilgisi eksik.';

  @override
  String get errorIncomeNotFound => 'Gelir kaydı bulunamadı.';

  @override
  String get errorReminderDateOrTimeMissing =>
      'Hatırlatma tarihi veya saati eksik.';

  @override
  String get errorSubscriptionVerificationFailed =>
      'Abonelik doğrulaması başarısız oldu.';

  @override
  String get errorSubscriptionNotFound => 'Abonelik bulunamadı.';

  @override
  String get errorAuthEmailInUse => 'Bu e-posta adresi zaten kullanımda.';

  @override
  String get errorAuthTooManyLoginAttempts =>
      'Çok fazla giriş denemesi yapıldı. Lütfen bir süre bekleyin.';

  @override
  String get errorAuthNotValid => 'E-posta veya şifre hatalı.';

  @override
  String get errorAuthTokenRevoked =>
      'Oturumunuz sonlandırıldı. Lütfen tekrar giriş yapın.';

  @override
  String get errorAuthTokenInvalid =>
      'Geçersiz oturum. Lütfen tekrar giriş yapın.';

  @override
  String get errorAuthCurrentPasswordInvalid => 'Mevcut şifreniz hatalı.';

  @override
  String get errorAuthTokenAlreadyUsed =>
      'Bu bağlantı zaten kullanılmış. Lütfen tekrar deneyin.';

  @override
  String get errorAuthTooManyPasswordResetAttempts =>
      'Çok fazla şifre sıfırlama denemesi yapıldı. Lütfen bir süre bekleyin.';

  @override
  String get errorHouseholdInviteeNotFound =>
      'Bu e-posta ile kayıtlı bir kullanıcı bulunamadı.';

  @override
  String get errorHouseholdCannotInviteSelf => 'Kendinizi davet edemezsiniz.';

  @override
  String get errorHouseholdAlreadyLinked =>
      'Zaten aktif veya bekleyen bir haneniz var.';

  @override
  String get errorHouseholdInviteeAlreadyLinked =>
      'Bu kullanıcının zaten aktif veya bekleyen bir hanesi var.';

  @override
  String get errorHouseholdNotFound => 'Hane bulunamadı.';

  @override
  String get errorHouseholdNotPending =>
      'Bu davet artık bekleyen durumda değil.';

  @override
  String get household => 'Hane';

  @override
  String get householdDesc =>
      'Eşinizle geliri birleştirip ortak borç/gelir oranını görün';

  @override
  String get householdInviteTitle => 'Hane Daveti Gönder';

  @override
  String get householdInviteEmailLabel => 'E-posta';

  @override
  String get householdInviteButton => 'Davet Gönder';

  @override
  String get householdInviteSent => 'Davet gönderildi';

  @override
  String get householdNoHousehold => 'Henüz bir haneniz yok';

  @override
  String householdPendingAsOwner(String email) {
    return '$email için davetiniz onay bekliyor';
  }

  @override
  String householdPendingAsPartner(String email) {
    return '$email sizi hanesine davet etti';
  }

  @override
  String householdActiveWith(String email) {
    return '$email ile ortak hane';
  }

  @override
  String get householdAccept => 'Kabul Et';

  @override
  String get householdDecline => 'Reddet';

  @override
  String get householdLeave => 'Haneden Ayrıl';

  @override
  String get householdLeaveConfirmTitle => 'Haneden ayrıl?';

  @override
  String get householdLeaveConfirmBody =>
      'Ortak gelir birleştirme durdurulacak.';

  @override
  String get householdIncomeLabel => 'Hane Geliri';

  @override
  String get householdRatioLabel => 'Hane B/G Oranı';

  @override
  String get notifications => 'Bildirimler';

  @override
  String get notificationsDesc =>
      'Hatırlatıcılar, hane davetleri ve haftalık özet';

  @override
  String get notificationsEmpty => 'Henüz bildirim yok';

  @override
  String get retryButton => 'Tekrar Dene';

  @override
  String personCount(int count) {
    return '$count kişi';
  }

  @override
  String get baseCurrency => 'Baz Para Birimi';

  @override
  String get monthlySummary => 'Aylık Özet';

  @override
  String get totalPaidAmount => 'Toplam Ödendi';

  @override
  String get trendLast6Months => 'Son 6 Ay Trendi';

  @override
  String get categoryBreakdown => 'Kategori Dağılımı';

  @override
  String get debtProgressTitle => 'Borç İlerlemesi';

  @override
  String get noDataForMonth => 'Bu ay için veri yok';

  @override
  String get debtToIncomeRatioLabel => 'Borç/Gelir Oranı';

  @override
  String get pendingLabel => 'Bekleyen';

  @override
  String get skippedLabel => 'Atlanan';

  @override
  String get noIncomeSet => 'Gelir tanımlanmamış';

  @override
  String installmentsLabel(int paid, int total) {
    return '$paid / $total taksit';
  }

  @override
  String openEndedInstallments(int count) {
    return '$count taksit (açık uçlu)';
  }

  @override
  String get purchaseFailed => 'Satın alma sırasında bir hata oluştu.';

  @override
  String get storeUnavailable =>
      'Mağazaya bağlanılamadı. İnternet bağlantını kontrol et.';

  @override
  String get subscriptionPlansLoadFailed =>
      'Abonelik planları şu an yüklenemedi.';

  @override
  String debtCountLabel(int count) {
    return '$count borç';
  }

  @override
  String percentValue(String value) {
    return '%$value';
  }

  @override
  String get trendNoPayments => 'Son 6 ayda ödeme yok';

  @override
  String dueOn(String date) {
    return 'Vade $date';
  }
}
