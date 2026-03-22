// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Turkish (`tr`).
class AppLocalizationsTr extends AppLocalizations {
  AppLocalizationsTr([String locale = 'tr']) : super(locale);

  @override
  String get appName => 'DebtTracker';

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
  String get premiumMembership => 'Premium Üyelik';

  @override
  String get premiumMembershipDesc => 'Daha fazla özelliğe erişin';

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
  String get categoryOptional => 'Kategori (İsteğe Bağlı)';

  @override
  String get descriptionOptional => 'Açıklama (İsteğe Bağlı)';

  @override
  String get myCategories => 'Sahip Olduklarım';

  @override
  String get joinedCategories => 'Üye Olduklarım';

  @override
  String get noCategoriesYet => 'Henüz kategori yok';

  @override
  String get noJoinedCategories => 'Üye olduğun kategori yok';

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
  String get categoryName => 'Kategori Adı';

  @override
  String get periodDaily => 'Günlük';

  @override
  String get periodWeekly => 'Haftalık';

  @override
  String get periodMonthly => 'Aylık';

  @override
  String get periodQuarterly => 'Üç Aylık';

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
  String get appearance => 'Görünüm';

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
  String developerCredit(String name) {
    return 'Developed by $name';
  }
}
