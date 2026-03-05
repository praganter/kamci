package dev.batuhanyetgin.debttracker.repositoryservice;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DebtRepositoryService {
    private final DebtRepository debtRepository;

    public DebtDocument findById(final String id) {
        return debtRepository.findById(id)
                .orElseThrow(() ->  new BaseException("error.debt.not_found", HttpStatus.NOT_FOUND, id));
    }

    public DebtDocument save(final DebtDocument debtDocument) {
        return debtRepository.save(debtDocument);
    }

    public DebtDocument update(final DebtDocument debtDocument) {
        return debtRepository.save(debtDocument);
    }

    public void delete(final String id) {
        debtRepository.deleteById(id);
    }
}