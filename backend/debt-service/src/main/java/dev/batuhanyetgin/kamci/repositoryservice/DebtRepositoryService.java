package dev.batuhanyetgin.kamci.repositoryservice;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import dev.batuhanyetgin.kamci.enums.PaymentStatus;
import dev.batuhanyetgin.kamci.repository.DebtRepository;
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

    public long countByOwnerIdAndCategoryIdIn(final String ownerId, final List<String> categoryIds) {
        return debtRepository.countByOwnerIdAndCategoryIdIn(ownerId, categoryIds);
    }

    public List<DebtDocument> findDueReminders(final LocalDateTime now) {
        return debtRepository.findByStatusAndPaymentStatusInAndReminderEnabledTrueAndNextReminderDateLessThanEqual(
                DebtStatus.ACTIVE, List.of(PaymentStatus.UNPAID, PaymentStatus.OVERDUE, PaymentStatus.PARTIALLY_PAID), now);
    }

    public List<DebtDocument> findDueAutoPayments(final LocalDateTime now) {
        return debtRepository.findByAutoPaymentTrueAndNextAutoPaymentDateLessThanEqual(now);
    }

    public List<DebtDocument> findUnpaidManualDueDebts(final LocalDateTime now) {
        return debtRepository.findByStatusAndPaymentStatusInAndAutoPaymentFalseAndFirstPaymentDateLessThanEqual(
                DebtStatus.ACTIVE, List.of(PaymentStatus.UNPAID, PaymentStatus.PARTIALLY_PAID), now);
    }

    public List<DebtDocument> findActiveDebts() {
        return debtRepository.findByStatus(DebtStatus.ACTIVE);
    }
}