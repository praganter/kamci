import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import '../../core/utils/date_formatter.dart';

class PlatformDateField extends StatefulWidget {
  const PlatformDateField({
    super.key,
    required this.label,
    this.prefixIcon,
    required this.value,
    required this.onChanged,
    this.validator,
    this.hint,
    this.firstDate,
    this.lastDate,
  });

  final String label;
  final Widget? prefixIcon;
  final DateTime? value;
  final ValueChanged<DateTime?> onChanged;
  final FormFieldValidator<DateTime?>? validator;
  final String? hint;
  final DateTime? firstDate;
  final DateTime? lastDate;

  @override
  State<PlatformDateField> createState() => _PlatformDateFieldState();
}

class _PlatformDateFieldState extends State<PlatformDateField> {
  final _fieldKey = GlobalKey<FormFieldState<DateTime?>>();

  @override
  void didUpdateWidget(PlatformDateField oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.value != widget.value) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _fieldKey.currentState?.didChange(widget.value);
      });
    }
  }

  DateTime _clampDate(DateTime? value, DateTime min, DateTime max) {
    final date = value ?? min;
    if (date.isBefore(min)) return min;
    if (date.isAfter(max)) return max;
    return date;
  }

  Future<void> _showIOSPicker(
    BuildContext context,
    FormFieldState<DateTime?> state,
  ) async {
    final firstDate = widget.firstDate ?? DateTime(2000);
    final lastDate = widget.lastDate ?? DateTime(2100);
    final initDate = _clampDate(widget.value ?? DateTime.now(), firstDate, lastDate);

    final mloc = MaterialLocalizations.of(context);
    final cs = Theme.of(context).colorScheme;
    final brightness = Theme.of(context).brightness;

    await showCupertinoModalPopup<void>(
      context: context,
      builder: (ctx) => _DatePickerSheet(
        initialDate: initDate,
        minimumDate: firstDate,
        maximumDate: lastDate,
        cancelLabel: mloc.cancelButtonLabel,
        confirmLabel: mloc.okButtonLabel,
        colorScheme: cs,
        brightness: brightness,
        onConfirm: (date) {
          state.didChange(date);
          widget.onChanged(date);
        },
      ),
    );
  }

  Future<void> _showMaterialPicker(
    BuildContext context,
    FormFieldState<DateTime?> state,
  ) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: widget.value ?? DateTime.now(),
      firstDate: widget.firstDate ?? DateTime(2000),
      lastDate: widget.lastDate ?? DateTime(2100),
    );
    if (picked != null) {
      state.didChange(picked);
      widget.onChanged(picked);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isIOS = Theme.of(context).platform == TargetPlatform.iOS;

    return FormField<DateTime?>(
      key: _fieldKey,
      initialValue: widget.value,
      validator: widget.validator,
      builder: (state) {
        final hasValue = state.value != null;
        return Tooltip(
          message: hasValue ? widget.label : '',
          triggerMode: TooltipTriggerMode.longPress,
          preferBelow: false,
          child: InkWell(
            onTap: () => isIOS
                ? _showIOSPicker(context, state)
                : _showMaterialPicker(context, state),
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
              ),
              isEmpty: state.value == null,
              child: state.value != null
                  ? Text(DateFormatter.formatDate(state.value!))
                  : const SizedBox.shrink(),
            ),
          ),
        );
      },
    );
  }
}

class _DatePickerSheet extends StatefulWidget {
  const _DatePickerSheet({
    required this.initialDate,
    required this.minimumDate,
    required this.maximumDate,
    required this.cancelLabel,
    required this.confirmLabel,
    required this.colorScheme,
    required this.brightness,
    required this.onConfirm,
  });

  final DateTime initialDate;
  final DateTime minimumDate;
  final DateTime maximumDate;
  final String cancelLabel;
  final String confirmLabel;
  final ColorScheme colorScheme;
  final Brightness brightness;
  final ValueChanged<DateTime> onConfirm;

  @override
  State<_DatePickerSheet> createState() => _DatePickerSheetState();
}

class _DatePickerSheetState extends State<_DatePickerSheet> {
  late DateTime _selectedDate;

  @override
  void initState() {
    super.initState();
    _selectedDate = widget.initialDate;
  }

  @override
  Widget build(BuildContext context) {
    final cs = widget.colorScheme;
    return Container(
      height: 320,
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
      ),
      child: Column(
        children: [
          _buildHandle(cs),
          _buildHeader(cs),
          Expanded(
            child: CupertinoTheme(
              data: CupertinoThemeData(
                brightness: widget.brightness,
                textTheme: CupertinoTextThemeData(
                  dateTimePickerTextStyle: TextStyle(
                    color: cs.onSurface,
                    fontSize: 16,
                  ),
                ),
              ),
              child: CupertinoDatePicker(
                mode: CupertinoDatePickerMode.date,
                initialDateTime: widget.initialDate,
                minimumDate: widget.minimumDate,
                maximumDate: widget.maximumDate,
                onDateTimeChanged: (date) => _selectedDate = date,
              ),
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
              widget.onConfirm(_selectedDate);
              Navigator.of(context).pop();
            },
            child: Text(
              widget.confirmLabel,
              style: TextStyle(
                color: cs.primary,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
