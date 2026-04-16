package dev.batuhanyetgin.kamci.repositoryservice;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.kamci.document.UserIncomeDocument;
import dev.batuhanyetgin.kamci.repository.UserIncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserIncomeRepositoryService {

    private final UserIncomeRepository userIncomeRepository;

    public UserIncomeDocument save(final UserIncomeDocument document) {
        return userIncomeRepository.save(document);
    }

    public UserIncomeDocument findById(final String id) {
        return userIncomeRepository.findById(id)
                .orElseThrow(() -> BaseException.notFound("error.income.not_found", id));
    }

    public void delete(final String id) {
        userIncomeRepository.deleteById(id);
    }

    public List<UserIncomeDocument> findByOwnerIdOrderByEffectiveFromDesc(final String ownerId) {
        return userIncomeRepository.findByOwnerIdOrderByEffectiveFromDesc(ownerId);
    }

    public Optional<UserIncomeDocument> findCurrentByOwnerId(final String ownerId) {
        return userIncomeRepository.findTopByOwnerIdOrderByEffectiveFromDesc(ownerId);
    }

    public Optional<UserIncomeDocument> findByOwnerIdAsOf(final String ownerId, final LocalDateTime asOf) {
        return userIncomeRepository
                .findTopByOwnerIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(ownerId, asOf);
    }
}
