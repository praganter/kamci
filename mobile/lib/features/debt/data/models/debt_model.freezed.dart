// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'debt_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$DebtModel {

 String get id; String get name; String get ownerId; String? get categoryId; String? get description; String? get firstPaymentDate; int? get installmentCount; int? get paidInstallmentCount; PaymentPeriodTypeJson get paymentPeriodType;@JsonKey(fromJson: _amountFromJson) double get amount; DebtStatusJson get status; String? get currency; bool get autoPayment; bool get paidCurrentPeriod; List<PaymentEmbeddedModel> get payments; ReminderEmbeddedModel? get reminder; String? get nextReminderDate; String? get nextAutoPaymentDate;@JsonKey(name: 'createdDate') String? get createdAt;@JsonKey(name: 'updatedDate') String? get updatedAt;
/// Create a copy of DebtModel
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DebtModelCopyWith<DebtModel> get copyWith => _$DebtModelCopyWithImpl<DebtModel>(this as DebtModel, _$identity);

  /// Serializes this DebtModel to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DebtModel&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name)&&(identical(other.ownerId, ownerId) || other.ownerId == ownerId)&&(identical(other.categoryId, categoryId) || other.categoryId == categoryId)&&(identical(other.description, description) || other.description == description)&&(identical(other.firstPaymentDate, firstPaymentDate) || other.firstPaymentDate == firstPaymentDate)&&(identical(other.installmentCount, installmentCount) || other.installmentCount == installmentCount)&&(identical(other.paidInstallmentCount, paidInstallmentCount) || other.paidInstallmentCount == paidInstallmentCount)&&(identical(other.paymentPeriodType, paymentPeriodType) || other.paymentPeriodType == paymentPeriodType)&&(identical(other.amount, amount) || other.amount == amount)&&(identical(other.status, status) || other.status == status)&&(identical(other.currency, currency) || other.currency == currency)&&(identical(other.autoPayment, autoPayment) || other.autoPayment == autoPayment)&&(identical(other.paidCurrentPeriod, paidCurrentPeriod) || other.paidCurrentPeriod == paidCurrentPeriod)&&const DeepCollectionEquality().equals(other.payments, payments)&&(identical(other.reminder, reminder) || other.reminder == reminder)&&(identical(other.nextReminderDate, nextReminderDate) || other.nextReminderDate == nextReminderDate)&&(identical(other.nextAutoPaymentDate, nextAutoPaymentDate) || other.nextAutoPaymentDate == nextAutoPaymentDate)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hashAll([runtimeType,id,name,ownerId,categoryId,description,firstPaymentDate,installmentCount,paidInstallmentCount,paymentPeriodType,amount,status,currency,autoPayment,paidCurrentPeriod,const DeepCollectionEquality().hash(payments),reminder,nextReminderDate,nextAutoPaymentDate,createdAt,updatedAt]);

@override
String toString() {
  return 'DebtModel(id: $id, name: $name, ownerId: $ownerId, categoryId: $categoryId, description: $description, firstPaymentDate: $firstPaymentDate, installmentCount: $installmentCount, paidInstallmentCount: $paidInstallmentCount, paymentPeriodType: $paymentPeriodType, amount: $amount, status: $status, currency: $currency, autoPayment: $autoPayment, paidCurrentPeriod: $paidCurrentPeriod, payments: $payments, reminder: $reminder, nextReminderDate: $nextReminderDate, nextAutoPaymentDate: $nextAutoPaymentDate, createdAt: $createdAt, updatedAt: $updatedAt)';
}


}

