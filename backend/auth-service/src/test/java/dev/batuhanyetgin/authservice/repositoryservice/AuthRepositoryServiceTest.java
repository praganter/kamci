package dev.batuhanyetgin.authservice.repositoryservice;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.repository.AuthRepository;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
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
class AuthRepositoryServiceTest {

    @InjectMocks
    private AuthRepositoryService authRepositoryService;

    @Mock
    private AuthRepository authRepository;

    @Test
    void findByEMail_ShouldReturnAuthDocument_WhenEmailExists() {
        final String email = "test@test.com";
        final AuthDocument expectedDocument = AuthDocument.builder().email(email).build();
        when(authRepository.findByEmail(email)).thenReturn(Optional.of(expectedDocument));

        final AuthDocument actualDocument = authRepositoryService.findByEMail(email);

        assertNotNull(actualDocument);
        assertEquals(email, actualDocument.getEmail());
        verify(authRepository).findByEmail(email);
        verifyNoMoreInteractions(authRepository);
    }

    @Test
    void findByEMail_ShouldThrowBaseException_WhenEmailDoesNotExist() {
        final String email = "nonexistent@test.com";
        when(authRepository.findByEmail(email)).thenReturn(Optional.empty());

        final BaseException exception = assertThrows(BaseException.class, () -> authRepositoryService.findByEMail(email));

        assertEquals("error.auth.user_not_found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(authRepository).findByEmail(email);
        verifyNoMoreInteractions(authRepository);
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        final String email = "test@test.com";
        when(authRepository.existsByEmail(email)).thenReturn(true);

        final boolean exists = authRepositoryService.existsByEmail(email);

        assertTrue(exists);
        verify(authRepository).existsByEmail(email);
        verifyNoMoreInteractions(authRepository);
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenEmailDoesNotExist() {
        final String email = "test@test.com";
        when(authRepository.existsByEmail(email)).thenReturn(false);

        final boolean exists = authRepositoryService.existsByEmail(email);

        assertFalse(exists);
        verify(authRepository).existsByEmail(email);
        verifyNoMoreInteractions(authRepository);
    }
}