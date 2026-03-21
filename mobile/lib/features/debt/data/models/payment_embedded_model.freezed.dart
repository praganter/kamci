// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'payment_embedded_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$PaymentEmbeddedModel {

 bool get paid; String? get paidTime;
/// Create a copy of PaymentEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$PaymentEmbeddedModelCopyWith<PaymentEmbeddedModel> get copyWith => _$PaymentEmbeddedModelCopyWithImpl<PaymentEmbeddedModel>(this as PaymentEmbeddedModel, _$identity);

  /// Serializes this PaymentEmbeddedModel to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is PaymentEmbeddedModel&&(identical(other.paid, paid) || other.paid == paid)&&(identical(other.paidTime, paidTime) || other.paidTime == paidTime));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,paid,paidTime);

@override
String toString() {
  return 'PaymentEmbeddedModel(paid: $paid, paidTime: $paidTime)';
}


}

/// @nodoc
abstract mixin class $PaymentEmbeddedModelCopyWith<$Res>  {
  factory $PaymentEmbeddedModelCopyWith(PaymentEmbeddedModel value, $Res Function(PaymentEmbeddedModel) _then) = _$PaymentEmbeddedModelCopyWithImpl;
@useResult
$Res call({
 bool paid, String? paidTime
});




}
/// @nodoc
class _$PaymentEmbeddedModelCopyWithImpl<$Res>
    implements $PaymentEmbeddedModelCopyWith<$Res> {
  _$PaymentEmbeddedModelCopyWithImpl(this._self, this._then);

  final PaymentEmbeddedModel _self;
  final $Res Function(PaymentEmbeddedModel) _then;

/// Create a copy of PaymentEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? paid = null,Object? paidTime = freezed,}) {
  return _then(_self.copyWith(
paid: null == paid ? _self.paid : paid // ignore: cast_nullable_to_non_nullable
as bool,paidTime: freezed == paidTime ? _self.paidTime : paidTime // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [PaymentEmbeddedModel].
extension PaymentEmbeddedModelPatterns on PaymentEmbeddedModel {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _PaymentEmbeddedModel value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _PaymentEmbeddedModel() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _PaymentEmbeddedModel value)  $default,){
final _that = this;
switch (_that) {
case _PaymentEmbeddedModel():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _PaymentEmbeddedModel value)?  $default,){
final _that = this;
switch (_that) {
case _PaymentEmbeddedModel() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool paid,  String? paidTime)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _PaymentEmbeddedModel() when $default != null:
return $default(_that.paid,_that.paidTime);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool paid,  String? paidTime)  $default,) {final _that = this;
switch (_that) {
case _PaymentEmbeddedModel():
return $default(_that.paid,_that.paidTime);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool paid,  String? paidTime)?  $default,) {final _that = this;
switch (_that) {
case _PaymentEmbeddedModel() when $default != null:
return $default(_that.paid,_that.paidTime);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _PaymentEmbeddedModel implements PaymentEmbeddedModel {
  const _PaymentEmbeddedModel({required this.paid, this.paidTime});
  factory _PaymentEmbeddedModel.fromJson(Map<String, dynamic> json) => _$PaymentEmbeddedModelFromJson(json);

@override final  bool paid;
@override final  String? paidTime;

/// Create a copy of PaymentEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$PaymentEmbeddedModelCopyWith<_PaymentEmbeddedModel> get copyWith => __$PaymentEmbeddedModelCopyWithImpl<_PaymentEmbeddedModel>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$PaymentEmbeddedModelToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _PaymentEmbeddedModel&&(identical(other.paid, paid) || other.paid == paid)&&(identical(other.paidTime, paidTime) || other.paidTime == paidTime));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,paid,paidTime);

@override
String toString() {
  return 'PaymentEmbeddedModel(paid: $paid, paidTime: $paidTime)';
}


}

/// @nodoc
abstract mixin class _$PaymentEmbeddedModelCopyWith<$Res> implements $PaymentEmbeddedModelCopyWith<$Res> {
  factory _$PaymentEmbeddedModelCopyWith(_PaymentEmbeddedModel value, $Res Function(_PaymentEmbeddedModel) _then) = __$PaymentEmbeddedModelCopyWithImpl;
@override @useResult
$Res call({
 bool paid, String? paidTime
});




}
/// @nodoc
class __$PaymentEmbeddedModelCopyWithImpl<$Res>
    implements _$PaymentEmbeddedModelCopyWith<$Res> {
  __$PaymentEmbeddedModelCopyWithImpl(this._self, this._then);

  final _PaymentEmbeddedModel _self;
  final $Res Function(_PaymentEmbeddedModel) _then;

/// Create a copy of PaymentEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? paid = null,Object? paidTime = freezed,}) {
  return _then(_PaymentEmbeddedModel(
paid: null == paid ? _self.paid : paid // ignore: cast_nullable_to_non_nullable
as bool,paidTime: freezed == paidTime ? _self.paidTime : paidTime // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

// dart format on
