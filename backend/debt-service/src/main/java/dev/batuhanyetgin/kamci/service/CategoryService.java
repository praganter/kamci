package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.commonmodule.event.CategoryMemberAddedEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.kamci.client.AuthClient;
import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.kamci.dto.request.AddMemberRequest;
import dev.batuhanyetgin.kamci.dto.request.BulkAddMemberRequest;
import dev.batuhanyetgin.kamci.dto.request.CreateCategoryRequest;
import dev.batuhanyetgin.kamci.dto.request.UpdateCategoryRequest;
import dev.batuhanyetgin.kamci.dto.response.GetAllCategoryResponse;
import dev.batuhanyetgin.kamci.dto.response.MigrateCategoryResponse;
import dev.batuhanyetgin.kamci.dto.response.SaveCategoryResponse;
import dev.batuhanyetgin.kamci.dto.response.UpdateCategoryResponse;
import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import dev.batuhanyetgin.kamci.mapper.CategoryMapper;
import dev.batuhanyetgin.kamci.producer.DebtEventProducer;
import dev.batuhanyetgin.kamci.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final int FREE_MAX_CATEGORIES = 2;
    private static final int PREMIUM_MAX_CATEGORIES = 100;

    private final CategoryRepositoryService categoryRS;
    private final DebtRepositoryService debtRS;
    private final AuthClient authClient;
    private final DebtEventProducer eventProducer;
    private final CategoryMapper categoryMapper;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    public SaveCategoryResponse save(final CreateCategoryRequest request) {
        final String userId = RequestContextUtil.getUserId();

        final int maxCategories = RequestContextUtil.isPremium() ? PREMIUM_MAX_CATEGORIES : FREE_MAX_CATEGORIES;
        if (categoryRS.countByOwnerIdAndMembersIsEmpty(userId) >= maxCategories) {
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

    public void delete(final String id) {
        final String userId = RequestContextUtil.getUserId();
        final CategoryDocument category = categoryRS.findById(id);

        checkOwner(category, userId);

        debtRS.deleteByCategoryId(id);
        categoryRS.delete(id);
        log.warn("Deleted category {} and all its debts for user: {}", id, userId);
    }

    public MigrateCategoryResponse deleteAndMigrate(final String sourceId, final String targetId) {
        if (sourceId.equals(targetId)) {
            throw BaseException.badRequest("error.category.migrate_same_category");
        }

        final String userId = RequestContextUtil.getUserId();

        final CategoryDocument source = categoryRS.findById(sourceId);
        checkOwner(source, userId);

        final CategoryDocument target = categoryRS.findById(targetId);
        checkOwner(target, userId);

        final List<DebtDocument> debts = debtRS.findByCategoryId(sourceId);
        for (final DebtDocument debt : debts) {
            debt.setCategoryId(targetId);
            debtRS.update(debt);
        }

        categoryRS.delete(sourceId);
        log.info("Migrated {} debts from category {} to {} for user {}", debts.size(), sourceId, targetId, userId);

        return MigrateCategoryResponse.builder()
                .migratedDebtCount(debts.size())
                .build();
    }

    public void addMember(final String categoryId, final AddMemberRequest request) {
        if (!RequestContextUtil.isPremium()) {
            throw BaseException.forbidden("error.category.premium_required");
        }
        final String userId = RequestContextUtil.getUserId();
        final CategoryDocument category = categoryRS.findById(categoryId);

        checkOwner(category, userId);
        addMemberToCategory(category, request.getEmail(), request.getAccessLevel());
        log.info("Added member {} to category {} with access {}", request.getEmail(), categoryId, request.getAccessLevel());
    }

    public void bulkAddMember(final BulkAddMemberRequest request) {
        if (!RequestContextUtil.isPremium()) {
            throw BaseException.forbidden("error.category.premium_required");
        }
        final String userId = RequestContextUtil.getUserId();

        final List<CategoryDocument> categoryDocumentList = categoryRS.findByIdIn(request.getCategoryIds());
        Map<String, CategoryDocument> categoryDocumentMap = categoryDocumentList.stream()
                .collect(Collectors.toMap(CategoryDocument::getId, Function.identity()));

        for (final String categoryId : request.getCategoryIds()) {
            final CategoryDocument category = categoryDocumentMap.get(categoryId);
            if (category == null) {
                throw BaseException.notFound("error.category.not_found");
            }
            checkOwner(category, userId);
            addMemberToCategory(category, request.getEmail(), request.getAccessLevel());
            log.info("Bulk added member {} to category {} with access {}", request.getEmail(), categoryId, request.getAccessLevel());
        }
    }

    private void addMemberToCategory(final CategoryDocument category, final String email, final AccessLevelType accessLevel) {
        final String memberUserId;
        try {
            memberUserId = authClient.getUserIdByEmail(email);
        } catch (feign.FeignException.NotFound e) {
            throw BaseException.notFound("error.category.member_user_not_found", email);
        }

        final boolean alreadyMember = category.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(memberUserId));

        if (alreadyMember) {
            throw BaseException.conflict("error.category.member_already_exists");
        }

        final List<MemberEmbedded> updated = new ArrayList<>(category.getMembers());
        updated.add(MemberEmbedded.builder()
                .userId(memberUserId)
                .email(email)
                .accessLevel(accessLevel)
                .build());
        category.setMembers(updated);
        categoryRS.save(category);

        eventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.CATEGORY_MEMBER_ADDED_ROUTING_KEY,
                CategoryMemberAddedEvent.builder()
                        .categoryId(category.getId())
                        .categoryName(category.getName())
                        .addedByUserId(category.getOwnerId())
                        .memberUserId(memberUserId)
                        .memberEmail(email)
                        .locale(RequestContextUtil.getLocale())
                        .build()
        );
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
        log.info("Getting all categories for user: {}", userId);

        final CompletableFuture<List<CategoryDocument>> ownedFuture =
                CompletableFuture.supplyAsync(() -> categoryRS.findByOwnerId(userId), taskExecutor);
        final CompletableFuture<List<CategoryDocument>> memberOfFuture =
                CompletableFuture.supplyAsync(() -> categoryRS.findByMemberUserId(userId), taskExecutor);
        CompletableFuture.allOf(ownedFuture, memberOfFuture).join();

        final List<CategoryDocument> all = new ArrayList<>(ownedFuture.join());
        all.addAll(memberOfFuture.join());

        return all.stream()
                .map(categoryMapper::convert)
                .toList();
    }

    private void checkOwner(final CategoryDocument category, final String userId) {
        if (!category.getOwnerId().equals(userId)) {
            throw BaseException.forbidden("error.category.permission_denied");
        }
    }
}