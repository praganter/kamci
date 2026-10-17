package dev.batuhanyetgin.kamci.repository;

import dev.batuhanyetgin.kamci.document.UserIncomeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserIncomeRepository extends MongoRepository<UserIncomeDocument, String> {

    List<UserIncomeDocument> findByOwnerIdOrderByEffectiveFromDesc(String ownerId);

    Optional<UserIncomeDocument> findTopByOwnerIdOrderByEffectiveFromDesc(String ownerId);

    Optional<UserIncomeDocument> findTopByOwnerIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            String ownerId, LocalDateTime asOf);

    void deleteByOwnerId(String ownerId);
}
