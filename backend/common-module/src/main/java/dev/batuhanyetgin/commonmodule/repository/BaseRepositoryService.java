package dev.batuhanyetgin.commonmodule.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Getter
public class BaseRepositoryService<D> {

    private final BaseRepository<D, String> baseRepository;

    public D findById(final String id) {
        return baseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Could not find document with id: " + id));
    }

    public D save(final D document) {
        return baseRepository.save(document);
    }

    public D update(final D document) {
        return baseRepository.save(document);
    }
}