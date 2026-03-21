// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'member_embedded_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$MemberEmbeddedModel {

 String get userId; AccessLevelJson get accessLevel;
/// Create a copy of MemberEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MemberEmbeddedModelCopyWith<MemberEmbeddedModel> get copyWith => _$MemberEmbeddedModelCopyWithImpl<MemberEmbeddedModel>(this as MemberEmbeddedModel, _$identity);

  /// Serializes this MemberEmbeddedModel to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MemberEmbeddedModel&&(identical(other.userId, userId) || other.userId == userId)&&(identical(other.accessLevel, accessLevel) || other.accessLevel == accessLevel));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,userId,accessLevel);

@override
String toString() {
  return 'MemberEmbeddedModel(userId: $userId, accessLevel: $accessLevel)';
}


}

/// @nodoc
abstract mixin class $MemberEmbeddedModelCopyWith<$Res>  {
  factory $MemberEmbeddedModelCopyWith(MemberEmbeddedModel value, $Res Function(MemberEmbeddedModel) _then) = _$MemberEmbeddedModelCopyWithImpl;
@useResult
$Res call({
 String userId, AccessLevelJson accessLevel
});




}
/// @nodoc
class _$MemberEmbeddedModelCopyWithImpl<$Res>
    implements $MemberEmbeddedModelCopyWith<$Res> {
  _$MemberEmbeddedModelCopyWithImpl(this._self, this._then);

  final MemberEmbeddedModel _self;
  final $Res Function(MemberEmbeddedModel) _then;

/// Create a copy of MemberEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? userId = null,Object? accessLevel = null,}) {
  return _then(_self.copyWith(
userId: null == userId ? _self.userId : userId // ignore: cast_nullable_to_non_nullable
as String,accessLevel: null == accessLevel ? _self.accessLevel : accessLevel // ignore: cast_nullable_to_non_nullable
as AccessLevelJson,
  ));
}

}


/// Adds pattern-matching-related methods to [MemberEmbeddedModel].
extension MemberEmbeddedModelPatterns on MemberEmbeddedModel {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MemberEmbeddedModel value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MemberEmbeddedModel() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MemberEmbeddedModel value)  $default,){
final _that = this;
switch (_that) {
case _MemberEmbeddedModel():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MemberEmbeddedModel value)?  $default,){
final _that = this;
switch (_that) {
case _MemberEmbeddedModel() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String userId,  AccessLevelJson accessLevel)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MemberEmbeddedModel() when $default != null:
return $default(_that.userId,_that.accessLevel);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String userId,  AccessLevelJson accessLevel)  $default,) {final _that = this;
switch (_that) {
case _MemberEmbeddedModel():
return $default(_that.userId,_that.accessLevel);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String userId,  AccessLevelJson accessLevel)?  $default,) {final _that = this;
switch (_that) {
case _MemberEmbeddedModel() when $default != null:
return $default(_that.userId,_that.accessLevel);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _MemberEmbeddedModel implements MemberEmbeddedModel {
  const _MemberEmbeddedModel({required this.userId, required this.accessLevel});
  factory _MemberEmbeddedModel.fromJson(Map<String, dynamic> json) => _$MemberEmbeddedModelFromJson(json);

@override final  String userId;
@override final  AccessLevelJson accessLevel;

/// Create a copy of MemberEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MemberEmbeddedModelCopyWith<_MemberEmbeddedModel> get copyWith => __$MemberEmbeddedModelCopyWithImpl<_MemberEmbeddedModel>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$MemberEmbeddedModelToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MemberEmbeddedModel&&(identical(other.userId, userId) || other.userId == userId)&&(identical(other.accessLevel, accessLevel) || other.accessLevel == accessLevel));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,userId,accessLevel);

@override
String toString() {
  return 'MemberEmbeddedModel(userId: $userId, accessLevel: $accessLevel)';
}


}

/// @nodoc
abstract mixin class _$MemberEmbeddedModelCopyWith<$Res> implements $MemberEmbeddedModelCopyWith<$Res> {
  factory _$MemberEmbeddedModelCopyWith(_MemberEmbeddedModel value, $Res Function(_MemberEmbeddedModel) _then) = __$MemberEmbeddedModelCopyWithImpl;
@override @useResult
$Res call({
 String userId, AccessLevelJson accessLevel
});




}
/// @nodoc
class __$MemberEmbeddedModelCopyWithImpl<$Res>
    implements _$MemberEmbeddedModelCopyWith<$Res> {
  __$MemberEmbeddedModelCopyWithImpl(this._self, this._then);

  final _MemberEmbeddedModel _self;
  final $Res Function(_MemberEmbeddedModel) _then;

/// Create a copy of MemberEmbeddedModel
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? userId = null,Object? accessLevel = null,}) {
  return _then(_MemberEmbeddedModel(
userId: null == userId ? _self.userId : userId // ignore: cast_nullable_to_non_nullable
as String,accessLevel: null == accessLevel ? _self.accessLevel : accessLevel // ignore: cast_nullable_to_non_nullable
as AccessLevelJson,
  ));
}


}

// dart format on
