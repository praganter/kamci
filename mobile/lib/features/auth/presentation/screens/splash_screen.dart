import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/constants/app_constants.dart';

class SplashScreen extends ConsumerWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const _DtLogo(size: 88),
            const SizedBox(height: 28),
            _BrandName(context: context),
            const SizedBox(height: 72),
            SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(
                strokeWidth: 2.5,
                color: Theme.of(context).colorScheme.primary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DtLogo extends StatelessWidget {
  const _DtLogo({required this.size});
  final double size;

  @override
  Widget build(BuildContext context) {
    final primary = Theme.of(context).colorScheme.primary;
    final primaryContainer = Theme.of(context).colorScheme.primaryContainer;
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [primary, primary.withValues(alpha: 0.75)],
        ),
        borderRadius: BorderRadius.circular(size * 0.28),
        boxShadow: [
          BoxShadow(
            color: primary.withValues(alpha: 0.35),
            blurRadius: 24,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          Positioned(
            bottom: size * 0.12,
            right: size * 0.1,
            child: Icon(
              Icons.account_balance_wallet_rounded,
              size: size * 0.45,
              color: Colors.white.withValues(alpha: 0.18),
            ),
          ),
          Text(
            'DT',
            style: TextStyle(
              color: Colors.white,
              fontSize: size * 0.38,
              fontWeight: FontWeight.w800,
              letterSpacing: -1.5,
            ),
          ),
        ],
      ),
    );
  }
}

class _BrandName extends StatelessWidget {
  const _BrandName({required this.context});
  final BuildContext context;

  @override
  Widget build(BuildContext context) {
    return RichText(
      text: TextSpan(
        children: [
          TextSpan(
            text: 'Debt',
            style: TextStyle(
              color: Theme.of(context).colorScheme.primary,
              fontSize: 30,
              fontWeight: FontWeight.w800,
              letterSpacing: -0.5,
            ),
          ),
          TextSpan(
            text: 'Tracker',
            style: TextStyle(
              color: Theme.of(context).colorScheme.onSurface,
              fontSize: 30,
              fontWeight: FontWeight.w300,
              letterSpacing: -0.5,
            ),
          ),
        ],
      ),
    );
  }
}
