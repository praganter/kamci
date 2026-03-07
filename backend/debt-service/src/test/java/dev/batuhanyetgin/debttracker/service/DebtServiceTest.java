package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.SharedWithEmbedded;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.DebtResponse;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import dev.batuhanyetgin.debttracker.mapper.DebtMapper;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtServiceTest {

    private static final String TEST_USER_ID = "test-user-id";
    private static final String ANOTHER_USER_ID = "another-user-id";
    private static final String DEBT_ID = "debt-id";

    @InjectMocks
    private DebtService debtService;

    @Mock
    private DebtRepositoryService debtRS;

    @Mock
    private DebtMapper debtMapper;

    @BeforeEach
    void setUp() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("user-id", TEST_USER_ID);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void create_ShouldReturnDebtResponseAndSetOwnerId_WhenRequestIsValid() {
        LocalDateTime now = LocalDateTime.now();
        CreateDebtRequest request = CreateDebtRequest.builder()
                .name("Test Debt")
                .startDate(now)
                .paymentPeriodType(PaymentPeriodType.MONTHLY)
                .amount(BigDecimal.TEN)
                .build();

        DebtDocument mappedDocument = new DebtDocument();
        mappedDocument.setName("Test Debt");

        DebtDocument savedDocument = DebtDocument.builder()
                .id(DEBT_ID)
                .amount(BigDecimal.TEN)
                .startDate(now)
                .name("Test Debt")
                .ownerId(TEST_USER_ID)
                .build();

        when(debtMapper.convert(request)).thenReturn(mappedDocument);
        when(debtRS.save(any(DebtDocument.class))).thenReturn(savedDocument);

        DebtResponse response = debtService.create(request);

        assertNotNull(response);
        assertEquals(DEBT_ID, response.getDebt().getId());
        assertEquals(TEST_USER_ID, request.getOwnerId());

        ArgumentCaptor<DebtDocument> documentCaptor = ArgumentCaptor.forClass(DebtDocument.class);
        verify(debtMapper).convert(request);
        verify(debtRS).save(documentCaptor.capture());

        verifyNoMoreInteractions(debtMapper, debtRS);
    }

    @Test
    void update_ShouldReturnDebtResponse_WhenUserIsOwner() {
        LocalDateTime now = LocalDateTime.now();
        UpdateDebtRequest request = UpdateDebtRequest.builder()
                .id(DEBT_ID)
                .name("Updated Debt")
                .startDate(now)
                .paymentPeriodType(PaymentPeriodType.MONTHLY)
                .amount(BigDecimal.valueOf(200.0))
                .build();

        DebtDocument existingDebt = DebtDocument.builder()
                .id(DEBT_ID)
                .ownerId(TEST_USER_ID)
                .name("Original Debt")
                .amount(BigDecimal.valueOf(100.0))
                .build();

        DebtDocument mappedDocument = DebtDocument.builder()
                .id(DEBT_ID)
                .name("Updated Debt")
                .amount(BigDecimal.valueOf(200.0))
                .build();

        DebtDocument updatedDocument = DebtDocument.builder()
                .id(DEBT_ID)
                .ownerId(TEST_USER_ID)
                .name("Updated Debt")
                .amount(BigDecimal.valueOf(200.0))
                .build();

        when(debtRS.findById(DEBT_ID)).thenReturn(existingDebt);
        when(debtMapper.convert(request)).thenReturn(mappedDocument);
        when(debtRS.update(any(DebtDocument.class))).thenReturn(updatedDocument);

        DebtResponse response = debtService.update(request);

        assertNotNull(response);
        assertEquals(DEBT_ID, response.getDebt().getId());
        assertEquals("Updated Debt", response.getDebt().getName());
        assertEquals(TEST_USER_ID, request.getOwnerId());

        verify(debtRS).findById(DEBT_ID);
        verify(debtMapper).convert(request);
        verify(debtRS).update(mappedDocument);
        verifyNoMoreInteractions(debtMapper, debtRS);
    }

    @Test
    void update_ShouldReturnDebtResponse_WhenUserIsSharedWithEditPermission() {
        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            mockedStatic.when(RequestContextUtil::getUserId).thenReturn(ANOTHER_USER_ID);

            LocalDateTime now = LocalDateTime.now();
            UpdateDebtRequest request = UpdateDebtRequest.builder()
                    .id(DEBT_ID)
                    .name("Updated Debt by Shared User")
                    .startDate(now)
                    .paymentPeriodType(PaymentPeriodType.MONTHLY)
                    .amount(BigDecimal.valueOf(300.0))
                    .build();

            SharedWithEmbedded sharedUser = SharedWithEmbedded.builder()
                    .userId(ANOTHER_USER_ID)
                    .accessLevelType(AccessLevelType.EDIT)
                    .build();

            DebtDocument existingDebt = DebtDocument.builder()
                    .id(DEBT_ID)
                    .ownerId(TEST_USER_ID)
                    .name("Original Debt")
                    .amount(BigDecimal.valueOf(100.0))
                    .sharedWith(Collections.singletonList(sharedUser))
                    .build();

            DebtDocument mappedDocument = DebtDocument.builder()
                    .id(DEBT_ID)
                    .name("Updated Debt by Shared User")
                    .amount(BigDecimal.valueOf(300.0))
                    .build();

            DebtDocument updatedDocument = DebtDocument.builder()
                    .id(DEBT_ID)
                    .ownerId(TEST_USER_ID)
                    .name("Updated Debt by Shared User")
                    .amount(BigDecimal.valueOf(300.0))
                    .build();

            when(debtRS.findById(DEBT_ID)).thenReturn(existingDebt);
            when(debtMapper.convert(request)).thenReturn(mappedDocument);
            when(debtRS.update(any(DebtDocument.class))).thenReturn(updatedDocument);

            DebtResponse response = debtService.update(request);

            assertNotNull(response);
            assertEquals(DEBT_ID, response.getDebt().getId());
            assertEquals("Updated Debt by Shared User", response.getDebt().getName());
            assertEquals(TEST_USER_ID, request.getOwnerId());

            verify(debtRS).findById(DEBT_ID);
            verify(debtMapper).convert(request);
            verify(debtRS).update(mappedDocument);
            verifyNoMoreInteractions(debtMapper, debtRS);
        }
    }

    @Test
    void update_ShouldThrowBaseException_WhenPermissionDenied() {
        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            mockedStatic.when(RequestContextUtil::getUserId).thenReturn(ANOTHER_USER_ID);

            UpdateDebtRequest request = UpdateDebtRequest.builder()
                    .id(DEBT_ID)
                    .name("Unauthorized Update")
                    .amount(BigDecimal.valueOf(500.0))
                    .build();

            DebtDocument existingDebt = DebtDocument.builder()
                    .id(DEBT_ID)
                    .ownerId(TEST_USER_ID)
                    .name("Original Debt")
                    .amount(BigDecimal.valueOf(100.0))
                    .sharedWith(Collections.emptyList())
                    .build();

            when(debtRS.findById(DEBT_ID)).thenReturn(existingDebt);

            BaseException exception = assertThrows(BaseException.class, () -> debtService.update(request));
            assertEquals("error.debt.permission_denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            verify(debtRS).findById(DEBT_ID);
            verifyNoInteractions(debtMapper);
            verifyNoMoreInteractions(debtRS);
        }
    }

    @Test
    void update_ShouldThrowBaseException_WhenSharedWithReadPermissionOnly() {
        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            mockedStatic.when(RequestContextUtil::getUserId).thenReturn(ANOTHER_USER_ID);

            UpdateDebtRequest request = UpdateDebtRequest.builder()
                    .id(DEBT_ID)
                    .name("Unauthorized Update")
                    .amount(BigDecimal.valueOf(500.0))
                    .build();

            SharedWithEmbedded sharedUser = SharedWithEmbedded.builder()
                    .userId(ANOTHER_USER_ID)
                    .accessLevelType(AccessLevelType.READ)
                    .build();

            DebtDocument existingDebt = DebtDocument.builder()
                    .id(DEBT_ID)
                    .ownerId(TEST_USER_ID)
                    .name("Original Debt")
                    .amount(BigDecimal.valueOf(100.0))
                    .sharedWith(Collections.singletonList(sharedUser))
                    .build();

            when(debtRS.findById(DEBT_ID)).thenReturn(existingDebt);

            BaseException exception = assertThrows(BaseException.class, () -> debtService.update(request));
            assertEquals("error.debt.permission_denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            verify(debtRS).findById(DEBT_ID);
            verifyNoInteractions(debtMapper);
            verifyNoMoreInteractions(debtRS);
        }
    }

    @Test
    void delete_ShouldInvokeRepositoryService_WhenUserIsOwner() {
        DebtDocument existingDebt = DebtDocument.builder()
                .id(DEBT_ID)
                .ownerId(TEST_USER_ID)
                .name("Original Debt")
                .build();

        when(debtRS.findById(DEBT_ID)).thenReturn(existingDebt);
        doNothing().when(debtRS).delete(DEBT_ID);

        debtService.delete(DEBT_ID);

        verify(debtRS).findById(DEBT_ID);
        verify(debtRS).delete(DEBT_ID);
        verifyNoInteractions(debtMapper);
        verifyNoMoreInteractions(debtRS);
    }

    @Test
    void delete_ShouldInvokeRepositoryService_WhenUserIsSharedWithFullPermission() {
        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            mockedStatic.when(RequestContextUtil::getUserId).thenReturn(ANOTHER_USER_ID);

            SharedWithEmbedded sharedUser = SharedWithEmbedded.builder()
                    .userId(ANOTHER_USER_ID)
                    .accessLevelType(AccessLevelType.FULL)
                    .build();

            DebtDocument existingDebt = DebtDocument.builder()
                    .id(DEBT_ID)
                    .ownerId(TEST_USER_ID)
                    .name("Original Debt")
                    .sharedWith(Collections.singletonList(sharedUser))
                    .build();

            when(debtRS.findById(DEBT_ID)).thenReturn(existingDebt);
            doNothing().when(debtRS).delete(DEBT_ID);

            debtService.delete(DEBT_ID);

            verify(debtRS).findById(DEBT_ID);
            verify(debtRS).delete(DEBT_ID);
            verifyNoInteractions(debtMapper);
            verifyNoMoreInteractions(debtRS);
        }
    }

    @Test
    void delete_ShouldThrowBaseException_WhenPermissionDenied() {
        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            mockedStatic.when(RequestContextUtil::getUserId).thenReturn(ANOTHER_USER_ID);

            DebtDocument existingDebt = DebtDocument.builder()
                    .id(DEBT_ID)
                    .ownerId(TEST_USER_ID)
                    .name("Original Debt")
                    .sharedWith(Collections.emptyList())
                    .build();

            when(debtRS.findById(DEBT_ID)).thenReturn(existingDebt);

            BaseException exception = assertThrows(BaseException.class, () -> debtService.delete(DEBT_ID));
            assertEquals("error.debt.permission_denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            verify(debtRS).findById(DEBT_ID);
            verifyNoMoreInteractions(debtRS);
            verifyNoInteractions(debtMapper);
        }
    }

    @Test
    void delete_ShouldThrowBaseException_WhenSharedWithReadPermissionOnly() {
        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            mockedStatic.when(RequestContextUtil::getUserId).thenReturn(ANOTHER_USER_ID);

            SharedWithEmbedded sharedUser = SharedWithEmbedded.builder()
                    .userId(ANOTHER_USER_ID)
                    .accessLevelType(AccessLevelType.READ)
                    .build();

            DebtDocument existingDebt = DebtDocument.builder()
                    .id(DEBT_ID)
                    .ownerId(TEST_USER_ID)
                    .name("Original Debt")
                    .sharedWith(Collections.singletonList(sharedUser))
                    .build();

            when(debtRS.findById(DEBT_ID)).thenReturn(existingDebt);

            BaseException exception = assertThrows(BaseException.class, () -> debtService.delete(DEBT_ID));
            assertEquals("error.debt.permission_denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            verify(debtRS).findById(DEBT_ID);
            verifyNoMoreInteractions(debtRS);
            verifyNoInteractions(debtMapper);
        }
    }
}