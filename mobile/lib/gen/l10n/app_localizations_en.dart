// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appName => 'Kamci';

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
  String get firstPaymentDate => 'First Payment Date';

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
  String get joinedCategories => 'Shared';

  @override
  String get noCategoriesYet => 'No categories yet';

  @override
  String get noJoinedCategories => 'No shared categories';

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
  String get transferAndDeleteCategory => 'Transfer Debts & Delete';

  @override
  String get deleteWithDebts => 'Delete with Debts';

  @override
  String get selectTargetCategory => 'Select Target Category';

  @override
  String transferAndDeleteConfirm(String name) {
    return 'Debts in \"$name\" will be moved to the selected category, then the category will be deleted.';
  }

  @override
  String deleteWithDebtsConfirm(String name) {
    return 'Category \"$name\" and all its debts will be deleted. This cannot be undone.';
  }

  @override
  String migratedDebtCount(int count) {
    return '$count debts migrated to target category';
  }

  @override
  String get categoryName => 'Category Name';

  @override
  String get periodOneTime => 'One-Time';

  @override
  String get periodDaily => 'Daily';

  @override
  String get periodWeekly => 'Weekly';

  @override
  String get periodBiWeekly => 'Bi-weekly';

  @override
  String get periodMonthly => 'Monthly';

  @override
  String get periodQuarterly => 'Quarterly';

  @override
  String get periodHalfYearly => 'Half-yearly';

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
  String get appearance => 'App Settings';

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
  String get visibility => 'Debt Visibility';

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

  @override
  String get installmentCount => 'Installment Count';

  @override
  String get installmentCountOptional => 'Total Installment Count (Optional)';

  @override
  String installmentProgress(int paid, int total) {
    return '$paid / $total';
  }

  @override
  String get openEnded => 'Open-Ended';

  @override
  String openEndedProgress(int count) {
    return '$count';
  }

  @override
  String get memberEmail => 'Member Email Address';

  @override
  String get reminder => 'Reminder';

  @override
  String get reminderDate => 'Reminder Date';

  @override
  String get reminderTime => 'Reminder Time';

  @override
  String get reminderChannels => 'Notification Channels';

  @override
  String get channelEmail => 'Email';

  @override
  String get channelInApp => 'In-App';

  @override
  String get premiumRequiredTitle => 'Premium Required';

  @override
  String premiumLimitReached(int limit, String feature) {
    return 'In the free version, you can add at most $limit $feature. Upgrade to Premium to continue.';
  }

  @override
  String premiumFeatureRequired(String feature) {
    return 'Upgrade to Premium to use the $feature feature.';
  }

  @override
  String get maybeLater => 'Maybe Later';

  @override
  String get premium => 'Premium';

  @override
  String get getPremium => 'Get Premium';

  @override
  String get goPremium => 'Go Premium';

  @override
  String get premiumActive => 'Active';

  @override
  String get premiumUnlockDesc => 'Unlock the full potential of Kamci';

  @override
  String get premiumUnlimitedDesc => 'Unlimited debts, categories & more';

  @override
  String renewsOn(String date) {
    return 'Renews $date';
  }

  @override
  String get bestValue => 'Best Value';

  @override
  String get autoRenewInfo => 'Renews automatically. Cancel anytime.';

  @override
  String get restorePurchases => 'Restore Purchases';

  @override
  String get featureUnlimitedDebts => 'Unlimited debts';

  @override
  String get featureUnlimitedCategories => 'Unlimited categories';

  @override
  String get featureReminders => 'Debt reminders & notifications';

  @override
  String get featureCategorySharing => 'Category sharing with friends';

  @override
  String get perMonth => '/ month';

  @override
  String get perYear => '/ year';

  @override
  String get sharedPremiumDesc =>
      'You need to be a Premium member to view shared debts and manage debts with friends.';

  @override
  String get userId => 'User ID';

  @override
  String get userIdHint => 'Enter User ID';

  @override
  String get accessLevel => 'Access Level';

  @override
  String get skipPayment => 'Skip Payment';

  @override
  String get skipPaymentConfirm =>
      'Are you sure you want to skip this payment period?';

  @override
  String paymentSkipped(String name) {
    return '$name payment period skipped.';
  }

  @override
  String get income => 'Income';

  @override
  String get incomeHistory => 'Income History';

  @override
  String get addIncome => 'Add Income';

  @override
  String get noIncomeYet => 'No income records yet';

  @override
  String get incomeAmount => 'Income Amount';

  @override
  String get incomeNote => 'Note (Optional)';

  @override
  String get effectiveFrom => 'Effective From';

  @override
  String get deleteIncome => 'Delete Income';

  @override
  String get deleteIncomeConfirm =>
      'Are you sure you want to delete this income record?';

  @override
  String get reminderChannelRequired =>
      'At least one notification channel must be selected';

  @override
  String get reminderDateRequired => 'Reminder date is required';

  @override
  String get reminderTimeRequired => 'Reminder time is required';

  @override
  String get paymentActions => 'Payment Actions';

  @override
  String get partialPayment => 'Partial Payment';

  @override
  String get fullPayment => 'Full Payment';

  @override
  String get bulkPayment => 'Bulk Payment';

  @override
  String get enterAmount => 'Enter Amount';

  @override
  String get partialPaymentHint => 'Leave empty to use full amount';

  @override
  String get bulkPayTitle => 'Select Bulk Payments';

  @override
  String bulkPayConfirm(int count) {
    return '$count payments recorded.';
  }

  @override
  String get charts => 'Charts';

  @override
  String get manageMembers => 'Manage Members';

  @override
  String get leaveCategory => 'Leave Category';

  @override
  String leaveCategoryConfirm(String name) {
    return 'Are you sure you want to leave \"$name\"?';
  }

  @override
  String get bulkShare => 'Bulk Share';

  @override
  String get shareCategory => 'Share';

  @override
  String shareAllSelected(int count) {
    return 'Share All Selected ($count)';
  }

  @override
  String get selectCategoriesToShare => 'Select Categories';

  @override
  String addToCategories(int count) {
    return 'Add to $count categories';
  }

  @override
  String get shareAccessLevel => 'Access Level';

  @override
  String get selectAccessLevel => 'Select access level';

  @override
  String categoriesSelected(int count) {
    return '$count categories selected';
  }

  @override
  String get clearSelection => 'Clear Selection';

  @override
  String memberAddedSuccess(String email) {
    return '$email added as member.';
  }

  @override
  String get memberRemovedSuccess => 'Member removed.';

  @override
  String bulkShareSuccess(String email, int count) {
    return '$email added to $count categories.';
  }

  @override
  String get memberLeftCategory => 'You have left the category.';

  @override
  String get changeAccessLevel => 'Change Access Level';

  @override
  String get membersList => 'Members List';

  @override
  String get accessRead => 'Read';

  @override
  String get accessReadDesc => 'View only';

  @override
  String get accessWrite => 'Write';

  @override
  String get accessWriteDesc => 'Can view and make payments';

  @override
  String get accessEdit => 'Edit';

  @override
  String get accessEditDesc => 'Can view, make payments and edit';

  @override
  String get accessDelete => 'Admin';

  @override
  String get accessDeleteDesc => 'Full access: view, pay, edit and delete';

  @override
  String get noPermission => 'You don\'t have permission for this operation.';

  @override
  String errorWithMessage(String message) {
    return 'Error: $message';
  }

  @override
  String get validationEmailRequired => 'Email is required';

  @override
  String get validationEmailInvalid => 'Enter a valid email address';

  @override
  String get validationPasswordRequired => 'Password is required';

  @override
  String get validationPasswordMinLength =>
      'Password must be at least 8 characters';

  @override
  String get validationConfirmPasswordRequired =>
      'Confirm password is required';

  @override
  String get validationPasswordMismatch => 'Passwords do not match';

  @override
  String validationFieldRequired(String fieldName) {
    return '$fieldName is required';
  }

  @override
  String get validationAmountRequired => 'Amount is required';

  @override
  String get validationAmountInvalid => 'Enter a valid amount';

  @override
  String get validationAmountPositive => 'Amount must be greater than 0';

  @override
  String get errorInvalidCredentials => 'Incorrect email or password.';

  @override
  String get errorUnexpected => 'An unexpected error occurred.';

  @override
  String get errorNetwork =>
      'No internet connection. Please check your connection.';

  @override
  String get errorServer => 'A server error occurred. Please try again.';

  @override
  String get errorNotFound => 'The requested resource was not found.';

  @override
  String get errorRateLimit => 'Too many requests. Please wait a moment.';

  @override
  String get errorSessionExpired =>
      'Your session has expired. Please sign in again.';

  @override
  String get retryButton => 'Try Again';

  @override
  String personCount(int count) {
    return '$count people';
  }

  @override
  String get baseCurrency => 'Base Currency';

  @override
  String get monthlySummary => 'Monthly Summary';

  @override
  String get totalPaidAmount => 'Total Paid';

  @override
  String get trendLast6Months => 'Last 6 Months Trend';

  @override
  String get categoryBreakdown => 'Category Breakdown';

  @override
  String get debtProgressTitle => 'Debt Progress';

  @override
  String get noDataForMonth => 'No data for this month';

  @override
  String get debtToIncomeRatioLabel => 'Debt/Income Ratio';

  @override
  String get pendingLabel => 'Pending';

  @override
  String get skippedLabel => 'Skipped';

  @override
  String get noIncomeSet => 'No income set';

  @override
  String installmentsLabel(int paid, int total) {
    return '$paid / $total installments';
  }

  @override
  String openEndedInstallments(int count) {
    return '$count installments (open-ended)';
  }
}
