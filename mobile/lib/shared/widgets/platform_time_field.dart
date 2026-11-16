import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class PlatformTimeField extends StatelessWidget {
  const PlatformTimeField({
    super.key,
    required this.label,
    this.prefixIcon,
    required this.value,
    required this.onChanged,
    this.hint,
  });

  final String label;
  final Widget? prefixIcon;
  final TimeOfDay? value;
  final ValueChanged<TimeOfDay?> onChanged;
  final String? hint;

  Future<void> _showIOSPicker(BuildContext context) async {
    final now = TimeOfDay.now();
    final initTime = value ?? now;
    final baseDate = DateTime.now();
    final initDateTime = DateTime(
      baseDate.year,
      baseDate.month,
      baseDate.day,
      initTime.hour,
      initTime.minute,
    );

    final mloc = MaterialLocalizations.of(context);
    final cs = Theme.of(context).colorScheme;
    final brightness = Theme.of(context).brightness;

    await showCupertinoModalPopup<void>(
      context: context,
      builder: (ctx) => _TimePickerSheet(
        initialDateTime: initDateTime,
        cancelLabel: mloc.cancelButtonLabel,
        confirmLabel: mloc.okButtonLabel,
        colorScheme: cs,
        brightness: brightness,
        onConfirm: (dt) {
          onChanged(TimeOfDay(hour: dt.hour, minute: dt.minute));
        },
      ),
    );
  }

  Future<void> _showMaterialPicker(BuildContext context) async {
    final picked = await showTimePicker(
      context: context,
      initialTime: value ?? TimeOfDay.now(),
    );
    if (picked != null) onChanged(picked);
  }

  @override
  Widget build(BuildContext context) {
    final isIOS = Theme.of(context).platform == TargetPlatform.iOS;
    return Tooltip(
      message: value != null ? label : '',
      triggerMode: TooltipTriggerMode.longPress,
      preferBelow: false,
      child: InkWell(
        onTap: () {
          FocusManager.instance.primaryFocus?.unfocus();
          isIOS ? _showIOSPicker(context) : _showMaterialPicker(context);
        },
        borderRadius: BorderRadius.circular(4),
        hoverColor: Colors.transparent,
        highlightColor: Colors.transparent,
        splashColor: Colors.transparent,
        splashFactory: NoSplash.splashFactory,
        child: InputDecorator(
          decoration: InputDecoration(
            labelText: label,
            prefixIcon: prefixIcon,
            hintText: hint,
          ),
          isEmpty: value == null,
          child: value != null
              ? Text(value!.format(context))
              : const SizedBox.shrink(),
        ),
      ),
    );
  }
}

class _TimePickerSheet extends StatefulWidget {
  const _TimePickerSheet({
    required this.initialDateTime,
    required this.cancelLabel,
    required this.confirmLabel,
    required this.colorScheme,
    required this.brightness,
    required this.onConfirm,
  });

  final DateTime initialDateTime;
  final String cancelLabel;
  final String confirmLabel;
  final ColorScheme colorScheme;
  final Brightness brightness;
  final ValueChanged<DateTime> onConfirm;

  @override
  State<_TimePickerSheet> createState() => _TimePickerSheetState();
}

class _TimePickerSheetState extends State<_TimePickerSheet> {
  late DateTime _selectedDateTime;

  @override
  void initState() {
    super.initState();
    _selectedDateTime = widget.initialDateTime;
  }

  @override
  Widget build(BuildContext context) {
    final cs = widget.colorScheme;
    return Container(
      height: 280,
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
                mode: CupertinoDatePickerMode.time,
                initialDateTime: widget.initialDateTime,
                use24hFormat: true,
                onDateTimeChanged: (dt) => _selectedDateTime = dt,
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
              widget.onConfirm(_selectedDateTime);
              Navigator.of(context).pop();
            },
            child: Text(
              widget.confirmLabel,
              style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
    );
  }
}
