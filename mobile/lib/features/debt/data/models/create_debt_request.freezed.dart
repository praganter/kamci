// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'create_debt_request.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$CreateDebtRequest {

 String? get id; String get name; String? get categoryId; String? get description; String get startDate; String? get endDate; PaymentPeriodTypeJson get paymentPeriodType; double get amount; DebtStatusJson get status;
/// Create a copy of CreateDebtRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CreateDebtRequestCopyWith<CreateDebtRequest> get copyWith => _$CreateDebtRequestCopyWithImpl<CreateDebtRequest>(this as CreateDebtRequest, _$identity);

  /// Serializes this CreateDebtRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CreateDebtRequest&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name)&&(identical(other.categoryId, categoryId) || other.categoryId == categoryId)&&(identical(other.description, description) || other.description == description)&&(identical(other.startDate, startDate) || other.startDate == startDate)&&(identical(other.endDate, endDate) || other.endDate == endDate)&&(identical(other.paymentPeriodType, paymentPeriodType) || other.paymentPeriodType == paymentPeriodType)&&(identical(other.amount, amount) || other.amount == amount)&&(identical(other.status, status) || other.status == status));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,id,name,categoryId,description,startDate,endDate,paymentPeriodType,amount,status);

@override
String toString() {
  return 'CreateDebtRequest(id: $id, name: $name, categoryId: $categoryId, description: $description, startDate: $startDate, endDate: $endDate, paymentPeriodType: $paymentPeriodType, amount: $amount, status: $status)';
}


}

/// @nodoc
abstract mixin class $CreateDebtRequestCopyWith<$Res>  {
  factory $CreateDebtRequestCopyWith(CreateDebtRequest value, $Res Function(CreateDebtRequest) _then) = _$CreateDebtRequestCopyWithImpl;
@useResult
$Res call({
 String? id, String name, String? categoryId, String? description, String startDate, String? endDate, PaymentPeriodTypeJson paymentPeriodType, double amount, DebtStatusJson status
});




}
/// @nodoc
class _$CreateDebtRequestCopyWithImpl<$Res>
    implements $CreateDebtRequestCopyWith<$Res> {
  _$CreateDebtRequestCopyWithImpl(this._self, this._then);

  final CreateDebtRequest _self;
  final $Res Function(CreateDebtRequest) _then;

/// Create a copy of CreateDebtRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = freezed,Object? name = null,Object? categoryId = freezed,Object? description = freezed,Object? startDate = null,Object? endDate = freezed,Object? paymentPeriodType = null,Object? amount = null,Object? status = null,}) {
  return _then(_self.copyWith(
id: freezed == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String?,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,categoryId: freezed == categoryId ? _self.categoryId : categoryId // ignore: cast_nullable_to_non_nullable
as String?,description: freezed == description ? _self.description : description // ignore: cast_nullable_to_non_nullable
as String?,startDate: null == startDate ? _self.startDate : startDate // ignore: cast_nullable_to_non_nullable
as String,endDate: freezed == endDate ? _self.endDate : endDate // ignore: cast_nullable_to_non_nullable
as String?,paymentPeriodType: null == paymentPeriodType ? _self.paymentPeriodType : paymentPeriodType // ignore: cast_nullable_to_non_nullable
as PaymentPeriodTypeJson,amount: null == amount ? _self.amount : amount // ignore: cast_nullable_to_non_nullable
as double,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as DebtStatusJson,
  ));
}

}


