package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.DebtResponse;
import dev.batuhanyetgin.debttracker.dto.response.GetDebtResponse;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.mapper.DebtMapper;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepositoryService debtRS;
    private final DebtMapper debtMapper;

    public DebtResponse create(final CreateDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();
        request.setOwnerId(userId);

        log.info("Creating debt for user: {}", userId);

        return DebtResponse.builder()
                .debt(debtRS.save(debtMapper.convert(request)))
                .build();
    }

    public DebtResponse update(final UpdateDebtRequest request) {
        log.info("Updating debt with id: {}", request.getId());

        final String currentUserId = RequestContextUtil.getUserId();
        final DebtDocument debtDocument = debtRS.findById(request.getId());

        if (checkForPermission(debtDocument, currentUserId)) {
            log.error("Permission denied for user {} to update debt {}", currentUserId, request.getId());
            throw new BaseException("error.debt.permission_denied", HttpStatus.FORBIDDEN);
        }

        request.setOwnerId(debtDocument.getOwnerId());

        return DebtResponse.builder()
                .debt(debtRS.update(debtMapper.convert(request)))
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

    private boolean checkForPermission(final DebtDocument debtDocument, final String userId) {
        if (debtDocument.getOwnerId().equals(userId)) {
            return false;
        }

        if (debtDocument.getSharedWith() != null) {
            return debtDocument.getSharedWith().stream()
                    .noneMatch(sw -> sw.getUserId().equals(userId) && !sw.getAccessLevelType().equals(AccessLevelType.READ));
        }

        return true;
    }
}