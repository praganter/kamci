package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.DebtResponse;
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
    public DebtResponse create(final CreateDebtRequest request) {
        return DebtResponse.builder()
                .debt(debtRepositoryService.save(debtMapper.convert(request)))
                .build();
    }

    @Transactional
    public DebtResponse update(final UpdateDebtRequest request) {
        return DebtResponse.builder()
                .debt(debtRepositoryService.update(debtMapper.convert(request)))
                .build();
    }

    @Transactional
    public void delete(final String id) {
        debtRepositoryService.delete(id);
    }
}