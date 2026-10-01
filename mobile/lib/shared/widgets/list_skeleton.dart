import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';

/// Liste yüklenirken spinner yerine gösterilen iskelet: kart içinde
/// ikon + iki satır metin + tutar biçiminde gri bloklar, üstünden shimmer geçer.
class ListSkeleton extends StatelessWidget {
  const ListSkeleton({super.key, this.rows = 5});

  final int rows;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final block = cs.surfaceContainerHighest;

    Widget bar(double w, double h) => Container(
      width: w,
      height: h,
      decoration: BoxDecoration(
        color: block,
        borderRadius: BorderRadius.circular(6),
      ),
    );

    return Shimmer.fromColors(
      baseColor: block,
      highlightColor: cs.surface,
      child: ListView.separated(
        physics: const NeverScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        itemCount: rows,
        separatorBuilder: (_, _) => const SizedBox(height: 8),
        itemBuilder: (_, _) => Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: cs.surface,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(color: block, shape: BoxShape.circle),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    bar(140, 12),
                    const SizedBox(height: 8),
                    bar(70, 10),
                  ],
                ),
              ),
              bar(64, 12),
            ],
          ),
        ),
      ),
    );
  }
}
