package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.UserIncomeDocument;
import dev.batuhanyetgin.debttracker.dto.request.CreateIncomeRequest;
import dev.batuhanyetgin.debttracker.dto.response.IncomeResponse;
import dev.batuhanyetgin.debttracker.repositoryservice.UserIncomeRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncomeService {

    private final UserIncomeRepositoryService incomeRS;

    public IncomeResponse create(final CreateIncomeRequest request) {
        final String userId = RequestContextUtil.getUserId();

        final UserIncomeDocument document = UserIncomeDocument.builder()
                .ownerId(userId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .effectiveFrom(request.getEffectiveFrom())
                .note(request.getNote())
                .build();

        final UserIncomeDocument saved = incomeRS.save(document);
        log.info("Income record created for user: {} effectiveFrom: {}", userId, request.getEffectiveFrom());
        return toResponse(saved);
    }

    public List<IncomeResponse> getHistory() {
        final String userId = RequestContextUtil.getUserId();
        return incomeRS.findByOwnerIdOrderByEffectiveFromDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public IncomeResponse getCurrent() {
        final String userId = RequestContextUtil.getUserId();
        return incomeRS.findCurrentByOwnerId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> BaseException.notFound("error.income.not_found", userId));
    }

    public void delete(final String id) {
        final String userId = RequestContextUtil.getUserId();
        final UserIncomeDocument document = incomeRS.findById(id);
        if (!document.getOwnerId().equals(userId)) {
            throw BaseException.forbidden("error.income.permission_denied");
        }
        incomeRS.delete(id);
        log.info("Income record {} deleted by user: {}", id, userId);
    }

    private IncomeResponse toResponse(final UserIncomeDocument doc) {
        return IncomeResponse.builder()
                .id(doc.getId())
                .amount(doc.getAmount())
                .currency(doc.getCurrency())
                .effectiveFrom(doc.getEffectiveFrom())
                .note(doc.getNote())
                .createdDate(doc.getCreatedDate())
                .build();
    }
}
