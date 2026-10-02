import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart' hide TextDirection;

import '../../../../core/l10n/l10n.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/theme_helpers.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../debt/presentation/providers/debt_providers.dart';
import '../../domain/entities/monthly_summary.dart';
import '../providers/charts_providers.dart';

// ── Renk paleti (donut & trend) ───────────────────────────────────────────────

// alt-b: zümrüt öncelikli, sakin ve ayrışan tonlar (mor yok)
const _kChartColors = [
  Color(0xFF059669), // zümrüt
  Color(0xFF0EA5E9), // gök mavisi
  Color(0xFFF59E0B), // kehribar
  Color(0xFF6366F1), // indigo
  Color(0xFFEF4444), // kırmızı
  Color(0xFF14B8A6), // camgöbeği
  Color(0xFF64748B), // arduvaz
  Color(0xFFEC4899), // pembe
];

// ── Ana ekran ─────────────────────────────────────────────────────────────────

class ChartsScreen extends ConsumerWidget {
  const ChartsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final summaryAsync = ref.watch(monthlySummaryProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.charts),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(56),
          child: _MonthSelector(),
        ),
      ),
      body: summaryAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => AppErrorWidget(
          error: e,
          onRetry: () => ref.invalidate(monthlySummaryProvider),
        ),
        data: (summary) => _SummaryBody(summary: summary),
      ),
    );
  }
}

// ── Ay seçici ─────────────────────────────────────────────────────────────────

class _MonthSelector extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final month = ref.watch(selectedMonthProvider);
    final cs = Theme.of(context).colorScheme;
    final label = DateFormat(
      'MMMM yyyy',
      Localizations.localeOf(context).languageCode,
    ).format(month);
    final isCurrentMonth =
        month.year == DateTime.now().year &&
        month.month == DateTime.now().month;

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Semantics(
            container: true,
            identifier: 'chartPrevMonthBtn',
            child: IconButton(
              key: const Key('chartPrevMonthBtn'),
              icon: const Icon(Icons.chevron_left),
              onPressed: () =>
                  ref.read(selectedMonthProvider.notifier).previous(),
            ),
          ),
          Text(
            label,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w600,
              color: cs.onSurface,
            ),
          ),
          Semantics(
            container: true,
            identifier: 'chartNextMonthBtn',
            child: IconButton(
              key: const Key('chartNextMonthBtn'),
              icon: Icon(
                Icons.chevron_right,
                color: isCurrentMonth
                    ? cs.onSurfaceVariant.withValues(alpha: 0.3)
                    : null,
              ),
              onPressed: isCurrentMonth
                  ? null
                  : () => ref.read(selectedMonthProvider.notifier).next(),
            ),
          ),
        ],
      ),
    );
  }
}

// ── Özet gövde ────────────────────────────────────────────────────────────────

class _SummaryBody extends StatelessWidget {
  const _SummaryBody({required this.summary});
  final MonthlySummary summary;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
      children: [
        // ── İstatistik kartları ──────────────────────────────────────────────
        _StatsGrid(summary: summary),
        const SizedBox(height: 20),

        // ── Hane geliri (varsa) ──────────────────────────────────────────────
        if (summary.household) ...[
          _HouseholdCard(summary: summary),
          const SizedBox(height: 20),
        ],

        // ── Trend çizgi grafiği ──────────────────────────────────────────────
        if (summary.trend.isNotEmpty) ...[
          _SectionHeader(title: l10n.trendLast6Months),
          const SizedBox(height: 8),
          _TrendChartCard(points: summary.trend),
          const SizedBox(height: 20),
        ],

        // ── Kategori donut ───────────────────────────────────────────────────
        if (summary.categoryBreakdown.isNotEmpty) ...[
          _SectionHeader(title: l10n.categoryBreakdown),
          const SizedBox(height: 8),
          _CategoryDonutCard(items: summary.categoryBreakdown),
          const SizedBox(height: 20),
        ],

        // ── Borç ilerleme ────────────────────────────────────────────────────
        if (summary.debtProgress.isNotEmpty) ...[
          _SectionHeader(title: l10n.debtProgressTitle),
          const SizedBox(height: 8),
          _DebtProgressCard(items: summary.debtProgress),
        ],

        if (summary.trend.isEmpty &&
            summary.categoryBreakdown.isEmpty &&
            summary.debtProgress.isEmpty)
          Center(
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 64),
              child: Text(
                l10n.noDataForMonth,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ),
          ),
      ],
    );
  }
}

// ── Hane geliri kartı ────────────────────────────────────────────────────────

