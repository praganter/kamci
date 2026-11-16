import 'package:dt_mobile/core/providers/app_settings_provider.dart';
import 'package:dt_mobile/features/auth/domain/entities/auth_tokens.dart';
import 'package:dt_mobile/features/auth/presentation/providers/auth_provider.dart';
import 'package:dt_mobile/features/auth/presentation/providers/auth_state.dart';
import 'package:dt_mobile/features/category/data/repositories/category_repository_impl.dart';
import 'package:dt_mobile/features/category/domain/entities/category.dart';
import 'package:dt_mobile/features/category/domain/repositories/category_repository.dart';
import 'package:dt_mobile/features/debt/data/repositories/debt_repository_impl.dart';
import 'package:dt_mobile/features/debt/domain/entities/category_with_debts.dart';
import 'package:dt_mobile/features/debt/domain/entities/debt.dart';
import 'package:dt_mobile/features/debt/domain/entities/debt_summary.dart';
import 'package:dt_mobile/features/debt/domain/repositories/debt_repository.dart';
import 'package:dt_mobile/features/debt/presentation/providers/debt_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fakes.dart';

class _FakeDebtRepo extends Fake implements DebtRepository {
  final groupedCalls = <(String, bool)>[];
  final paid = <(String, double?)>[];
  List<CategoryWithDebts> grouped = [];

  @override
  Future<List<CategoryWithDebts>> getGrouped(String month, bool isOwner) async {
    groupedCalls.add((month, isOwner));
    return grouped;
  }

  @override
  Future<Debt> pay(String id, {double? amountPaid, DateTime? paidTime}) async {
    paid.add((id, amountPaid));
    return Debt(
      id: id,
      name: 'Debt $id',
      ownerId: 'me',
      firstPaymentDate: DateTime(2026, 1, 1),
      paymentPeriodType: PaymentPeriodType.monthly,
      amount: 500,
      status: DebtStatus.active,
      autoPayment: false,
      paymentStatus: PaymentStatus.partiallyPaid,
      payments: const [],
    );
  }
}

class _FakeCategoryRepo extends Fake implements CategoryRepository {
  _FakeCategoryRepo(this.categories);
  final List<Category> categories;

  @override
  Future<List<Category>> getAll() async => categories;
}

DebtSummary _debt(String id, String categoryId) => DebtSummary(
  id: id,
  name: 'Debt $id',
  categoryId: categoryId,
  paidCount: 0,
  paymentPeriodType: PaymentPeriodType.monthly,
  amount: 500,
  currency: 'TRY',
  status: DebtStatus.active,
  paymentStatus: PaymentStatus.unpaid,
  autoPayment: false,
  paidCurrentPeriod: false,
  hasReminder: false,
);

class _SignedIn extends AuthNotifier {
  @override
  AuthState build() => AuthState.authenticated(
    AuthTokens(accessToken: fakeJwt(sub: 'me'), refreshToken: 'r'),
  );
}

void main() {
  late _FakeDebtRepo debts;
  late ProviderContainer c;

  ProviderContainer container(List<Category> categories) {
    final container = ProviderContainer(
      overrides: [
        authNotifierProvider.overrideWith(_SignedIn.new),
        debtRepositoryProvider.overrideWithValue(debts),
        categoryRepositoryProvider.overrideWithValue(
          _FakeCategoryRepo(categories),
        ),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  setUp(() => debts = _FakeDebtRepo());

  test('debtGrouped queries the selected month as YYYY-MM', () async {
    c = container([]);
    c.read(selectedMonthProvider.notifier).setMonth(DateTime(2026, 3));

    await c.read(debtGroupedProvider(false).future);

    expect(debts.groupedCalls, [('2026-03', false)]);
  });

  test('owned tab lists own empty categories, never other people\'s', () async {
    debts.grouped = [
      CategoryWithDebts(
        categoryId: 'c1',
        categoryName: 'Home',
        owned: true,
        debts: [_debt('d1', 'c1')],
      ),
    ];
    c = container(const [
      Category(id: 'c1', ownerId: 'me', name: 'Home', members: []),
      Category(id: 'c2', ownerId: 'me', name: 'Empty mine', members: []),
      Category(id: 'c3', ownerId: 'someone-else', name: 'Shared', members: []),
    ]);

    final groups = await c.read(debtGroupedProvider(true).future);

    expect(groups.map((g) => g.categoryId), ['c1', 'c2']);
    expect(groups.last.debts, isEmpty);
  });

  test('shared tab does not add empty categories', () async {
    debts.grouped = [];
    c = container(const [
      Category(id: 'c2', ownerId: 'me', name: 'Empty mine', members: []),
    ]);

    expect(await c.read(debtGroupedProvider(false).future), isEmpty);
  });

  test('DebtList flattens the groups of the active tab', () async {
    debts.grouped = [
      CategoryWithDebts(
        categoryId: 'c1',
        categoryName: 'A',
        owned: true,
        debts: [_debt('d1', 'c1'), _debt('d2', 'c1')],
      ),
      CategoryWithDebts(
        categoryId: 'c2',
        categoryName: 'B',
        owned: true,
        debts: [_debt('d3', 'c2')],
      ),
    ];
    c = container(const [
      Category(id: 'c1', ownerId: 'me', name: 'A', members: []),
      Category(id: 'c2', ownerId: 'me', name: 'B', members: []),
    ]);

    final list = await c.read(debtListProvider.future);
    expect(list.map((d) => d.id), ['d1', 'd2', 'd3']);

    c.read(debtTabProvider.notifier).state = 1; // "shared with me" tab
    await c.read(debtListProvider.future);
    expect(debts.groupedCalls.last.$2, isFalse);
  });

  test('makePayment hits the repo and refetches both tabs', () async {
    c = container([]);
    await c.read(debtGroupedProvider(true).future);
    await c.read(debtGroupedProvider(false).future);
    final before = debts.groupedCalls.length;

    await c.read(debtListProvider.notifier).makePayment('d1', amountPaid: 40);

    expect(debts.paid, [('d1', 40.0)]);
    // Providers are auto-dispose; re-reading proves the cache was invalidated.
    await c.read(debtGroupedProvider(true).future);
    expect(debts.groupedCalls.length, greaterThan(before));
  });
}
