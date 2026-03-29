package dev.batuhanyetgin.debttracker.repositoryservice;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtRepositoryService {

    private final DebtRepository debtRepository;

    public DebtDocument findById(final String id) {
        return debtRepository.findById(id)
                .orElseThrow(() -> BaseException.notFound("error.debt.not_found", id));
    }

    public DebtDocument save(final DebtDocument debtDocument) {
        return debtRepository.save(debtDocument);
    }

    public DebtDocument update(final DebtDocument debtDocument) {
        return debtRepository.save(debtDocument);
    }

    public void delete(final String id) {
        debtRepository.deleteById(id);
    }

    public List<DebtDocument> findByCategoryId(final String categoryId) {
        return debtRepository.findByCategoryId(categoryId);
    }

    public boolean existsByCategoryId(final String categoryId) {
        return debtRepository.existsByCategoryId(categoryId);
    }

    public void deleteByCategoryId(final String categoryId) {
        debtRepository.deleteByCategoryId(categoryId);
    }

    public List<DebtDocument> findByCategoryIdIn(final List<String> categoryIds) {
        return debtRepository.findByCategoryIdIn(categoryIds);
    }

    public long countByOwnerId(final String ownerId) {
        return debtRepository.countByOwnerId(ownerId);
    }

    public List<DebtDocument> findDueReminders(final LocalDateTime now) {
        return debtRepository.findByStatusAndReminderEnabledTrueAndNextReminderDateLessThanEqual(
                DebtStatus.ACTIVE, now);
    }

    public List<DebtDocument> findDueAutoPayments(final LocalDateTime now) {
        return debtRepository.findByAutoPaymentTrueAndNextAutoPaymentDateLessThanEqual(now);
    }

    public List<DebtDocument> findActiveManualDueDebts(final LocalDateTime now) {
        return debtRepository.findByStatusAndAutoPaymentFalseAndFirstPaymentDateLessThanEqual(
                DebtStatus.ACTIVE, now);
    }
}