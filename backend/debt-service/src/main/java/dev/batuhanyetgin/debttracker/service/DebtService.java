package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.mapper.DebtMapper;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepositoryService debtRepositoryService;
    private final DebtMapper debtMapper;

    @Transactional
    public void save(final CreateDebtRequest request) {
        debtRepositoryService.save(debtMapper.convert(request));
    }

    @Transactional
    public void update(final CreateDebtRequest request) {
        if (request.getId() != null && !request.getId().isBlank()) {
            debtRepositoryService.update(debtMapper.convert(request));
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Transactional
    public void delete(final String id) {
        debtRepositoryService.delete(id);
    }
}