/// @nodoc
abstract mixin class $DebtModelCopyWith<$Res>  {
  factory $DebtModelCopyWith(DebtModel value, $Res Function(DebtModel) _then) = _$DebtModelCopyWithImpl;
@useResult
$Res call({
 String id, String name, String ownerId, String? categoryId, String? description, String? firstPaymentDate, int? installmentCount, int? paidInstallmentCount, PaymentPeriodTypeJson paymentPeriodType,@JsonKey(fromJson: _amountFromJson) double amount, DebtStatusJson status, String? currency, bool autoPayment, bool paidCurrentPeriod, List<PaymentEmbeddedModel> payments, ReminderEmbeddedModel? reminder, String? nextReminderDate, String? nextAutoPaymentDate,@JsonKey(name: 'createdDate') String? createdAt,@JsonKey(name: 'updatedDate') String? updatedAt
});


$ReminderEmbeddedModelCopyWith<$Res>? get reminder;

}
/// @nodoc
class _$DebtModelCopyWithImpl<$Res>
    implements $DebtModelCopyWith<$Res> {
  _$DebtModelCopyWithImpl(this._self, this._then);

  final DebtModel _self;
  final $Res Function(DebtModel) _then;

/// Create a copy of DebtModel
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? name = null,Object? ownerId = null,Object? categoryId = freezed,Object? description = freezed,Object? firstPaymentDate = freezed,Object? installmentCount = freezed,Object? paidInstallmentCount = freezed,Object? paymentPeriodType = null,Object? amount = null,Object? status = null,Object? currency = freezed,Object? autoPayment = null,Object? paidCurrentPeriod = null,Object? payments = null,Object? reminder = freezed,Object? nextReminderDate = freezed,Object? nextAutoPaymentDate = freezed,Object? createdAt = freezed,Object? updatedAt = freezed,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,ownerId: null == ownerId ? _self.ownerId : ownerId // ignore: cast_nullable_to_non_nullable
as String,categoryId: freezed == categoryId ? _self.categoryId : categoryId // ignore: cast_nullable_to_non_nullable
as String?,description: freezed == description ? _self.description : description // ignore: cast_nullable_to_non_nullable
as String?,firstPaymentDate: freezed == firstPaymentDate ? _self.firstPaymentDate : firstPaymentDate // ignore: cast_nullable_to_non_nullable
as String?,installmentCount: freezed == installmentCount ? _self.installmentCount : installmentCount // ignore: cast_nullable_to_non_nullable
as int?,paidInstallmentCount: freezed == paidInstallmentCount ? _self.paidInstallmentCount : paidInstallmentCount // ignore: cast_nullable_to_non_nullable
as int?,paymentPeriodType: null == paymentPeriodType ? _self.paymentPeriodType : paymentPeriodType // ignore: cast_nullable_to_non_nullable
as PaymentPeriodTypeJson,amount: null == amount ? _self.amount : amount // ignore: cast_nullable_to_non_nullable
as double,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as DebtStatusJson,currency: freezed == currency ? _self.currency : currency // ignore: cast_nullable_to_non_nullable
as String?,autoPayment: null == autoPayment ? _self.autoPayment : autoPayment // ignore: cast_nullable_to_non_nullable
as bool,paidCurrentPeriod: null == paidCurrentPeriod ? _self.paidCurrentPeriod : paidCurrentPeriod // ignore: cast_nullable_to_non_nullable
as bool,payments: null == payments ? _self.payments : payments // ignore: cast_nullable_to_non_nullable
as List<PaymentEmbeddedModel>,reminder: freezed == reminder ? _self.reminder : reminder // ignore: cast_nullable_to_non_nullable
as ReminderEmbeddedModel?,nextReminderDate: freezed == nextReminderDate ? _self.nextReminderDate : nextReminderDate // ignore: cast_nullable_to_non_nullable
as String?,nextAutoPaymentDate: freezed == nextAutoPaymentDate ? _self.nextAutoPaymentDate : nextAutoPaymentDate // ignore: cast_nullable_to_non_nullable
as String?,createdAt: freezed == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String?,updatedAt: freezed == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}
/// Create a copy of DebtModel
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ReminderEmbeddedModelCopyWith<$Res>? get reminder {
    if (_self.reminder == null) {
    return null;
  }

  return $ReminderEmbeddedModelCopyWith<$Res>(_self.reminder!, (value) {
    return _then(_self.copyWith(reminder: value));
  });
}
}


