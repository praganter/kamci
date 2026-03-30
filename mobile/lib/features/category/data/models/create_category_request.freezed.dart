// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'create_category_request.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$CreateCategoryRequest {

 String get id; String get name;
/// Create a copy of CreateCategoryRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CreateCategoryRequestCopyWith<CreateCategoryRequest> get copyWith => _$CreateCategoryRequestCopyWithImpl<CreateCategoryRequest>(this as CreateCategoryRequest, _$identity);

  /// Serializes this CreateCategoryRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CreateCategoryRequest&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,id,name);

@override
String toString() {
  return 'CreateCategoryRequest(id: $id, name: $name)';
}


}

/// @nodoc
abstract mixin class $CreateCategoryRequestCopyWith<$Res>  {
  factory $CreateCategoryRequestCopyWith(CreateCategoryRequest value, $Res Function(CreateCategoryRequest) _then) = _$CreateCategoryRequestCopyWithImpl;
@useResult
$Res call({
 String id, String name
});




}
/// @nodoc
class _$CreateCategoryRequestCopyWithImpl<$Res>
    implements $CreateCategoryRequestCopyWith<$Res> {
  _$CreateCategoryRequestCopyWithImpl(this._self, this._then);

  final CreateCategoryRequest _self;
  final $Res Function(CreateCategoryRequest) _then;

/// Create a copy of CreateCategoryRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? name = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [CreateCategoryRequest].
extension CreateCategoryRequestPatterns on CreateCategoryRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CreateCategoryRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CreateCategoryRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CreateCategoryRequest value)  $default,){
final _that = this;
switch (_that) {
case _CreateCategoryRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CreateCategoryRequest value)?  $default,){
final _that = this;
switch (_that) {
case _CreateCategoryRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String name)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CreateCategoryRequest() when $default != null:
return $default(_that.id,_that.name);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String name)  $default,) {final _that = this;
switch (_that) {
case _CreateCategoryRequest():
return $default(_that.id,_that.name);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String name)?  $default,) {final _that = this;
switch (_that) {
case _CreateCategoryRequest() when $default != null:
return $default(_that.id,_that.name);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _CreateCategoryRequest implements CreateCategoryRequest {
  const _CreateCategoryRequest({required this.id, required this.name});
  factory _CreateCategoryRequest.fromJson(Map<String, dynamic> json) => _$CreateCategoryRequestFromJson(json);

@override final  String id;
@override final  String name;

/// Create a copy of CreateCategoryRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CreateCategoryRequestCopyWith<_CreateCategoryRequest> get copyWith => __$CreateCategoryRequestCopyWithImpl<_CreateCategoryRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$CreateCategoryRequestToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CreateCategoryRequest&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,id,name);

@override
String toString() {
  return 'CreateCategoryRequest(id: $id, name: $name)';
}


}

/// @nodoc
abstract mixin class _$CreateCategoryRequestCopyWith<$Res> implements $CreateCategoryRequestCopyWith<$Res> {
  factory _$CreateCategoryRequestCopyWith(_CreateCategoryRequest value, $Res Function(_CreateCategoryRequest) _then) = __$CreateCategoryRequestCopyWithImpl;
@override @useResult
$Res call({
 String id, String name
});




}
/// @nodoc
class __$CreateCategoryRequestCopyWithImpl<$Res>
    implements _$CreateCategoryRequestCopyWith<$Res> {
  __$CreateCategoryRequestCopyWithImpl(this._self, this._then);

  final _CreateCategoryRequest _self;
  final $Res Function(_CreateCategoryRequest) _then;

/// Create a copy of CreateCategoryRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? name = null,}) {
  return _then(_CreateCategoryRequest(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$AddMemberRequest {

 String get email; AccessLevelJson get accessLevel;
/// Create a copy of AddMemberRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$AddMemberRequestCopyWith<AddMemberRequest> get copyWith => _$AddMemberRequestCopyWithImpl<AddMemberRequest>(this as AddMemberRequest, _$identity);

  /// Serializes this AddMemberRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is AddMemberRequest&&(identical(other.email, email) || other.email == email)&&(identical(other.accessLevel, accessLevel) || other.accessLevel == accessLevel));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,email,accessLevel);

@override
String toString() {
  return 'AddMemberRequest(email: $email, accessLevel: $accessLevel)';
}


}

/// @nodoc
abstract mixin class $AddMemberRequestCopyWith<$Res>  {
  factory $AddMemberRequestCopyWith(AddMemberRequest value, $Res Function(AddMemberRequest) _then) = _$AddMemberRequestCopyWithImpl;
@useResult
$Res call({
 String email, AccessLevelJson accessLevel
});




}
/// @nodoc
class _$AddMemberRequestCopyWithImpl<$Res>
    implements $AddMemberRequestCopyWith<$Res> {
  _$AddMemberRequestCopyWithImpl(this._self, this._then);

  final AddMemberRequest _self;
  final $Res Function(AddMemberRequest) _then;

/// Create a copy of AddMemberRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? email = null,Object? accessLevel = null,}) {
  return _then(_self.copyWith(
email: null == email ? _self.email : email // ignore: cast_nullable_to_non_nullable
as String,accessLevel: null == accessLevel ? _self.accessLevel : accessLevel // ignore: cast_nullable_to_non_nullable
as AccessLevelJson,
  ));
}

}


/// Adds pattern-matching-related methods to [AddMemberRequest].
extension AddMemberRequestPatterns on AddMemberRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _AddMemberRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _AddMemberRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _AddMemberRequest value)  $default,){
final _that = this;
switch (_that) {
case _AddMemberRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _AddMemberRequest value)?  $default,){
final _that = this;
switch (_that) {
case _AddMemberRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String email,  AccessLevelJson accessLevel)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _AddMemberRequest() when $default != null:
return $default(_that.email,_that.accessLevel);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String email,  AccessLevelJson accessLevel)  $default,) {final _that = this;
switch (_that) {
case _AddMemberRequest():
return $default(_that.email,_that.accessLevel);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String email,  AccessLevelJson accessLevel)?  $default,) {final _that = this;
switch (_that) {
case _AddMemberRequest() when $default != null:
return $default(_that.email,_that.accessLevel);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _AddMemberRequest implements AddMemberRequest {
  const _AddMemberRequest({required this.email, required this.accessLevel});
  factory _AddMemberRequest.fromJson(Map<String, dynamic> json) => _$AddMemberRequestFromJson(json);

@override final  String email;
@override final  AccessLevelJson accessLevel;

/// Create a copy of AddMemberRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$AddMemberRequestCopyWith<_AddMemberRequest> get copyWith => __$AddMemberRequestCopyWithImpl<_AddMemberRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$AddMemberRequestToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _AddMemberRequest&&(identical(other.email, email) || other.email == email)&&(identical(other.accessLevel, accessLevel) || other.accessLevel == accessLevel));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,email,accessLevel);

@override
String toString() {
  return 'AddMemberRequest(email: $email, accessLevel: $accessLevel)';
}


}

/// @nodoc
abstract mixin class _$AddMemberRequestCopyWith<$Res> implements $AddMemberRequestCopyWith<$Res> {
  factory _$AddMemberRequestCopyWith(_AddMemberRequest value, $Res Function(_AddMemberRequest) _then) = __$AddMemberRequestCopyWithImpl;
@override @useResult
$Res call({
 String email, AccessLevelJson accessLevel
});




}
/// @nodoc
class __$AddMemberRequestCopyWithImpl<$Res>
    implements _$AddMemberRequestCopyWith<$Res> {
  __$AddMemberRequestCopyWithImpl(this._self, this._then);

  final _AddMemberRequest _self;
  final $Res Function(_AddMemberRequest) _then;

/// Create a copy of AddMemberRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? email = null,Object? accessLevel = null,}) {
  return _then(_AddMemberRequest(
email: null == email ? _self.email : email // ignore: cast_nullable_to_non_nullable
as String,accessLevel: null == accessLevel ? _self.accessLevel : accessLevel // ignore: cast_nullable_to_non_nullable
as AccessLevelJson,
  ));
}


}


/// @nodoc
mixin _$AddMembersBulkRequest {

 String get categoryId; List<AddMemberRequest> get members;
/// Create a copy of AddMembersBulkRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$AddMembersBulkRequestCopyWith<AddMembersBulkRequest> get copyWith => _$AddMembersBulkRequestCopyWithImpl<AddMembersBulkRequest>(this as AddMembersBulkRequest, _$identity);

  /// Serializes this AddMembersBulkRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is AddMembersBulkRequest&&(identical(other.categoryId, categoryId) || other.categoryId == categoryId)&&const DeepCollectionEquality().equals(other.members, members));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,categoryId,const DeepCollectionEquality().hash(members));

@override
String toString() {
  return 'AddMembersBulkRequest(categoryId: $categoryId, members: $members)';
}


}

/// @nodoc
abstract mixin class $AddMembersBulkRequestCopyWith<$Res>  {
  factory $AddMembersBulkRequestCopyWith(AddMembersBulkRequest value, $Res Function(AddMembersBulkRequest) _then) = _$AddMembersBulkRequestCopyWithImpl;
@useResult
$Res call({
 String categoryId, List<AddMemberRequest> members
});




}
/// @nodoc
class _$AddMembersBulkRequestCopyWithImpl<$Res>
    implements $AddMembersBulkRequestCopyWith<$Res> {
  _$AddMembersBulkRequestCopyWithImpl(this._self, this._then);

  final AddMembersBulkRequest _self;
  final $Res Function(AddMembersBulkRequest) _then;

/// Create a copy of AddMembersBulkRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? categoryId = null,Object? members = null,}) {
  return _then(_self.copyWith(
categoryId: null == categoryId ? _self.categoryId : categoryId // ignore: cast_nullable_to_non_nullable
as String,members: null == members ? _self.members : members // ignore: cast_nullable_to_non_nullable
as List<AddMemberRequest>,
  ));
}

}


/// Adds pattern-matching-related methods to [AddMembersBulkRequest].
extension AddMembersBulkRequestPatterns on AddMembersBulkRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _AddMembersBulkRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _AddMembersBulkRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _AddMembersBulkRequest value)  $default,){
final _that = this;
switch (_that) {
case _AddMembersBulkRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _AddMembersBulkRequest value)?  $default,){
final _that = this;
switch (_that) {
case _AddMembersBulkRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String categoryId,  List<AddMemberRequest> members)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _AddMembersBulkRequest() when $default != null:
return $default(_that.categoryId,_that.members);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String categoryId,  List<AddMemberRequest> members)  $default,) {final _that = this;
switch (_that) {
case _AddMembersBulkRequest():
return $default(_that.categoryId,_that.members);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String categoryId,  List<AddMemberRequest> members)?  $default,) {final _that = this;
switch (_that) {
case _AddMembersBulkRequest() when $default != null:
return $default(_that.categoryId,_that.members);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _AddMembersBulkRequest implements AddMembersBulkRequest {
  const _AddMembersBulkRequest({required this.categoryId, required final  List<AddMemberRequest> members}): _members = members;
  factory _AddMembersBulkRequest.fromJson(Map<String, dynamic> json) => _$AddMembersBulkRequestFromJson(json);

@override final  String categoryId;
 final  List<AddMemberRequest> _members;
@override List<AddMemberRequest> get members {
  if (_members is EqualUnmodifiableListView) return _members;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_members);
}


/// Create a copy of AddMembersBulkRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$AddMembersBulkRequestCopyWith<_AddMembersBulkRequest> get copyWith => __$AddMembersBulkRequestCopyWithImpl<_AddMembersBulkRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$AddMembersBulkRequestToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _AddMembersBulkRequest&&(identical(other.categoryId, categoryId) || other.categoryId == categoryId)&&const DeepCollectionEquality().equals(other._members, _members));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,categoryId,const DeepCollectionEquality().hash(_members));

@override
String toString() {
  return 'AddMembersBulkRequest(categoryId: $categoryId, members: $members)';
}


}

/// @nodoc
abstract mixin class _$AddMembersBulkRequestCopyWith<$Res> implements $AddMembersBulkRequestCopyWith<$Res> {
  factory _$AddMembersBulkRequestCopyWith(_AddMembersBulkRequest value, $Res Function(_AddMembersBulkRequest) _then) = __$AddMembersBulkRequestCopyWithImpl;
@override @useResult
$Res call({
 String categoryId, List<AddMemberRequest> members
});




}
/// @nodoc
class __$AddMembersBulkRequestCopyWithImpl<$Res>
    implements _$AddMembersBulkRequestCopyWith<$Res> {
  __$AddMembersBulkRequestCopyWithImpl(this._self, this._then);

  final _AddMembersBulkRequest _self;
  final $Res Function(_AddMembersBulkRequest) _then;

/// Create a copy of AddMembersBulkRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? categoryId = null,Object? members = null,}) {
  return _then(_AddMembersBulkRequest(
categoryId: null == categoryId ? _self.categoryId : categoryId // ignore: cast_nullable_to_non_nullable
as String,members: null == members ? _self._members : members // ignore: cast_nullable_to_non_nullable
as List<AddMemberRequest>,
  ));
}


}

// dart format on
