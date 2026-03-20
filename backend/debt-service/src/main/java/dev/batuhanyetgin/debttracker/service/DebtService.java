package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import dev.batuhanyetgin.commonmodule.event.DebtAddedEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.CategoryDebtResponse;
import dev.batuhanyetgin.debttracker.dto.response.DebtResponse;
import dev.batuhanyetgin.debttracker.dto.response.GetDebtResponse;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.mapper.DebtMapper;
import dev.batuhanyetgin.debttracker.producer.DebtEventProducer;
import dev.batuhanyetgin.debttracker.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {

    private static final int MAX_DEBTS = 150;

    private final DebtRepositoryService debtRS;
    private final CategoryRepositoryService categoryRS;
    private final DebtMapper debtMapper;
    private final DebtEventProducer debtEventProducer;

    public DebtResponse create(final CreateDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();

        if (debtRS.countByOwnerId(userId) >= MAX_DEBTS) {
            throw BaseException.badRequest("error.debt.limit_exceeded");
        }

        final CategoryDocument category = categoryRS.findById(request.getCategoryId());
        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        request.setOwnerId(userId);

        DebtDocument debtDocument = debtMapper.convert(request);
        setNextReminderDate(debtDocument);
        debtDocument = debtRS.save(debtDocument);

        log.info("Debt created with id: {} for user: {}", debtDocument.getId(), userId);
        produceAddedEventIfNeeded(debtDocument, category, userId);

        return DebtResponse.builder().debt(debtMapper.convert(debtDocument)).build();
    }

    public DebtResponse update(final UpdateDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(request.getId());

        final CategoryDocument currentCategory = categoryRS.findById(existing.getCategoryId());
        if (lacksWriteAccess(currentCategory, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        if (!existing.getCategoryId().equals(request.getCategoryId())) {
            final CategoryDocument newCategory = categoryRS.findById(request.getCategoryId());
            if (lacksWriteAccess(newCategory, userId)) {
                throw BaseException.forbidden("error.debt.permission_denied");
            }
        }

        request.setOwnerId(existing.getOwnerId());

        DebtDocument debtDocument = debtMapper.convert(request);
        setNextReminderDate(debtDocument);
        debtDocument = debtRS.update(debtDocument);

        log.info("Debt updated with id: {}", debtDocument.getId());
        return DebtResponse.builder().debt(debtMapper.convert(debtDocument)).build();
    }

    public void delete(final String id) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument debtDocument = debtRS.findById(id);

        final CategoryDocument category = categoryRS.findById(debtDocument.getCategoryId());
        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        debtRS.delete(id);
        log.warn("Deleted debt: {}", id);
    }

    public GetDebtResponse get() {
        final String userId = RequestContextUtil.getUserId();

        log.info("Getting debts for user: {}", userId);

        final List<CategoryDocument> owned = categoryRS.findByOwnerId(userId);
        final List<CategoryDocument> shared = categoryRS.findByMemberUserId(userId);

        final List<CategoryDocument> all = new ArrayList<>();
        all.addAll(owned);
        all.addAll(shared);

        final List<String> categoryIds = all.stream().map(BaseDocument::getId).toList();
        final Map<String, List<DebtDocument>> byCategory = debtRS.findByCategoryIdIn(categoryIds)
                .stream().collect(Collectors.groupingBy(DebtDocument::getCategoryId));

        final Set<String> ownedIds = owned.stream()
                .map(BaseDocument::getId).collect(Collectors.toSet());

        final List<CategoryDebtResponse> categories = all.stream()
                .map(cat -> CategoryDebtResponse.builder()
                        .categoryId(cat.getId())
                        .categoryName(cat.getName())
                        .owned(ownedIds.contains(cat.getId()))
                        .ownerUserId(ownedIds.contains(cat.getId()) ? null : cat.getOwnerId())
                        .debts(byCategory.getOrDefault(cat.getId(), List.of())
                                .stream().map(debtMapper::convert).toList())
                        .build())
                .toList();

        return GetDebtResponse.builder().categories(categories).build();
    }

    private boolean lacksWriteAccess(final CategoryDocument category, final String userId) {
        if (category.getOwnerId().equals(userId)) {
            return false;
        }
        return category.getMembers().stream()
                .noneMatch(m -> m.getUserId().equals(userId)
                        && m.getAccessLevel() == AccessLevelType.WRITE);
    }

    private void produceAddedEventIfNeeded(
            final DebtDocument debt, final CategoryDocument category, final String creatorId) {
        final List<String> memberIds = category.getMembers().stream()
                .map(MemberEmbedded::getUserId)
                .filter(uid -> !uid.equals(creatorId))
                .toList();

        if (memberIds.isEmpty()) {
            return;
        }

        debtEventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.DEBT_ADDED_ROUTING_KEY,
                DebtAddedEvent.builder()
                        .debtId(debt.getId())
                        .ownerId(debt.getOwnerId())
                        .debtName(debt.getName())
                        .amount(debt.getAmount())
                        .memberUserIds(memberIds)
                        .build()
        );
        log.info("Debt added event produced for {} member(s)", memberIds.size());
    }

    private void setNextReminderDate(final DebtDocument debtDocument) {
        final ReminderEmbedded reminder = debtDocument.getReminder();

        if (reminder == null || !reminder.isEnabled()
                || reminder.getReminderDate() == null || reminder.getReminderTime() == null) {
            return;
        }

        debtDocument.setNextReminderDate(
                LocalDateTime.of(reminder.getReminderDate(), reminder.getReminderTime())
        );
    }
}