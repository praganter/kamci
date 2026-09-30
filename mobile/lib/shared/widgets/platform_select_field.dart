import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class PlatformSelectItem<T> {
  const PlatformSelectItem({required this.value, required this.label});
  final T value;
  final String label;
}

class PlatformSelectField<T> extends StatefulWidget {
  const PlatformSelectField({
    super.key,
    required this.label,
    this.prefixIcon,
    required this.value,
    required this.items,
    required this.onChanged,
    this.validator,
    this.hint,
    this.semanticsId,
  });

  final String label;
  final Widget? prefixIcon;
  final T? value;
  final List<PlatformSelectItem<T>> items;
  final ValueChanged<T?> onChanged;
  final FormFieldValidator<T>? validator;
  final String? hint;
  final String? semanticsId;

  @override
  State<PlatformSelectField<T>> createState() => _PlatformSelectFieldState<T>();
}

class _PlatformSelectFieldState<T> extends State<PlatformSelectField<T>> {
  final _fieldKey = GlobalKey<FormFieldState<T>>();

  @override
  void didUpdateWidget(PlatformSelectField<T> oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.value != widget.value) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _fieldKey.currentState?.didChange(widget.value);
      });
    }
  }

  Future<void> _showPicker(
    BuildContext context,
    FormFieldState<T> state,
  ) async {
    final items = widget.items;
    if (items.isEmpty) return;

    final currentIdx = state.value == null
        ? 0
        : items
              .indexWhere((i) => i.value == state.value)
              .clamp(0, items.length - 1);

    final mloc = MaterialLocalizations.of(context);
    final cs = Theme.of(context).colorScheme;

    await showModalBottomSheet<void>(
      context: context,
      isDismissible: false,
      enableDrag: false,
      backgroundColor: Colors.transparent,
      builder: (ctx) => _SelectPickerSheet(
        initialIndex: currentIdx,
        labels: items.map((i) => i.label).toList(),
        cancelLabel: mloc.cancelButtonLabel,
        confirmLabel: mloc.okButtonLabel,
        colorScheme: cs,
        onConfirm: (idx) {
          final selected = items[idx].value;
          state.didChange(selected);
          widget.onChanged(selected);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isIOS = Theme.of(context).platform == TargetPlatform.iOS;

    if (isIOS) {
      return FormField<T>(
        key: _fieldKey,
        initialValue: widget.value,
        validator: widget.validator,
        builder: (state) {
          final selectedLabel = state.value == null
              ? null
              : widget.items
                    .cast<PlatformSelectItem<T>?>()
                    .firstWhere(
                      (i) => i!.value == state.value,
                      orElse: () => null,
                    )
                    ?.label;

          return Tooltip(
            message: selectedLabel != null ? widget.label : '',
            triggerMode: TooltipTriggerMode.longPress,
            preferBelow: false,
            child: Semantics(
              identifier: widget.semanticsId ?? '',
              child: InkWell(
                onTap: () => _showPicker(context, state),
                borderRadius: BorderRadius.circular(4),
                hoverColor: Colors.transparent,
                highlightColor: Colors.transparent,
                splashColor: Colors.transparent,
                splashFactory: NoSplash.splashFactory,
                child: InputDecorator(
                  decoration: InputDecoration(
                    labelText: widget.label,
                    prefixIcon: widget.prefixIcon,
                    hintText: widget.hint,
                    errorText: state.errorText,
                    suffixIcon: const Icon(Icons.arrow_drop_down),
                  ),
                  isEmpty: selectedLabel == null,
                  child: selectedLabel != null
                      ? Text(selectedLabel)
                      : const SizedBox.shrink(),
                ),
              ),
            ),
          );
        },
      );
    }

    // Android: native DropdownButtonFormField. container: kendi node'u olmazsa
    // identifier üst node'a merge olur ve resource-id olarak görünmez (Maestro id:).
    return Semantics(
      container: widget.semanticsId != null,
      identifier: widget.semanticsId ?? '',
      child: _buildAndroidDropdown(),
    );
  }

  Widget _buildAndroidDropdown() {
    return DropdownButtonFormField<T>(
      initialValue: widget.value,
      decoration: InputDecoration(
        labelText: widget.label,
        prefixIcon: widget.prefixIcon,
      ),
      hint: widget.hint != null ? Text(widget.hint!) : null,
      items: widget.items
          .map(
            (item) =>
                DropdownMenuItem<T>(value: item.value, child: Text(item.label)),
          )
          .toList(),
      onChanged: widget.onChanged,
      validator: widget.validator,
    );
  }
}

class _SelectPickerSheet extends StatefulWidget {
  const _SelectPickerSheet({
    required this.initialIndex,
    required this.labels,
    required this.cancelLabel,
    required this.confirmLabel,
    required this.colorScheme,
    required this.onConfirm,
  });

  final int initialIndex;
  final List<String> labels;
  final String cancelLabel;
  final String confirmLabel;
  final ColorScheme colorScheme;
  final ValueChanged<int> onConfirm;

  @override
  State<_SelectPickerSheet> createState() => _SelectPickerSheetState();
}

class _SelectPickerSheetState extends State<_SelectPickerSheet> {
  late FixedExtentScrollController _controller;
  late int _selectedIdx;

  @override
  void initState() {
    super.initState();
    _selectedIdx = widget.initialIndex;
    _controller = FixedExtentScrollController(initialItem: _selectedIdx);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cs = widget.colorScheme;
    return Container(
      height: 300,
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
      ),
      child: Column(
        children: [
          _buildHandle(cs),
          _buildHeader(cs),
          Expanded(
            child: CupertinoPicker(
              scrollController: _controller,
              itemExtent: 44,
              backgroundColor: Colors.transparent,
              onSelectedItemChanged: (i) => _selectedIdx = i,
              children: widget.labels
                  .map(
                    (label) => Center(
                      child: Text(
                        label,
                        style: TextStyle(color: cs.onSurface, fontSize: 16),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHandle(ColorScheme cs) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Container(
        width: 40,
        height: 4,
        decoration: BoxDecoration(
          color: cs.onSurface.withValues(alpha: 0.2),
          borderRadius: BorderRadius.circular(2),
        ),
      ),
    );
  }

  Widget _buildHeader(ColorScheme cs) {
    return Container(
      height: 50,
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(color: cs.outlineVariant, width: 0.5),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          CupertinoButton(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            onPressed: () => Navigator.of(context).pop(),
            child: Text(
              widget.cancelLabel,
              style: TextStyle(color: cs.onSurface.withValues(alpha: 0.6)),
            ),
          ),
          CupertinoButton(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            onPressed: () {
              widget.onConfirm(_selectedIdx);
              Navigator.of(context).pop();
            },
            child: Semantics(
              identifier: 'pickerOkBtn',
              child: Text(
                widget.confirmLabel,
                style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
