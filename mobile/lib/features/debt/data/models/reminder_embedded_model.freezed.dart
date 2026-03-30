// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'reminder_embedded_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$ReminderEmbeddedModel {

 bool get enabled; List<String> get channels; String? get reminderDate; String? get reminderTime;
/// Create a copy of ReminderEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ReminderEmbeddedModelCopyWith<ReminderEmbeddedModel> get copyWith => _$ReminderEmbeddedModelCopyWithImpl<ReminderEmbeddedModel>(this as ReminderEmbeddedModel, _$identity);

  /// Serializes this ReminderEmbeddedModel to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ReminderEmbeddedModel&&(identical(other.enabled, enabled) || other.enabled == enabled)&&const DeepCollectionEquality().equals(other.channels, channels)&&(identical(other.reminderDate, reminderDate) || other.reminderDate == reminderDate)&&(identical(other.reminderTime, reminderTime) || other.reminderTime == reminderTime));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,enabled,const DeepCollectionEquality().hash(channels),reminderDate,reminderTime);

@override
String toString() {
  return 'ReminderEmbeddedModel(enabled: $enabled, channels: $channels, reminderDate: $reminderDate, reminderTime: $reminderTime)';
}


}

/// @nodoc
abstract mixin class $ReminderEmbeddedModelCopyWith<$Res>  {
  factory $ReminderEmbeddedModelCopyWith(ReminderEmbeddedModel value, $Res Function(ReminderEmbeddedModel) _then) = _$ReminderEmbeddedModelCopyWithImpl;
@useResult
$Res call({
 bool enabled, List<String> channels, String? reminderDate, String? reminderTime
});




}
/// @nodoc
class _$ReminderEmbeddedModelCopyWithImpl<$Res>
    implements $ReminderEmbeddedModelCopyWith<$Res> {
  _$ReminderEmbeddedModelCopyWithImpl(this._self, this._then);

  final ReminderEmbeddedModel _self;
  final $Res Function(ReminderEmbeddedModel) _then;

/// Create a copy of ReminderEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? enabled = null,Object? channels = null,Object? reminderDate = freezed,Object? reminderTime = freezed,}) {
  return _then(_self.copyWith(
enabled: null == enabled ? _self.enabled : enabled // ignore: cast_nullable_to_non_nullable
as bool,channels: null == channels ? _self.channels : channels // ignore: cast_nullable_to_non_nullable
as List<String>,reminderDate: freezed == reminderDate ? _self.reminderDate : reminderDate // ignore: cast_nullable_to_non_nullable
as String?,reminderTime: freezed == reminderTime ? _self.reminderTime : reminderTime // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [ReminderEmbeddedModel].
extension ReminderEmbeddedModelPatterns on ReminderEmbeddedModel {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ReminderEmbeddedModel value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ReminderEmbeddedModel() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ReminderEmbeddedModel value)  $default,){
final _that = this;
switch (_that) {
case _ReminderEmbeddedModel():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ReminderEmbeddedModel value)?  $default,){
final _that = this;
switch (_that) {
case _ReminderEmbeddedModel() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool enabled,  List<String> channels,  String? reminderDate,  String? reminderTime)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ReminderEmbeddedModel() when $default != null:
return $default(_that.enabled,_that.channels,_that.reminderDate,_that.reminderTime);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool enabled,  List<String> channels,  String? reminderDate,  String? reminderTime)  $default,) {final _that = this;
switch (_that) {
case _ReminderEmbeddedModel():
return $default(_that.enabled,_that.channels,_that.reminderDate,_that.reminderTime);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool enabled,  List<String> channels,  String? reminderDate,  String? reminderTime)?  $default,) {final _that = this;
switch (_that) {
case _ReminderEmbeddedModel() when $default != null:
return $default(_that.enabled,_that.channels,_that.reminderDate,_that.reminderTime);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _ReminderEmbeddedModel implements ReminderEmbeddedModel {
  const _ReminderEmbeddedModel({this.enabled = false, final  List<String> channels = const [], this.reminderDate, this.reminderTime}): _channels = channels;
  factory _ReminderEmbeddedModel.fromJson(Map<String, dynamic> json) => _$ReminderEmbeddedModelFromJson(json);

@override@JsonKey() final  bool enabled;
 final  List<String> _channels;
@override@JsonKey() List<String> get channels {
  if (_channels is EqualUnmodifiableListView) return _channels;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_channels);
}

@override final  String? reminderDate;
@override final  String? reminderTime;

/// Create a copy of ReminderEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ReminderEmbeddedModelCopyWith<_ReminderEmbeddedModel> get copyWith => __$ReminderEmbeddedModelCopyWithImpl<_ReminderEmbeddedModel>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$ReminderEmbeddedModelToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ReminderEmbeddedModel&&(identical(other.enabled, enabled) || other.enabled == enabled)&&const DeepCollectionEquality().equals(other._channels, _channels)&&(identical(other.reminderDate, reminderDate) || other.reminderDate == reminderDate)&&(identical(other.reminderTime, reminderTime) || other.reminderTime == reminderTime));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,enabled,const DeepCollectionEquality().hash(_channels),reminderDate,reminderTime);

@override
String toString() {
  return 'ReminderEmbeddedModel(enabled: $enabled, channels: $channels, reminderDate: $reminderDate, reminderTime: $reminderTime)';
}


}

/// @nodoc
abstract mixin class _$ReminderEmbeddedModelCopyWith<$Res> implements $ReminderEmbeddedModelCopyWith<$Res> {
  factory _$ReminderEmbeddedModelCopyWith(_ReminderEmbeddedModel value, $Res Function(_ReminderEmbeddedModel) _then) = __$ReminderEmbeddedModelCopyWithImpl;
@override @useResult
$Res call({
 bool enabled, List<String> channels, String? reminderDate, String? reminderTime
});




}
/// @nodoc
class __$ReminderEmbeddedModelCopyWithImpl<$Res>
    implements _$ReminderEmbeddedModelCopyWith<$Res> {
  __$ReminderEmbeddedModelCopyWithImpl(this._self, this._then);

  final _ReminderEmbeddedModel _self;
  final $Res Function(_ReminderEmbeddedModel) _then;

/// Create a copy of ReminderEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? enabled = null,Object? channels = null,Object? reminderDate = freezed,Object? reminderTime = freezed,}) {
  return _then(_ReminderEmbeddedModel(
enabled: null == enabled ? _self.enabled : enabled // ignore: cast_nullable_to_non_nullable
as bool,channels: null == channels ? _self._channels : channels // ignore: cast_nullable_to_non_nullable
as List<String>,reminderDate: freezed == reminderDate ? _self.reminderDate : reminderDate // ignore: cast_nullable_to_non_nullable
as String?,reminderTime: freezed == reminderTime ? _self.reminderTime : reminderTime // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

// dart format on
