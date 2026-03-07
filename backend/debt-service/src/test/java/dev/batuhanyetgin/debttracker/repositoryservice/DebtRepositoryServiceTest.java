package dev.batuhanyetgin.debttracker.repositoryservice;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.repository.DebtRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtRepositoryServiceTest {

    private static final String TEST_ID = "test-id";

    @InjectMocks
    private DebtRepositoryService debtRepositoryService;

    @Mock
    private DebtRepository debtRepository;

    @Test
    void findById_ShouldReturnDebtDocument_WhenIdExists() {
        final DebtDocument expectedDocument = DebtDocument.builder().id(TEST_ID).build();
        when(debtRepository.findById(TEST_ID)).thenReturn(Optional.of(expectedDocument));

        final DebtDocument actualDocument = debtRepositoryService.findById(TEST_ID);

        assertNotNull(actualDocument);
        assertEquals(TEST_ID, actualDocument.getId());
        verify(debtRepository).findById(TEST_ID);
        verifyNoMoreInteractions(debtRepository);
    }

    @Test
    void findById_ShouldThrowBaseException_WhenIdDoesNotExist() {
        when(debtRepository.findById(TEST_ID)).thenReturn(Optional.empty());

        final BaseException exception = assertThrows(BaseException.class, () -> debtRepositoryService.findById(TEST_ID));

        assertEquals("error.debt.not_found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(debtRepository).findById(TEST_ID);
        verifyNoMoreInteractions(debtRepository);
    }

    @Test
    void save_ShouldReturnSavedDocument_WhenCalled() {
        final DebtDocument documentToSave = DebtDocument.builder().name("New Debt").build();
        final DebtDocument savedDocument = DebtDocument.builder().id(TEST_ID).name("New Debt").build();
        when(debtRepository.save(documentToSave)).thenReturn(savedDocument);

        final DebtDocument result = debtRepositoryService.save(documentToSave);

        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        verify(debtRepository).save(documentToSave);
        verifyNoMoreInteractions(debtRepository);
    }

    @Test
    void update_ShouldReturnUpdatedDocument_WhenCalled() {
        final DebtDocument documentToUpdate = DebtDocument.builder().id(TEST_ID).name("Updated Name").build();
        when(debtRepository.save(documentToUpdate)).thenReturn(documentToUpdate);

        final DebtDocument result = debtRepositoryService.update(documentToUpdate);

        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        verify(debtRepository).save(documentToUpdate);
        verifyNoMoreInteractions(debtRepository);
    }

    @Test
    void delete_ShouldInvokeRepositoryDeleteById_WhenCalled() {
        doNothing().when(debtRepository).deleteById(TEST_ID);

        debtRepositoryService.delete(TEST_ID);

        verify(debtRepository).deleteById(TEST_ID);
        verifyNoMoreInteractions(debtRepository);
    }
}