/// Adds pattern-matching-related methods to [CreateDebtRequest].
extension CreateDebtRequestPatterns on CreateDebtRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CreateDebtRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CreateDebtRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CreateDebtRequest value)  $default,){
final _that = this;
switch (_that) {
case _CreateDebtRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CreateDebtRequest value)?  $default,){
final _that = this;
switch (_that) {
case _CreateDebtRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String? id,  String name,  String? categoryId,  String? description,  String startDate,  String? endDate,  PaymentPeriodTypeJson paymentPeriodType,  double amount,  DebtStatusJson status)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CreateDebtRequest() when $default != null:
return $default(_that.id,_that.name,_that.categoryId,_that.description,_that.startDate,_that.endDate,_that.paymentPeriodType,_that.amount,_that.status);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String? id,  String name,  String? categoryId,  String? description,  String startDate,  String? endDate,  PaymentPeriodTypeJson paymentPeriodType,  double amount,  DebtStatusJson status)  $default,) {final _that = this;
switch (_that) {
case _CreateDebtRequest():
return $default(_that.id,_that.name,_that.categoryId,_that.description,_that.startDate,_that.endDate,_that.paymentPeriodType,_that.amount,_that.status);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String? id,  String name,  String? categoryId,  String? description,  String startDate,  String? endDate,  PaymentPeriodTypeJson paymentPeriodType,  double amount,  DebtStatusJson status)?  $default,) {final _that = this;
switch (_that) {
case _CreateDebtRequest() when $default != null:
return $default(_that.id,_that.name,_that.categoryId,_that.description,_that.startDate,_that.endDate,_that.paymentPeriodType,_that.amount,_that.status);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _CreateDebtRequest implements CreateDebtRequest {
  const _CreateDebtRequest({this.id, required this.name, this.categoryId, this.description, required this.startDate, this.endDate, required this.paymentPeriodType, required this.amount, this.status = DebtStatusJson.active});
  factory _CreateDebtRequest.fromJson(Map<String, dynamic> json) => _$CreateDebtRequestFromJson(json);

@override final  String? id;
@override final  String name;
@override final  String? categoryId;
@override final  String? description;
@override final  String startDate;
@override final  String? endDate;
@override final  PaymentPeriodTypeJson paymentPeriodType;
@override final  double amount;
@override@JsonKey() final  DebtStatusJson status;

/// Create a copy of CreateDebtRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CreateDebtRequestCopyWith<_CreateDebtRequest> get copyWith => __$CreateDebtRequestCopyWithImpl<_CreateDebtRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$CreateDebtRequestToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CreateDebtRequest&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name)&&(identical(other.categoryId, categoryId) || other.categoryId == categoryId)&&(identical(other.description, description) || other.description == description)&&(identical(other.startDate, startDate) || other.startDate == startDate)&&(identical(other.endDate, endDate) || other.endDate == endDate)&&(identical(other.paymentPeriodType, paymentPeriodType) || other.paymentPeriodType == paymentPeriodType)&&(identical(other.amount, amount) || other.amount == amount)&&(identical(other.status, status) || other.status == status));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,id,name,categoryId,description,startDate,endDate,paymentPeriodType,amount,status);

@override
String toString() {
  return 'CreateDebtRequest(id: $id, name: $name, categoryId: $categoryId, description: $description, startDate: $startDate, endDate: $endDate, paymentPeriodType: $paymentPeriodType, amount: $amount, status: $status)';
}


}

/// @nodoc
abstract mixin class _$CreateDebtRequestCopyWith<$Res> implements $CreateDebtRequestCopyWith<$Res> {
  factory _$CreateDebtRequestCopyWith(_CreateDebtRequest value, $Res Function(_CreateDebtRequest) _then) = __$CreateDebtRequestCopyWithImpl;
@override @useResult
$Res call({
 String? id, String name, String? categoryId, String? description, String startDate, String? endDate, PaymentPeriodTypeJson paymentPeriodType, double amount, DebtStatusJson status
});




}
/// @nodoc
class __$CreateDebtRequestCopyWithImpl<$Res>
    implements _$CreateDebtRequestCopyWith<$Res> {
  __$CreateDebtRequestCopyWithImpl(this._self, this._then);

  final _CreateDebtRequest _self;
  final $Res Function(_CreateDebtRequest) _then;

/// Create a copy of CreateDebtRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = freezed,Object? name = null,Object? categoryId = freezed,Object? description = freezed,Object? startDate = null,Object? endDate = freezed,Object? paymentPeriodType = null,Object? amount = null,Object? status = null,}) {
  return _then(_CreateDebtRequest(
id: freezed == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String?,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,categoryId: freezed == categoryId ? _self.categoryId : categoryId // ignore: cast_nullable_to_non_nullable
as String?,description: freezed == description ? _self.description : description // ignore: cast_nullable_to_non_nullable
as String?,startDate: null == startDate ? _self.startDate : startDate // ignore: cast_nullable_to_non_nullable
as String,endDate: freezed == endDate ? _self.endDate : endDate // ignore: cast_nullable_to_non_nullable
as String?,paymentPeriodType: null == paymentPeriodType ? _self.paymentPeriodType : paymentPeriodType // ignore: cast_nullable_to_non_nullable
as PaymentPeriodTypeJson,amount: null == amount ? _self.amount : amount // ignore: cast_nullable_to_non_nullable
as double,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as DebtStatusJson,
  ));
}


}

// dart format on