class _HouseholdCard extends StatelessWidget {
  const _HouseholdCard({required this.summary});
  final MonthlySummary summary;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final ratioText = summary.householdDebtToIncomeRatio != null
        ? '%${summary.householdDebtToIncomeRatio!.toStringAsFixed(1)}'
        : l10n.noIncomeSet;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: altBCard(cs),
      child: Row(
        children: [
          Icon(Icons.family_restroom, color: cs.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.householdIncomeLabel,
                  style: Theme.of(
                    context,
                  ).textTheme.labelSmall?.copyWith(color: cs.onSurfaceVariant),
                ),
                Text(
                  summary.householdIncomeAtMonth != null
                      ? _fmtAmount(summary.householdIncomeAtMonth!)
                      : l10n.noIncomeSet,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                l10n.householdRatioLabel,
                style: Theme.of(
                  context,
                ).textTheme.labelSmall?.copyWith(color: cs.onSurfaceVariant),
              ),
              Text(
                ratioText,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: cs.primary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

// ── İstatistik grid ───────────────────────────────────────────────────────────

class _StatsGrid extends StatelessWidget {
  const _StatsGrid({required this.summary});
  final MonthlySummary summary;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;

    final ratioText = summary.debtToIncomeRatio != null
        ? '%${summary.debtToIncomeRatio!.toStringAsFixed(1)}'
        : l10n.noIncomeSet;

    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 12,
      mainAxisSpacing: 12,
      childAspectRatio: 1.55,
      children: [
        _StatCard(
          label: l10n.totalPaidAmount,
          value: _fmtAmount(summary.totalPaid),
          sub: '${summary.paidCount} borç',
          color: AppColors.success,
          icon: Icons.check_circle_outline,
        ),
        _StatCard(
          label: l10n.pendingLabel,
          value: _fmtAmount(summary.pendingAmount),
          sub: '${summary.pendingCount} borç',
          color: AppColors.warning,
          icon: Icons.hourglass_empty_outlined,
        ),
        _StatCard(
          label: l10n.skippedLabel,
          value: _fmtAmount(summary.skippedAmount),
          sub: '${summary.skippedCount} borç',
          color: cs.error,
          icon: Icons.skip_next_outlined,
        ),
        _StatCard(
          label: l10n.debtToIncomeRatioLabel,
          value: ratioText,
          sub: summary.incomeAtMonth != null
              ? _fmtAmount(summary.incomeAtMonth!)
              : '',
          color: cs.primary,
          icon: Icons.pie_chart_outline,
        ),
      ],
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({
    required this.label,
    required this.value,
    required this.sub,
    required this.color,
    required this.icon,
  });

  final String label;
  final String value;
  final String sub;
  final Color color;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: altBCard(cs),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            children: [
              Icon(icon, size: 16, color: color),
              const SizedBox(width: 4),
              Expanded(
                child: Text(
                  label,
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: cs.onSurfaceVariant,
                    fontSize: 10,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                value,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: color,
                  fontWeight: FontWeight.w700,
                  fontSize: 15,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              if (sub.isNotEmpty)
                Text(
                  sub,
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: cs.onSurfaceVariant,
                    fontSize: 10,
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

// ── Bölüm başlığı ─────────────────────────────────────────────────────────────

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.title});
  final String title;

  @override
  Widget build(BuildContext context) {
    return Text(
      title,
      style: Theme.of(context).textTheme.titleSmall?.copyWith(
        fontWeight: FontWeight.w700,
        letterSpacing: 0.3,
      ),
    );
  }
}

// ── Trend çizgi grafiği ───────────────────────────────────────────────────────

class _TrendChartCard extends StatelessWidget {
  const _TrendChartCard({required this.points});
  final List<TrendPoint> points;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final values = points.map((p) => p.totalPaid).toList();
    final months = points.map((p) => _shortMonth(p.month)).toList();

    return Container(
      height: 200,
      padding: const EdgeInsets.fromLTRB(8, 16, 16, 8),
      decoration: altBCard(cs),
      // Giriş animasyonu: çizgi tabandan yükselir
      child: TweenAnimationBuilder<double>(
        tween: Tween(begin: 0, end: 1),
        duration: const Duration(milliseconds: 800),
        curve: Curves.easeOutCubic,
        builder: (context, t, _) => CustomPaint(
          painter: _TrendPainter(
            values: values,
            months: months,
            lineColor: cs.primary,
            gridColor: cs.outlineVariant,
            labelColor: cs.onSurfaceVariant,
            progress: t,
          ),
          size: Size.infinite,
        ),
      ),
    );
  }

  String _shortMonth(String yyyyMm) {
    try {
      final parts = yyyyMm.split('-');
      final dt = DateTime(int.parse(parts[0]), int.parse(parts[1]));
      return DateFormat('MMM').format(dt);
    } catch (_) {
      return yyyyMm;
    }
  }
}

class _TrendPainter extends CustomPainter {
  const _TrendPainter({
    required this.values,
    required this.months,
    required this.lineColor,
    required this.gridColor,
    required this.labelColor,
    this.progress = 1,
  });

  final List<double> values;
  final List<String> months;
  final Color lineColor;
  final Color gridColor;
  final Color labelColor;

  /// 0→1 giriş animasyonu ilerlemesi (değerler tabandan yükselir).
  final double progress;

  static const _leftPad = 48.0;
  static const _rightPad = 8.0;
  static const _topPad = 8.0;
  static const _bottomPad = 28.0;

  @override
  void paint(Canvas canvas, Size size) {
    if (values.isEmpty) return;

    final chartW = size.width - _leftPad - _rightPad;
    final chartH = size.height - _topPad - _bottomPad;
    final maxVal = values.reduce(max);
    if (maxVal == 0) return;

    final labelStyle = TextStyle(
      color: labelColor,
      fontSize: 9,
      fontWeight: FontWeight.w500,
    );

    // Grid + Y labels
    final gridPaint = Paint()
      ..color = gridColor
      ..strokeWidth = 0.8;

    for (int i = 0; i <= 4; i++) {
      final y = _topPad + (i / 4) * chartH;
      canvas.drawLine(
        Offset(_leftPad, y),
        Offset(size.width - _rightPad, y),
        gridPaint,
      );
      final val = maxVal * (1 - i / 4);
      _drawText(
        canvas,
        _fmtAmount(val),
        Offset(0, y - 5),
        labelStyle,
        maxWidth: _leftPad - 4,
      );
    }

    // Positions
    final n = values.length;
    final positions = List.generate(n, (i) {
      final x = _leftPad + (n == 1 ? chartW / 2 : (i / (n - 1)) * chartW);
      final y = _topPad + chartH - (values[i] / maxVal) * chartH * progress;
      return Offset(x, y);
    });

    // Area fill
    final fillPath = Path()..moveTo(positions.first.dx, _topPad + chartH);
    for (final p in positions) {
      fillPath.lineTo(p.dx, p.dy);
    }
    fillPath
      ..lineTo(positions.last.dx, _topPad + chartH)
      ..close();

    canvas.drawPath(
      fillPath,
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            lineColor.withValues(alpha: 0.25),
            lineColor.withValues(alpha: 0.0),
          ],
        ).createShader(Rect.fromLTWH(_leftPad, _topPad, chartW, chartH)),
    );

    // Line
    final linePath = Path()..moveTo(positions.first.dx, positions.first.dy);
    for (int i = 1; i < positions.length; i++) {
      linePath.lineTo(positions[i].dx, positions[i].dy);
    }
    canvas.drawPath(
      linePath,
      Paint()
        ..color = lineColor
        ..strokeWidth = 2.5
        ..style = PaintingStyle.stroke
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round,
    );

    // Dots
    final dotFill = Paint()..color = lineColor;
    final dotHole = Paint()..color = Colors.white;
    for (final p in positions) {
      canvas.drawCircle(p, 5, dotFill);
      canvas.drawCircle(p, 3, dotHole);
    }

    // X labels
    for (int i = 0; i < positions.length; i++) {
      if (i < months.length) {
        _drawText(
          canvas,
          months[i],
          Offset(positions[i].dx - 14, _topPad + chartH + 6),
          labelStyle,
          maxWidth: 28,
        );
      }
    }
  }

  void _drawText(
    Canvas canvas,
    String text,
    Offset offset,
    TextStyle style, {
    double maxWidth = 80,
  }) {
    final tp = TextPainter(
      text: TextSpan(text: text, style: style),
      textDirection: TextDirection.ltr,
    )..layout(maxWidth: maxWidth);
    tp.paint(canvas, offset);
  }

  @override
  bool shouldRepaint(_TrendPainter old) =>
      old.values != values ||
      old.lineColor != lineColor ||
      old.progress != progress;
}

// ── Kategori donut ────────────────────────────────────────────────────────────

class _CategoryDonutCard extends StatelessWidget {
  const _CategoryDonutCard({required this.items});
  final List<CategoryBreakdownItem> items;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: altBCard(cs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          SizedBox(
            width: 120,
            height: 120,
            // Giriş animasyonu: dilimler saat yönünde açılır
            child: TweenAnimationBuilder<double>(
              tween: Tween(begin: 0, end: 1),
              duration: const Duration(milliseconds: 800),
              curve: Curves.easeOutCubic,
              builder: (context, t, _) => CustomPaint(
                painter: _DonutPainter(
                  items: items,
                  colors: _kChartColors,
                  bgColor: cs.surfaceContainer,
                  progress: t,
                ),
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                for (int i = 0; i < items.length; i++)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 3),
                    child: Row(
                      children: [
                        Container(
                          width: 10,
                          height: 10,
                          decoration: BoxDecoration(
                            color: _kChartColors[i % _kChartColors.length],
                            borderRadius: BorderRadius.circular(3),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            items[i].categoryName,
                            style: Theme.of(
                              context,
                            ).textTheme.bodySmall?.copyWith(fontSize: 11),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        Text(
                          '%${items[i].percentage.toStringAsFixed(1)}',
                          style: Theme.of(context).textTheme.labelSmall
                              ?.copyWith(
                                fontWeight: FontWeight.w700,
                                color: _kChartColors[i % _kChartColors.length],
                                fontSize: 11,
                              ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DonutPainter extends CustomPainter {
  const _DonutPainter({
    required this.items,
    required this.colors,
    required this.bgColor,
    this.progress = 1,
  });

  final List<CategoryBreakdownItem> items;
  final List<Color> colors;
  final Color bgColor;

  /// 0→1 giriş animasyonu ilerlemesi (toplam açı bununla ölçeklenir).
  final double progress;

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final outerRadius = min(size.width, size.height) / 2 - 2;
    final strokeW = outerRadius * 0.4;
    final arcRadius = outerRadius - strokeW / 2;

    double startAngle = -pi / 2;
    for (int i = 0; i < items.length; i++) {
      final sweep = (items[i].percentage / 100) * 2 * pi * progress;
      final gap = items.length > 1 ? 0.04 : 0.0;
      canvas.drawArc(
        Rect.fromCircle(center: center, radius: arcRadius),
        startAngle + gap / 2,
        sweep - gap,
        false,
        Paint()
          ..color = colors[i % colors.length]
          ..strokeWidth = strokeW
          ..style = PaintingStyle.stroke
          ..strokeCap = StrokeCap.butt,
      );
      startAngle += sweep;
    }
  }

  @override
  bool shouldRepaint(_DonutPainter old) =>
      old.items != items || old.progress != progress;
}

// ── Borç ilerleme listesi ─────────────────────────────────────────────────────

class _DebtProgressCard extends StatelessWidget {
  const _DebtProgressCard({required this.items});
  final List<DebtProgressItem> items;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final l10n = context.l10n;

    return Container(
      decoration: altBCard(cs),
      child: Column(
        children: [
          for (int i = 0; i < items.length; i++) ...[
            if (i > 0)
              Divider(
                height: 1,
                indent: 16,
                endIndent: 16,
                color: cs.outlineVariant,
              ),
            _ProgressTile(item: items[i], l10n: l10n),
          ],
        ],
      ),
    );
  }
}

class _ProgressTile extends StatelessWidget {
  const _ProgressTile({required this.item, required this.l10n});
  final DebtProgressItem item;
  final dynamic l10n;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final progress = item.completionPercentage != null
        ? item.completionPercentage! / 100
        : null;
    final progressColor = item.isCompleted ? AppColors.success : cs.primary;

    final installmentText = item.isOpenEnded
        ? l10n.openEndedInstallments(item.paidInstallments)
        : l10n.installmentsLabel(
            item.paidInstallments,
            item.totalInstallments!,
          );

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  item.debtName,
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 8),
              if (item.isCompleted)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 6,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.success.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    l10n.statusCompleted,
                    style: const TextStyle(
                      color: AppColors.success,
                      fontSize: 10,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              if (!item.isCompleted && progress != null)
                Text(
                  '%${item.completionPercentage!.toStringAsFixed(0)}',
                  style: TextStyle(
                    color: progressColor,
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 6),
          if (progress != null)
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: progress.clamp(0.0, 1.0),
                backgroundColor: progressColor.withValues(alpha: 0.12),
                valueColor: AlwaysStoppedAnimation<Color>(progressColor),
                minHeight: 6,
              ),
            )
          else
            // Açık uçlu — sadece bar göster dolu
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: null,
                backgroundColor: cs.primary.withValues(alpha: 0.12),
                valueColor: AlwaysStoppedAnimation<Color>(cs.primary),
                minHeight: 6,
              ),
            ),
          const SizedBox(height: 4),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                installmentText,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: cs.onSurfaceVariant,
                  fontSize: 10,
                ),
              ),
              Text(
                '${_fmtAmount(item.totalPaid)} ${item.currency}',
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: cs.onSurfaceVariant,
                  fontSize: 10,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

// ── Yardımcılar ───────────────────────────────────────────────────────────────

String _fmtAmount(double amount) {
  if (amount >= 1000000) return '${(amount / 1000000).toStringAsFixed(1)}M';
  if (amount >= 1000) return '${(amount / 1000).toStringAsFixed(1)}K';
  return amount.toStringAsFixed(0);
}
