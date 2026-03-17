package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.event.DebtAddedEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.SharedWithEmbedded;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.DebtResponse;
import dev.batuhanyetgin.debttracker.dto.response.GetDebtResponse;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.mapper.DebtMapper;
import dev.batuhanyetgin.debttracker.producer.DebtEventProducer;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepositoryService debtRS;
    private final DebtMapper debtMapper;
    private final DebtEventProducer debtEventProducer;

    public DebtResponse create(final CreateDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();
        request.setOwnerId(userId);

        log.info("Creating debt for user: {}", userId);

        DebtDocument debtDocument = debtMapper.convert(request);
        setNextReminderDate(debtDocument);
        debtDocument = debtRS.save(debtDocument);

        produceSharedDebtEventIfNeeded(debtDocument);

        return DebtResponse.builder()
                .debt(debtDocument)
                .build();
    }

    public DebtResponse update(final UpdateDebtRequest request) {
        log.info("Updating debt with id: {}", request.getId());

        final String currentUserId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(request.getId());

        if (checkForPermission(existing, currentUserId)) {
            log.error("Permission denied for user {} to update debt {}", currentUserId, request.getId());
            throw new BaseException("error.debt.permission_denied", HttpStatus.FORBIDDEN);
        }

        request.setOwnerId(existing.getOwnerId());

        DebtDocument debtDocument = debtMapper.convert(request);
        setNextReminderDate(debtDocument);
        debtDocument = debtRS.update(debtDocument);

        produceSharedDebtEventIfNeeded(debtDocument);

        return DebtResponse.builder()
                .debt(debtDocument)
                .build();
    }

    public void delete(final String id) {
        log.warn("Deleting debt with id: {}", id);

        final String currentUserId = RequestContextUtil.getUserId();
        final DebtDocument debtDocument = debtRS.findById(id);

        if (checkForPermission(debtDocument, currentUserId)) {
            log.error("Permission denied for user {} to delete debt {}", currentUserId, id);
            throw new BaseException("error.debt.permission_denied", HttpStatus.FORBIDDEN);
        }

        debtRS.delete(id);
    }

    public GetDebtResponse get() {
        final String userId = RequestContextUtil.getUserId();

        log.info("Getting debt with userId: {}", userId);

        return GetDebtResponse.builder()
                .ownerDebts(debtRS.findByOwnerId(userId))
                .sharedDebts(debtRS.findBySharedWithUserId(userId))
                .build();
    }

    private void produceSharedDebtEventIfNeeded(final DebtDocument debtDocument) {
        if (CollectionUtils.isEmpty(debtDocument.getSharedWith())) {
            return;
        }

        final List<String> sharedUserIds = debtDocument.getSharedWith().stream()
                .map(SharedWithEmbedded::getUserId)
                .toList();

        debtEventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.DEBT_ADDED_ROUTING_KEY,
                DebtAddedEvent.builder()
                        .debtId(debtDocument.getId())
                        .ownerId(debtDocument.getOwnerId())
                        .debtName(debtDocument.getName())
                        .amount(debtDocument.getAmount())
                        .sharedWithUserIds(sharedUserIds)
                        .build()
        );

        log.info("Shared debt event produced for debt {} with {} user(s)", debtDocument.getId(), sharedUserIds.size());
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

    private boolean checkForPermission(final DebtDocument debtDocument, final String userId) {
        if (debtDocument.getOwnerId().equals(userId)) {
            return false;
        }

        if (debtDocument.getSharedWith() != null) {
            return debtDocument.getSharedWith().stream()
                    .noneMatch(sw -> sw.getUserId().equals(userId)
                            && !sw.getAccessLevelType().equals(AccessLevelType.READ));
        }

        return true;
    }
}