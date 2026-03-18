package dev.batuhanyetgin.debttracker.repository;

import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
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

    List<DebtDocument> findByStatusAndReminderEnabledTrueAndNextReminderDateBefore(
            final DebtStatus status, final LocalDateTime dateTime);
}