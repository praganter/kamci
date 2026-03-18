package dev.batuhanyetgin.debttracker.repositoryservice;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryRepositoryService {

    private final CategoryRepository categoryRepository;

    public CategoryDocument findById(final String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> BaseException.notFound("error.category.not_found", id));
    }

    public CategoryDocument save(final CategoryDocument document) {
        return categoryRepository.save(document);
    }

    public void delete(final String id) {
        categoryRepository.deleteById(id);
    }

    public List<CategoryDocument> findByOwnerId(final String ownerId) {
        return categoryRepository.findByOwnerId(ownerId);
    }

    public List<CategoryDocument> findByMemberUserId(final String userId) {
        return categoryRepository.findByMembersUserId(userId);
    }

    public long countByOwnerId(final String ownerId) {
        return categoryRepository.countByOwnerId(ownerId);
    }
}