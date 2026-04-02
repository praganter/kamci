import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../domain/entities/debt.dart';
import '../providers/debt_providers.dart';
import 'compact_debt_tile.dart';

class SwipeableTile extends ConsumerStatefulWidget {
  const SwipeableTile({super.key, required this.debt});
  final Debt debt;

  @override
  ConsumerState<SwipeableTile> createState() => _SwipeableTileState();
}

class _SwipeableTileState extends ConsumerState<SwipeableTile>
    with SingleTickerProviderStateMixin {
  static const double _actionWidth = 72.0;
  double _dragOffset = 0;

  late final AnimationController _snapCtrl;
  late final Animation<double> _snapAnim;
  double _snapFrom = 0;

  @override
  void initState() {
    super.initState();
    _snapCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 220),
    );
    _snapAnim = CurvedAnimation(parent: _snapCtrl, curve: Curves.easeOut);
    _snapCtrl.addListener(() {
      setState(() {
        _dragOffset = _snapFrom * (1 - _snapAnim.value);
      });
    });
  }

  @override
  void dispose() {
    _snapCtrl.dispose();
    super.dispose();
  }

  void _snapBack() {
    _snapFrom = _dragOffset;
    _snapCtrl.forward(from: 0);
  }

  Future<void> _onDeleteTap() async {
    final confirmed = await showConfirmDialog(
      context,
      title: context.l10n.deleteDebt,
      content: context.l10n.deleteDebtPermanentWarning,
    );
    if (!mounted) return;
    if (confirmed) {
      await ref.read(debtListProvider.notifier).deleteDebt(widget.debt.id);
      ref.invalidate(debtGroupedProvider);
    } else {
      _snapBack();
    }
  }

  Future<void> _onPaymentTap() async {
    _snapBack();
    try {
      await ref.read(debtListProvider.notifier).makePayment(widget.debt.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.check_circle_rounded,
                    color: Colors.white, size: 20),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    context.l10n.paymentRecorded(widget.debt.name),
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                ),
              ],
            ),
            backgroundColor: AppColors.statusActive,
            behavior: SnackBarBehavior.floating,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            duration: const Duration(seconds: 3),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString()),
            backgroundColor: const Color(0xFFEF4444),
            behavior: SnackBarBehavior.floating,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final showPayment = _dragOffset > 0;
    final showDelete = _dragOffset < 0;

    return GestureDetector(
      onHorizontalDragUpdate: (details) {
        setState(() {
          _dragOffset = (_dragOffset + details.delta.dx)
              .clamp(-_actionWidth, _actionWidth);
        });
      },
      onHorizontalDragEnd: (_) {
        if (_dragOffset.abs() < _actionWidth / 2) _snapBack();
      },
      child: Stack(
        children: [
          if (showPayment)
            Positioned(
              left: 0,
              top: 0,
              bottom: 0,
              width: _dragOffset,
              child: GestureDetector(
                onTap: _onPaymentTap,
                child: Container(
                  color: AppColors.statusActive,
                  child: const Center(
                    child: Icon(Icons.payments_rounded,
                        color: Colors.white, size: 26),
                  ),
                ),
              ),
            ),
          if (showDelete)
            Positioned(
              right: 0,
              top: 0,
              bottom: 0,
              width: -_dragOffset,
              child: GestureDetector(
                onTap: _onDeleteTap,
                child: Container(
                  color: const Color(0xFFEF4444),
                  child: const Center(
                    child: Icon(Icons.delete_rounded,
                        color: Colors.white, size: 26),
                  ),
                ),
              ),
            ),
          Transform.translate(
            offset: Offset(_dragOffset, 0),
            child: CompactDebtTile(debt: widget.debt),
          ),
        ],
      ),
    );
  }
}
