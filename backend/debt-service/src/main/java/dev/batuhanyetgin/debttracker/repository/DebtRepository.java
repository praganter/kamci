package dev.batuhanyetgin.debttracker.repository;

import dev.batuhanyetgin.debttracker.document.DebtDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebtRepository extends MongoRepository<DebtDocument, String> {
}