/// Adds pattern-matching-related methods to [DebtModel].
extension DebtModelPatterns on DebtModel {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DebtModel value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DebtModel() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DebtModel value)  $default,){
final _that = this;
switch (_that) {
case _DebtModel():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DebtModel value)?  $default,){
final _that = this;
switch (_that) {
case _DebtModel() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String name,  String ownerId,  String? categoryId,  String? description,  String? firstPaymentDate,  int? installmentCount,  int? paidInstallmentCount,  PaymentPeriodTypeJson paymentPeriodType, @JsonKey(fromJson: _amountFromJson)  double amount,  DebtStatusJson status,  String? currency,  bool autoPayment,  bool paidCurrentPeriod,  List<PaymentEmbeddedModel> payments,  ReminderEmbeddedModel? reminder,  String? nextReminderDate,  String? nextAutoPaymentDate, @JsonKey(name: 'createdDate')  String? createdAt, @JsonKey(name: 'updatedDate')  String? updatedAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DebtModel() when $default != null:
return $default(_that.id,_that.name,_that.ownerId,_that.categoryId,_that.description,_that.firstPaymentDate,_that.installmentCount,_that.paidInstallmentCount,_that.paymentPeriodType,_that.amount,_that.status,_that.currency,_that.autoPayment,_that.paidCurrentPeriod,_that.payments,_that.reminder,_that.nextReminderDate,_that.nextAutoPaymentDate,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String name,  String ownerId,  String? categoryId,  String? description,  String? firstPaymentDate,  int? installmentCount,  int? paidInstallmentCount,  PaymentPeriodTypeJson paymentPeriodType, @JsonKey(fromJson: _amountFromJson)  double amount,  DebtStatusJson status,  String? currency,  bool autoPayment,  bool paidCurrentPeriod,  List<PaymentEmbeddedModel> payments,  ReminderEmbeddedModel? reminder,  String? nextReminderDate,  String? nextAutoPaymentDate, @JsonKey(name: 'createdDate')  String? createdAt, @JsonKey(name: 'updatedDate')  String? updatedAt)  $default,) {final _that = this;
switch (_that) {
case _DebtModel():
return $default(_that.id,_that.name,_that.ownerId,_that.categoryId,_that.description,_that.firstPaymentDate,_that.installmentCount,_that.paidInstallmentCount,_that.paymentPeriodType,_that.amount,_that.status,_that.currency,_that.autoPayment,_that.paidCurrentPeriod,_that.payments,_that.reminder,_that.nextReminderDate,_that.nextAutoPaymentDate,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String name,  String ownerId,  String? categoryId,  String? description,  String? firstPaymentDate,  int? installmentCount,  int? paidInstallmentCount,  PaymentPeriodTypeJson paymentPeriodType, @JsonKey(fromJson: _amountFromJson)  double amount,  DebtStatusJson status,  String? currency,  bool autoPayment,  bool paidCurrentPeriod,  List<PaymentEmbeddedModel> payments,  ReminderEmbeddedModel? reminder,  String? nextReminderDate,  String? nextAutoPaymentDate, @JsonKey(name: 'createdDate')  String? createdAt, @JsonKey(name: 'updatedDate')  String? updatedAt)?  $default,) {final _that = this;
switch (_that) {
case _DebtModel() when $default != null:
return $default(_that.id,_that.name,_that.ownerId,_that.categoryId,_that.description,_that.firstPaymentDate,_that.installmentCount,_that.paidInstallmentCount,_that.paymentPeriodType,_that.amount,_that.status,_that.currency,_that.autoPayment,_that.paidCurrentPeriod,_that.payments,_that.reminder,_that.nextReminderDate,_that.nextAutoPaymentDate,_that.createdAt,_that.updatedAt);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _DebtModel implements DebtModel {
  const _DebtModel({required this.id, required this.name, required this.ownerId, this.categoryId, this.description, this.firstPaymentDate, this.installmentCount, this.paidInstallmentCount, required this.paymentPeriodType, @JsonKey(fromJson: _amountFromJson) required this.amount, this.status = DebtStatusJson.active, this.currency, this.autoPayment = false, this.paidCurrentPeriod = false, final  List<PaymentEmbeddedModel> payments = const [], this.reminder, this.nextReminderDate, this.nextAutoPaymentDate, @JsonKey(name: 'createdDate') this.createdAt, @JsonKey(name: 'updatedDate') this.updatedAt}): _payments = payments;
  factory _DebtModel.fromJson(Map<String, dynamic> json) => _$DebtModelFromJson(json);

@override final  String id;
@override final  String name;
@override final  String ownerId;
@override final  String? categoryId;
@override final  String? description;
@override final  String? firstPaymentDate;
@override final  int? installmentCount;
@override final  int? paidInstallmentCount;
@override final  PaymentPeriodTypeJson paymentPeriodType;
@override@JsonKey(fromJson: _amountFromJson) final  double amount;
@override@JsonKey() final  DebtStatusJson status;
@override final  String? currency;
@override@JsonKey() final  bool autoPayment;
@override@JsonKey() final  bool paidCurrentPeriod;
 final  List<PaymentEmbeddedModel> _payments;
@override@JsonKey() List<PaymentEmbeddedModel> get payments {
  if (_payments is EqualUnmodifiableListView) return _payments;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_payments);
}

@override final  ReminderEmbeddedModel? reminder;
@override final  String? nextReminderDate;
@override final  String? nextAutoPaymentDate;
@override@JsonKey(name: 'createdDate') final  String? createdAt;
@override@JsonKey(name: 'updatedDate') final  String? updatedAt;

/// Create a copy of DebtModel
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DebtModelCopyWith<_DebtModel> get copyWith => __$DebtModelCopyWithImpl<_DebtModel>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$DebtModelToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DebtModel&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name)&&(identical(other.ownerId, ownerId) || other.ownerId == ownerId)&&(identical(other.categoryId, categoryId) || other.categoryId == categoryId)&&(identical(other.description, description) || other.description == description)&&(identical(other.firstPaymentDate, firstPaymentDate) || other.firstPaymentDate == firstPaymentDate)&&(identical(other.installmentCount, installmentCount) || other.installmentCount == installmentCount)&&(identical(other.paidInstallmentCount, paidInstallmentCount) || other.paidInstallmentCount == paidInstallmentCount)&&(identical(other.paymentPeriodType, paymentPeriodType) || other.paymentPeriodType == paymentPeriodType)&&(identical(other.amount, amount) || other.amount == amount)&&(identical(other.status, status) || other.status == status)&&(identical(other.currency, currency) || other.currency == currency)&&(identical(other.autoPayment, autoPayment) || other.autoPayment == autoPayment)&&(identical(other.paidCurrentPeriod, paidCurrentPeriod) || other.paidCurrentPeriod == paidCurrentPeriod)&&const DeepCollectionEquality().equals(other._payments, _payments)&&(identical(other.reminder, reminder) || other.reminder == reminder)&&(identical(other.nextReminderDate, nextReminderDate) || other.nextReminderDate == nextReminderDate)&&(identical(other.nextAutoPaymentDate, nextAutoPaymentDate) || other.nextAutoPaymentDate == nextAutoPaymentDate)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hashAll([runtimeType,id,name,ownerId,categoryId,description,firstPaymentDate,installmentCount,paidInstallmentCount,paymentPeriodType,amount,status,currency,autoPayment,paidCurrentPeriod,const DeepCollectionEquality().hash(_payments),reminder,nextReminderDate,nextAutoPaymentDate,createdAt,updatedAt]);

@override
String toString() {
  return 'DebtModel(id: $id, name: $name, ownerId: $ownerId, categoryId: $categoryId, description: $description, firstPaymentDate: $firstPaymentDate, installmentCount: $installmentCount, paidInstallmentCount: $paidInstallmentCount, paymentPeriodType: $paymentPeriodType, amount: $amount, status: $status, currency: $currency, autoPayment: $autoPayment, paidCurrentPeriod: $paidCurrentPeriod, payments: $payments, reminder: $reminder, nextReminderDate: $nextReminderDate, nextAutoPaymentDate: $nextAutoPaymentDate, createdAt: $createdAt, updatedAt: $updatedAt)';
}


}

/// @nodoc
abstract mixin class _$DebtModelCopyWith<$Res> implements $DebtModelCopyWith<$Res> {
  factory _$DebtModelCopyWith(_DebtModel value, $Res Function(_DebtModel) _then) = __$DebtModelCopyWithImpl;
@override @useResult
$Res call({
 String id, String name, String ownerId, String? categoryId, String? description, String? firstPaymentDate, int? installmentCount, int? paidInstallmentCount, PaymentPeriodTypeJson paymentPeriodType,@JsonKey(fromJson: _amountFromJson) double amount, DebtStatusJson status, String? currency, bool autoPayment, bool paidCurrentPeriod, List<PaymentEmbeddedModel> payments, ReminderEmbeddedModel? reminder, String? nextReminderDate, String? nextAutoPaymentDate,@JsonKey(name: 'createdDate') String? createdAt,@JsonKey(name: 'updatedDate') String? updatedAt
});


@override $ReminderEmbeddedModelCopyWith<$Res>? get reminder;

}
/// @nodoc
class __$DebtModelCopyWithImpl<$Res>
    implements _$DebtModelCopyWith<$Res> {
  __$DebtModelCopyWithImpl(this._self, this._then);

  final _DebtModel _self;
  final $Res Function(_DebtModel) _then;

/// Create a copy of DebtModel
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? name = null,Object? ownerId = null,Object? categoryId = freezed,Object? description = freezed,Object? firstPaymentDate = freezed,Object? installmentCount = freezed,Object? paidInstallmentCount = freezed,Object? paymentPeriodType = null,Object? amount = null,Object? status = null,Object? currency = freezed,Object? autoPayment = null,Object? paidCurrentPeriod = null,Object? payments = null,Object? reminder = freezed,Object? nextReminderDate = freezed,Object? nextAutoPaymentDate = freezed,Object? createdAt = freezed,Object? updatedAt = freezed,}) {
  return _then(_DebtModel(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,ownerId: null == ownerId ? _self.ownerId : ownerId // ignore: cast_nullable_to_non_nullable
as String,categoryId: freezed == categoryId ? _self.categoryId : categoryId // ignore: cast_nullable_to_non_nullable
as String?,description: freezed == description ? _self.description : description // ignore: cast_nullable_to_non_nullable
as String?,firstPaymentDate: freezed == firstPaymentDate ? _self.firstPaymentDate : firstPaymentDate // ignore: cast_nullable_to_non_nullable
as String?,installmentCount: freezed == installmentCount ? _self.installmentCount : installmentCount // ignore: cast_nullable_to_non_nullable
as int?,paidInstallmentCount: freezed == paidInstallmentCount ? _self.paidInstallmentCount : paidInstallmentCount // ignore: cast_nullable_to_non_nullable
as int?,paymentPeriodType: null == paymentPeriodType ? _self.paymentPeriodType : paymentPeriodType // ignore: cast_nullable_to_non_nullable
as PaymentPeriodTypeJson,amount: null == amount ? _self.amount : amount // ignore: cast_nullable_to_non_nullable
as double,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as DebtStatusJson,currency: freezed == currency ? _self.currency : currency // ignore: cast_nullable_to_non_nullable
as String?,autoPayment: null == autoPayment ? _self.autoPayment : autoPayment // ignore: cast_nullable_to_non_nullable
as bool,paidCurrentPeriod: null == paidCurrentPeriod ? _self.paidCurrentPeriod : paidCurrentPeriod // ignore: cast_nullable_to_non_nullable
as bool,payments: null == payments ? _self._payments : payments // ignore: cast_nullable_to_non_nullable
as List<PaymentEmbeddedModel>,reminder: freezed == reminder ? _self.reminder : reminder // ignore: cast_nullable_to_non_nullable
as ReminderEmbeddedModel?,nextReminderDate: freezed == nextReminderDate ? _self.nextReminderDate : nextReminderDate // ignore: cast_nullable_to_non_nullable
as String?,nextAutoPaymentDate: freezed == nextAutoPaymentDate ? _self.nextAutoPaymentDate : nextAutoPaymentDate // ignore: cast_nullable_to_non_nullable
as String?,createdAt: freezed == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String?,updatedAt: freezed == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

/// Create a copy of DebtModel
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ReminderEmbeddedModelCopyWith<$Res>? get reminder {
    if (_self.reminder == null) {
    return null;
  }

  return $ReminderEmbeddedModelCopyWith<$Res>(_self.reminder!, (value) {
    return _then(_self.copyWith(reminder: value));
  });
}
}

// dart format on
