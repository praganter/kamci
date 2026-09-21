package dev.batuhanyetgin.authservice.repositoryservice;

import dev.batuhanyetgin.authservice.document.HouseholdDocument;
import dev.batuhanyetgin.authservice.enums.HouseholdStatus;
import dev.batuhanyetgin.authservice.repository.HouseholdRepository;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HouseholdRepositoryService {

    private final HouseholdRepository householdRepository;

    public HouseholdDocument save(final HouseholdDocument document) {
        return householdRepository.save(document);
    }

    public HouseholdDocument findById(final String id) {
        return householdRepository.findById(id)
                .orElseThrow(() -> BaseException.notFound("error.household.not_found", id));
    }

    public void delete(final HouseholdDocument document) {
        householdRepository.delete(document);
    }

    public List<HouseholdDocument> findAllForUser(final String userId) {
        final List<HouseholdDocument> households = new ArrayList<>(householdRepository.findByOwnerId(userId));
        households.addAll(householdRepository.findByPartnerId(userId));
        return households;
    }

    public Optional<HouseholdDocument> findAcceptedForUser(final String userId) {
        return findAllForUser(userId).stream()
                .filter(h -> h.getStatus() == HouseholdStatus.ACCEPTED)
                .findFirst();
    }

    public Optional<HouseholdDocument> findActiveForUser(final String userId) {
        return findAllForUser(userId).stream()
                .filter(h -> h.getStatus() == HouseholdStatus.ACCEPTED || h.getStatus() == HouseholdStatus.PENDING)
                .findFirst();
    }
}
