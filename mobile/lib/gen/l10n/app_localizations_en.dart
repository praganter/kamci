// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appName => 'DebtTracker';

  @override
  String get save => 'Save';

  @override
  String get cancel => 'Cancel';

  @override
  String get delete => 'Delete';

  @override
  String get edit => 'Edit';

  @override
  String get create => 'Create';

  @override
  String get update => 'Update';

  @override
  String get send => 'Send';

  @override
  String get remove => 'Remove';

  @override
  String get add => 'Add';

  @override
  String get select => 'Select';

  @override
  String get retry => 'Retry';

  @override
  String get welcome => 'Welcome!';

  @override
  String get signIn => 'Sign In';

  @override
  String get signUp => 'Sign Up';

  @override
  String get signOut => 'Sign Out';

  @override
  String get email => 'Email Address';

  @override
  String get password => 'Password';

  @override
  String get confirmPassword => 'Confirm Password';

  @override
  String get newPassword => 'New Password';

  @override
  String get forgotPassword => 'Forgot Password?';

  @override
  String get forgotPasswordTitle => 'Forgot Password';

  @override
  String get resetPasswordTitle => 'Reset Password';

  @override
  String get updatePassword => 'Update Password';

  @override
  String get noAccount => 'Don\'t have an account?';

  @override
  String get hasAccount => 'Already have an account?';

  @override
  String get forgotPasswordDesc =>
      'A password reset link will be sent to your email address.';

  @override
  String get resetPasswordDesc => 'Set your new password.';

  @override
  String get checkEmail => 'Check Your Email';

  @override
  String checkEmailDesc(String email) {
    return 'A password reset link has been sent to $email.';
  }

  @override
  String get invalidResetLink => 'Invalid password reset link.';

  @override
  String get passwordUpdated => 'Your password has been updated successfully.';

  @override
  String get premiumMembership => 'Premium Membership';

  @override
  String get premiumMembershipDesc => 'Access more features';

  @override
  String get debts => 'Debts';

  @override
  String get categories => 'Categories';

  @override
  String get myDebts => 'My Debts';

  @override
  String get sharedDebts => 'Shared';

  @override
  String get noDebtsYet => 'No debts yet';

  @override
  String get noSharedDebts => 'No shared debts';

  @override
  String get addDebtHint => 'Press + to add a new debt';

  @override
  String get joinSharedHint => 'Join a shared category';

  @override
  String get noCategoryDebts => 'No debts in this category yet';

  @override
  String get noDebts => 'No debts';

  @override
  String get summaryTotal => 'Total';

  @override
  String get deleteDebtPermanentWarning =>
      'This debt will be permanently deleted and cannot be undone. Are you sure?';

  @override
  String paymentRecorded(String name) {
    return 'Payment for $name has been recorded.';
  }

  @override
  String get deleteDebt => 'Delete Debt';

  @override
  String deleteDebtConfirm(String name) {
    return 'Are you sure you want to delete \"$name\"?';
  }

  @override
  String get amount => 'Amount';

  @override
  String get status => 'Status';

  @override
  String get paymentPeriod => 'Payment Period';

  @override
  String get startDate => 'Start Date';

  @override
  String get endDate => 'End Date';

  @override
  String get description => 'Description';

  @override
  String get nextReminder => 'Next Reminder';

  @override
  String paymentHistory(int count) {
    return 'Payment History ($count)';
  }

  @override
  String get noPaymentsYet => 'No payments yet';

  @override
  String get paid => 'Paid';

  @override
  String get pendingPayment => 'Pending';

  @override
  String get newDebt => 'New Debt';

  @override
  String get editDebt => 'Edit Debt';

  @override
  String get debtName => 'Debt Name';

  @override
  String get selectDate => 'Select';

  @override
  String get selectStartDate => 'Please select a start date';

  @override
  String get noCategory => 'No Category';

  @override
  String get category => 'Category';

  @override
  String get autoPayment => 'Auto Payment';

  @override
  String get categoryOptional => 'Category (Optional)';

  @override
  String get descriptionOptional => 'Description (Optional)';

  @override
  String get myCategories => 'My Categories';

  @override
  String get joinedCategories => 'Joined';

  @override
  String get noCategoriesYet => 'No categories yet';

  @override
  String get noJoinedCategories => 'No joined categories';

  @override
  String get addCategoryHint => 'Press + to create a new category';

  @override
  String get waitingInvitation => 'Waiting for a category invitation';

  @override
  String memberCount(int count) {
    return '$count member(s)';
  }

  @override
  String get deleteCategory => 'Delete Category';

  @override
  String deleteCategoryConfirm(String name) {
    return 'Are you sure you want to delete \"$name\"?';
  }

  @override
  String memberCountTitle(int count) {
    return '$count Member(s)';
  }

  @override
  String get noMembersYet => 'No members yet';

  @override
  String get addMember => 'Add Member';

  @override
  String get removeMember => 'Remove Member';

  @override
  String get removeMemberConfirm =>
      'Are you sure you want to remove this member from the category?';

  @override
  String get newCategory => 'New Category';

  @override
  String get editCategory => 'Edit Category';

  @override
  String get categoryName => 'Category Name';

  @override
  String get periodDaily => 'Daily';

  @override
  String get periodWeekly => 'Weekly';

  @override
  String get periodMonthly => 'Monthly';

  @override
  String get periodQuarterly => 'Quarterly';

  @override
  String get periodYearly => 'Yearly';

  @override
  String get statusActive => 'Active';

  @override
  String get statusInactive => 'Inactive';

  @override
  String get statusCompleted => 'Completed';

  @override
  String get currentPassword => 'Current Password';

  @override
  String get settings => 'Settings';

  @override
  String get account => 'Account';

  @override
  String get changePassword => 'Change Password';

  @override
  String get changePasswordDesc =>
      'A password reset link will be sent to your email';

  @override
  String get feedback => 'Feedback';

  @override
  String get sendFeedback => 'Send Suggestion / Complaint';

  @override
  String get feedbackDesc => 'Share your thoughts and suggestions with us';

  @override
  String get appearance => 'Appearance';

  @override
  String get language => 'Language';

  @override
  String get theme => 'Theme';

  @override
  String get themeSystem => 'System';

  @override
  String get themeLight => 'Light';

  @override
  String get themeDark => 'Dark';

  @override
  String get langSystem => 'System';

  @override
  String get langTurkish => 'Türkçe';

  @override
  String get langEnglish => 'English';

  @override
  String get visibility => 'Visibility';

  @override
  String get filterAll => 'All';

  @override
  String get filterActive => 'Active';

  @override
  String get filterInactive => 'Inactive';

  @override
  String developerCredit(String name) {
    return 'Developed by $name';
  }

  @override
  String get currency => 'Currency';
}
