package dev.batuhanyetgin.kamci.repository;

import dev.batuhanyetgin.kamci.document.CategoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends MongoRepository<CategoryDocument, String> {

    List<CategoryDocument> findByOwnerId(final String ownerId);

    List<CategoryDocument> findByMembersUserId(final String userId);

    long countByOwnerId(final String ownerId);
}