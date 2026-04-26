package dev.batuhanyetgin.kamci.repository;

import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import dev.batuhanyetgin.kamci.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DebtRepository extends MongoRepository<DebtDocument, String> {

    List<DebtDocument> findByCategoryId(final String categoryId);

    boolean existsByCategoryId(final String categoryId);

    void deleteByCategoryId(final String categoryId);

    List<DebtDocument> findByCategoryIdIn(final List<String> categoryIds);

    long countByOwnerId(final String ownerId);

    long countByOwnerIdAndCategoryIdIn(final String ownerId, final List<String> categoryIds);

    List<DebtDocument> findByStatusAndPaymentStatusInAndReminderEnabledTrueAndNextReminderDateLessThanEqual(
            final DebtStatus status, final List<PaymentStatus> paymentStatuses, final LocalDateTime dateTime);

    List<DebtDocument> findByAutoPaymentTrueAndNextAutoPaymentDateLessThanEqual(
            final LocalDateTime dateTime);

    List<DebtDocument> findByStatusAndPaymentStatusInAndAutoPaymentFalseAndFirstPaymentDateLessThanEqual(
            final DebtStatus status, final List<PaymentStatus> paymentStatuses, final LocalDateTime dateTime);

    List<DebtDocument> findByStatus(final DebtStatus status);
}