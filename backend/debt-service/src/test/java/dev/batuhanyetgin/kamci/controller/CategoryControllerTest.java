package dev.batuhanyetgin.kamci.controller;

import dev.batuhanyetgin.kamci.dto.request.*;
import dev.batuhanyetgin.kamci.dto.response.*;
import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import dev.batuhanyetgin.kamci.service.CategoryService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock private CategoryService categoryService;

    @InjectMocks
    private CategoryController controller;

    private static final String CAT_ID = "cat123";
    private static final String USER_ID = "user456";
    private static final String TARGET_CAT_ID = "cat789";

    @Nested
    class Save {

        @Test
        void delegatesToServiceAndReturns200() {
            final CreateCategoryRequest req = new CreateCategoryRequest();
            final SaveCategoryResponse saved = SaveCategoryResponse.builder().id(CAT_ID).name("Bills").build();
            when(categoryService.save(req)).thenReturn(saved);

            final ResponseEntity<SaveCategoryResponse> response = controller.save(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getId()).isEqualTo(CAT_ID);
            verify(categoryService).save(req);
        }
    }

    @Nested
    class Update {

        @Test
        void delegatesToServiceAndReturns200() {
            final UpdateCategoryRequest req = new UpdateCategoryRequest();
            final UpdateCategoryResponse updated = UpdateCategoryResponse.builder().id(CAT_ID).name("Updated").build();
            when(categoryService.update(req)).thenReturn(updated);

            final ResponseEntity<UpdateCategoryResponse> response = controller.update(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getName()).isEqualTo("Updated");
        }
    }

    @Nested
    class Delete {

        @Test
        void delegatesToServiceAndReturns204() {
            final ResponseEntity<Void> response = controller.delete(CAT_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(categoryService).delete(CAT_ID);
        }
    }

    @Nested
    class DeleteAndMigrate {

        @Test
        void delegatesToServiceAndReturns200() {
            final MigrateCategoryResponse migrateResponse = MigrateCategoryResponse.builder().migratedDebtCount(3).build();
            when(categoryService.deleteAndMigrate(CAT_ID, TARGET_CAT_ID)).thenReturn(migrateResponse);

            final ResponseEntity<MigrateCategoryResponse> response = controller.deleteAndMigrate(CAT_ID, TARGET_CAT_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getMigratedDebtCount()).isEqualTo(3);
            verify(categoryService).deleteAndMigrate(CAT_ID, TARGET_CAT_ID);
        }
    }

    @Nested
    class AddMember {

        @Test
        void delegatesToServiceAndReturns200() {
            final AddMemberRequest req = new AddMemberRequest();

            final ResponseEntity<Void> response = controller.addMember(CAT_ID, req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(categoryService).addMember(CAT_ID, req);
        }
    }

    @Nested
    class BulkAddMember {

        @Test
        void delegatesToServiceAndReturns200() {
            final BulkAddMemberRequest req = new BulkAddMemberRequest();

            final ResponseEntity<Void> response = controller.bulkAddMember(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(categoryService).bulkAddMember(req);
        }
    }

    @Nested
    class RemoveMember {

        @Test
        void delegatesToServiceAndReturns204() {
            final ResponseEntity<Void> response = controller.removeMember(CAT_ID, USER_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(categoryService).removeMember(CAT_ID, USER_ID);
        }
    }

    @Nested
    class GetAll {

        @Test
        void delegatesToServiceAndReturns200() {
            final List<GetAllCategoryResponse> categories = List.of(
                    GetAllCategoryResponse.builder().id(CAT_ID).name("Bills").build()
            );
            when(categoryService.getAll()).thenReturn(categories);

            final ResponseEntity<List<GetAllCategoryResponse>> response = controller.getAll();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getId()).isEqualTo(CAT_ID);
        }
    }
}
