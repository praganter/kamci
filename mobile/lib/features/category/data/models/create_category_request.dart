import 'package:freezed_annotation/freezed_annotation.dart';
import 'member_embedded_model.dart';

part 'create_category_request.freezed.dart';
part 'create_category_request.g.dart';

@freezed
abstract class CreateCategoryRequest with _$CreateCategoryRequest {
  const factory CreateCategoryRequest({
    required String id,
    required String name,
  }) = _CreateCategoryRequest;

  factory CreateCategoryRequest.fromJson(Map<String, dynamic> json) =>
      _$CreateCategoryRequestFromJson(json);
}

@freezed
abstract class AddMemberRequest with _$AddMemberRequest {
  const factory AddMemberRequest({
    required String email,
    required AccessLevelJson accessLevel,
  }) = _AddMemberRequest;

  factory AddMemberRequest.fromJson(Map<String, dynamic> json) =>
      _$AddMemberRequestFromJson(json);
}

@freezed
abstract class AddMembersBulkRequest with _$AddMembersBulkRequest {
  const factory AddMembersBulkRequest({
    required List<String> categoryIds,
    required String email,
    required AccessLevelJson accessLevel,
  }) = _AddMembersBulkRequest;

  factory AddMembersBulkRequest.fromJson(Map<String, dynamic> json) =>
      _$AddMembersBulkRequestFromJson(json);
}
