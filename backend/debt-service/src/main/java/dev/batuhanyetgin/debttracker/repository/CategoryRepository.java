package dev.batuhanyetgin.debttracker.repository;

import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends MongoRepository<CategoryDocument, String> {

    List<CategoryDocument> findByOwnerId(final String ownerId);

    List<CategoryDocument> findByMembersUserId(final String userId);

    long countByOwnerId(final String ownerId);
}