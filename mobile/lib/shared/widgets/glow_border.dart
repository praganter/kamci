import 'dart:math' as math;

import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

/// Premium vurgusu: içeriğin etrafında yavaş dönen gradient çerçeve +
/// hafifçe nefes alan glow. Dikkat çeker ama rahatsız etmez (~4 sn tur).
///
/// Statik çizilir (dönmez) eğer:
/// - sistemde "animasyonları azalt" açıksa (erişilebilirlik),
/// - MAESTRO test build'iyse — sonsuz animasyon Maestro'nun
///   `waitForAnimationToEnd` adımını her seferinde zaman aşımına sokar,
/// - [enabled] false ise (ör. pasif buton).
class GlowBorder extends StatefulWidget {
  const GlowBorder({
    super.key,
    required this.child,
    this.radius = 14,
    this.width = 2,
    this.enabled = true,
  });

  final Widget child;
  final double radius;
  final double width;
  final bool enabled;

  static const _isMaestro = bool.fromEnvironment('MAESTRO');

  static const _colors = [
    Color(0xFFF59E0B), // kehribar
    Color(0xFFF97316), // turuncu
    AppColors.primary, // zümrüt
    Color(0xFFF59E0B),
  ];

  @override
  State<GlowBorder> createState() => _GlowBorderState();
}

class _GlowBorderState extends State<GlowBorder>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 4),
  );

  bool _animate(BuildContext context) =>
      widget.enabled &&
      !GlowBorder._isMaestro &&
      !MediaQuery.disableAnimationsOf(context);

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _sync();
  }

  @override
  void didUpdateWidget(GlowBorder old) {
    super.didUpdateWidget(old);
    _sync();
  }

  void _sync() {
    if (_animate(context)) {
      if (!_ctrl.isAnimating) _ctrl.repeat();
    } else {
      _ctrl.stop();
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.enabled) return widget.child;

    return AnimatedBuilder(
      animation: _ctrl,
      // child dışarıda: her karede sadece çerçeve yeniden çizilir
      child: ClipRRect(
        borderRadius: BorderRadius.circular(widget.radius - widget.width),
        child: widget.child,
      ),
      builder: (context, child) {
        final t = _ctrl.value;
        // Glow iki tur boyunca bir kez nefes alır (0.18 → 0.38 opaklık)
        final pulse = 0.28 + 0.10 * math.sin(t * 2 * math.pi);
        return Container(
          padding: EdgeInsets.all(widget.width),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(widget.radius),
            gradient: SweepGradient(
              colors: GlowBorder._colors,
              transform: GradientRotation(t * 2 * math.pi),
            ),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFFF59E0B).withValues(alpha: pulse),
                blurRadius: 18,
                spreadRadius: 0.5,
              ),
            ],
          ),
          child: child,
        );
      },
    );
  }
}
