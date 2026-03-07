package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.CategoryEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.SharedWithEmbedded;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebtMapperTest {

    private DebtMapper debtMapper;

    @BeforeEach
    void setUp() {
        debtMapper = new DebtMapper();
    }

    @Test
    void convert_ShouldMapCreateDebtRequestToDebtDocument_WhenRequestIsValid() {
        final LocalDateTime now = LocalDateTime.now();
        final CategoryEmbedded category = CategoryEmbedded.builder()
                .name("Food")
                .build();

        final SharedWithEmbedded sharedUser = SharedWithEmbedded.builder()
                .userId("shared-user-1")
                .accessLevelType(AccessLevelType.EDIT)
                .build();

        final CreateDebtRequest request = CreateDebtRequest.builder()
                .name("Test Debt")
                .description("Test Description")
                .ownerId("user-123")
                .amount(BigDecimal.TEN)
                .startDate(now)
                .paymentPeriodType(PaymentPeriodType.MONTHLY)
                .category(category)
                .sharedWith(List.of(sharedUser))
                .build();

        final DebtDocument result = debtMapper.convert(request);

        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getDescription(), result.getDescription());
        assertEquals(request.getOwnerId(), result.getOwnerId());
        assertEquals(request.getAmount(), result.getAmount());
        assertEquals(request.getStartDate(), result.getStartDate());
        assertEquals(request.getPaymentPeriodType(), result.getPaymentPeriodType());
        assertEquals(request.getCategory().getName(), result.getCategory().getName());

        assertNotNull(result.getSharedWith());
        assertEquals(1, result.getSharedWith().size());
        assertEquals(sharedUser.getUserId(), result.getSharedWith().get(0).getUserId());
        assertEquals(sharedUser.getAccessLevelType(), result.getSharedWith().get(0).getAccessLevelType());
    }

    @Test
    void convert_ShouldMapUpdateDebtRequestToDebtDocument_WhenRequestIsValid() {
        final LocalDateTime now = LocalDateTime.now();
        final CategoryEmbedded category = CategoryEmbedded.builder()
                .name("Bills")
                .build();

        final SharedWithEmbedded sharedUser = SharedWithEmbedded.builder()
                .userId("shared-user-2")
                .accessLevelType(AccessLevelType.FULL)
                .build();

        final UpdateDebtRequest request = UpdateDebtRequest.builder()
                .id("debt-123")
                .name("Updated Debt")
                .description("Updated Description")
                .amount(BigDecimal.valueOf(100.0))
                .startDate(now)
                .paymentPeriodType(PaymentPeriodType.YEARLY)
                .category(category)
                .sharedWith(List.of(sharedUser))
                .build();

        final DebtDocument result = debtMapper.convert(request);

        assertNotNull(result);
        assertEquals(request.getId(), result.getId());
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getDescription(), result.getDescription());
        assertEquals(request.getAmount(), result.getAmount());
        assertEquals(request.getStartDate(), result.getStartDate());
        assertEquals(request.getPaymentPeriodType(), result.getPaymentPeriodType());
        assertEquals(request.getCategory().getName(), result.getCategory().getName());

        assertNotNull(result.getSharedWith());
        assertEquals(1, result.getSharedWith().size());
        assertEquals(sharedUser.getUserId(), result.getSharedWith().get(0).getUserId());
        assertEquals(sharedUser.getAccessLevelType(), result.getSharedWith().get(0).getAccessLevelType());
    }

    @Test
    void convert_ShouldReturnNull_WhenCreateRequestIsNull() {
        final CreateDebtRequest request = null;
        final DebtDocument result = debtMapper.convert(request);
        assertNull(result);
    }

    @Test
    void convert_ShouldReturnNull_WhenUpdateRequestIsNull() {
        final UpdateDebtRequest request = null;
        final DebtDocument result = debtMapper.convert(request);
        assertNull(result);
    }

    @Test
    void convert_ShouldHandleEmptySharedWithList_WhenCreateRequestHasEmptyList() {
        final CreateDebtRequest request = CreateDebtRequest.builder()
                .sharedWith(Collections.emptyList())
                .category(CategoryEmbedded.builder().name("Test").build())
                .startDate(LocalDateTime.now())
                .paymentPeriodType(PaymentPeriodType.MONTHLY)
                .amount(BigDecimal.ONE)
                .build();

        final DebtDocument result = debtMapper.convert(request);

        assertNotNull(result);
        assertNotNull(result.getSharedWith());
        assertTrue(result.getSharedWith().isEmpty());
    }
}