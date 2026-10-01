package dev.batuhanyetgin.commonmodule.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseRepositoryServiceTest {

    @Mock
    private BaseRepository<String, String> baseRepository;

    private BaseRepositoryService<String> service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new BaseRepositoryService<>(baseRepository);
    }

    @Test
    @DisplayName("findById → found returns document")
    void findById_found_returnsDocument() {
        when(baseRepository.findById("id1")).thenReturn(Optional.of("doc1"));

        assertThat(service.findById("id1")).isEqualTo("doc1");
    }

    @Test
    @DisplayName("findById → not found throws IllegalArgumentException")
    void findById_notFound_throws() {
        when(baseRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("missing")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("save → delegates to repository")
    void save_delegatesToRepository() {
        when(baseRepository.save("doc1")).thenReturn("doc1");

        assertThat(service.save("doc1")).isEqualTo("doc1");
    }

    @Test
    @DisplayName("update → delegates to repository save")
    void update_delegatesToRepositorySave() {
        when(baseRepository.save("doc1")).thenReturn("doc1");

        assertThat(service.update("doc1")).isEqualTo("doc1");
    }
}
