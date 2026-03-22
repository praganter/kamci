package dev.batuhanyetgin.debttracker.repositoryservice;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryRepositoryServiceTest {

    @Mock private CategoryRepository categoryRepository;

    private CategoryRepositoryService categoryRepositoryService;

    private static final String CAT_ID   = "cat123";
    private static final String OWNER_ID = "owner123";
    private static final String USER_ID  = "user456";

    @BeforeEach
    void setUp() {
        categoryRepositoryService = new CategoryRepositoryService(categoryRepository);
    }

    private CategoryDocument buildCategory() {
        return CategoryDocument.builder()
                .id(CAT_ID)
                .ownerId(OWNER_ID)
                .name("Test Category")
                .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("existing id → returns category")
        void existingId_returnsCategory() {
            final CategoryDocument cat = buildCategory();
            when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(cat));
            assertThat(categoryRepositoryService.findById(CAT_ID)).isEqualTo(cat);
        }

        @Test
        @DisplayName("missing id → throws NOT_FOUND")
        void missingId_throwsNotFound() {
            when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.empty());
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> categoryRepositoryService.findById(CAT_ID))
                    .matches(e -> e.getStatus() == HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("save / delete")
    class SaveDelete {

        @Test
        @DisplayName("save delegates to repository")
        void save_delegates() {
            final CategoryDocument cat = buildCategory();
            when(categoryRepository.save(cat)).thenReturn(cat);
            assertThat(categoryRepositoryService.save(cat)).isEqualTo(cat);
            verify(categoryRepository).save(cat);
        }

        @Test
        @DisplayName("delete calls deleteById")
        void delete_callsDeleteById() {
            categoryRepositoryService.delete(CAT_ID);
            verify(categoryRepository).deleteById(CAT_ID);
        }
    }

    @Nested
    @DisplayName("query methods")
    class QueryMethods {

        @Test
        @DisplayName("findByOwnerId returns list")
        void findByOwnerId_returnsList() {
            final List<CategoryDocument> cats = List.of(buildCategory());
            when(categoryRepository.findByOwnerId(OWNER_ID)).thenReturn(cats);
            assertThat(categoryRepositoryService.findByOwnerId(OWNER_ID)).isEqualTo(cats);
        }

        @Test
        @DisplayName("findByMemberUserId delegates to findByMembersUserId")
        void findByMemberUserId_delegates() {
            final List<CategoryDocument> cats = List.of(buildCategory());
            when(categoryRepository.findByMembersUserId(USER_ID)).thenReturn(cats);
            assertThat(categoryRepositoryService.findByMemberUserId(USER_ID)).isEqualTo(cats);
        }

        @Test
        @DisplayName("countByOwnerId returns count")
        void countByOwnerId_returnsCount() {
            when(categoryRepository.countByOwnerId(OWNER_ID)).thenReturn(3L);
            assertThat(categoryRepositoryService.countByOwnerId(OWNER_ID)).isEqualTo(3L);
        }
    }
}
