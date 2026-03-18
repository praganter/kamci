package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.debttracker.dto.request.AddMemberRequest;
import dev.batuhanyetgin.debttracker.dto.request.BulkAddMemberRequest;
import dev.batuhanyetgin.debttracker.dto.request.CreateCategoryRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateCategoryRequest;
import dev.batuhanyetgin.debttracker.dto.response.GetAllCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.response.SaveCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.response.UpdateCategoryResponse;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final int MAX_CATEGORIES = 150;

    private final CategoryRepositoryService categoryRS;
    private final DebtRepositoryService debtRS;

    public SaveCategoryResponse save(final CreateCategoryRequest request) {
        final String userId = RequestContextUtil.getUserId();

        if (categoryRS.countByOwnerId(userId) >= MAX_CATEGORIES) {
            throw BaseException.badRequest("error.category.limit_exceeded");
        }

        final CategoryDocument saved = categoryRS.save(CategoryDocument.builder()
                .ownerId(userId)
                .name(request.getName())
                .build());

        log.info("Category created with id: {}", saved.getId());
        return SaveCategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .build();
    }

    public UpdateCategoryResponse update(final UpdateCategoryRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final CategoryDocument category = categoryRS.findById(request.getId());

        checkOwner(category, userId);
        category.setName(request.getName());

        final CategoryDocument updated = categoryRS.save(category);
        log.info("Updated category {} name to '{}'", request.getId(), request.getName());
        return UpdateCategoryResponse.builder()
                .id(updated.getId())
                .name(updated.getName())
                .build();
    }

    @Transactional
    public void delete(final String id) {
        final String userId = RequestContextUtil.getUserId();
        final CategoryDocument category = categoryRS.findById(id);

        checkOwner(category, userId);

        debtRS.deleteByCategoryId(id);
        categoryRS.delete(id);
        log.warn("Deleted category {} and all its debts for user: {}", id, userId);
    }

    public void addMember(final String categoryId, final AddMemberRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final CategoryDocument category = categoryRS.findById(categoryId);

        checkOwner(category, userId);
        addMemberToCategory(category, request.getUserId(), request.getAccessLevel());
        log.info("Added member {} to category {} with access {}", request.getUserId(), categoryId, request.getAccessLevel());
    }

    @Transactional
    public void bulkAddMember(final BulkAddMemberRequest request) {
        final String userId = RequestContextUtil.getUserId();

        for (final String categoryId : request.getCategoryIds()) {
            final CategoryDocument category = categoryRS.findById(categoryId);
            checkOwner(category, userId);
            addMemberToCategory(category, request.getUserId(), request.getAccessLevel());
            log.info("Bulk added member {} to category {} with access {}", request.getUserId(), categoryId, request.getAccessLevel());
        }
    }

    private void addMemberToCategory(final CategoryDocument category, final String memberUserId, final AccessLevelType accessLevel) {
        final boolean alreadyMember = category.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(memberUserId));

        if (alreadyMember) {
            throw BaseException.conflict("error.category.member_already_exists");
        }

        final List<MemberEmbedded> updated = new ArrayList<>(category.getMembers());
        updated.add(MemberEmbedded.builder()
                .userId(memberUserId)
                .accessLevel(accessLevel)
                .build());
        category.setMembers(updated);
        categoryRS.save(category);
    }

    public void removeMember(final String categoryId, final String memberUserId) {
        final String userId = RequestContextUtil.getUserId();
        final CategoryDocument category = categoryRS.findById(categoryId);

        checkOwner(category, userId);

        category.setMembers(category.getMembers().stream()
                .filter(m -> !m.getUserId().equals(memberUserId))
                .toList());

        categoryRS.save(category);
        log.info("Removed member {} from category {}", memberUserId, categoryId);
    }

    public List<GetAllCategoryResponse> getAll() {
        final String userId = RequestContextUtil.getUserId();

        final List<CategoryDocument> owned = categoryRS.findByOwnerId(userId);
        final List<CategoryDocument> memberOf = categoryRS.findByMemberUserId(userId);

        final List<CategoryDocument> all = new ArrayList<>(owned);
        all.addAll(memberOf);

        return all.stream()
                .map(c -> GetAllCategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .ownerId(c.getOwnerId())
                        .members(c.getMembers().stream()
                                .map(m -> GetAllCategoryResponse.MemberItem.builder()
                                        .userId(m.getUserId())
                                        .accessLevel(m.getAccessLevel())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private void checkOwner(final CategoryDocument category, final String userId) {
        if (!category.getOwnerId().equals(userId)) {
            throw BaseException.forbidden("error.category.permission_denied");
        }
    }
}