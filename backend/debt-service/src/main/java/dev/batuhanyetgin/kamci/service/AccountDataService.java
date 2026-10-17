package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.UserIncomeRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cascading cleanup of a user's debt-service data for account deletion,
 * called internally by auth-service when a user deletes their account.
 * Idempotent — safe to retry if a prior attempt only partially completed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDataService {

    private final CategoryRepositoryService categoryRS;
    private final DebtRepositoryService debtRS;
    private final UserIncomeRepositoryService userIncomeRS;

    public void deleteUserData(final String userId) {
        categoryRS.findByOwnerId(userId).forEach(category -> {
            debtRS.deleteByCategoryId(category.getId());
            categoryRS.delete(category.getId());
        });

        categoryRS.findByMemberUserId(userId).forEach(category -> removeMember(category, userId));

        userIncomeRS.deleteByOwnerId(userId);

        log.info("Deleted debt-service data for userId: {}", userId);
    }

    private void removeMember(final CategoryDocument category, final String userId) {
        category.setMembers(category.getMembers().stream()
                .filter(m -> !m.getUserId().equals(userId))
                .toList());
        categoryRS.save(category);
    }
}
