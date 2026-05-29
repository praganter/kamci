import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/category.dart';
import 'member_embedded_model.dart';

part 'category_model.freezed.dart';
part 'category_model.g.dart';

@freezed
sealed class CategoryModel with _$CategoryModel {
  const factory CategoryModel({
    required String id,
    required String ownerId,
    required String name,
    @Default([]) List<MemberEmbeddedModel> members,
    String? createdAt,
    String? updatedAt,
  }) = _CategoryModel;

  factory CategoryModel.fromJson(Map<String, dynamic> json) =>
      _$CategoryModelFromJson(json);
}

extension CategoryModelMapper on CategoryModel {
  Category toDomain() => Category(
    id: id,
    ownerId: ownerId,
    name: name,
    members: members.map((m) => m.toDomain()).toList(),
    createdAt: createdAt != null ? DateTime.tryParse(createdAt!) : null,
    updatedAt: updatedAt != null ? DateTime.tryParse(updatedAt!) : null,
  );
}
