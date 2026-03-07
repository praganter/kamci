package dev.batuhanyetgin.debttracker.repository;

import dev.batuhanyetgin.debttracker.document.DebtDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtRepository extends MongoRepository<DebtDocument, String> {

    List<DebtDocument> findByOwnerId(final String ownerId);

    List<DebtDocument> findBySharedWithUserId(final String userId);
}