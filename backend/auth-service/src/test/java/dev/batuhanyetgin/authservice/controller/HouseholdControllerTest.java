package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.dto.request.InviteHouseholdRequest;
import dev.batuhanyetgin.authservice.dto.response.HouseholdResponse;
import dev.batuhanyetgin.authservice.enums.HouseholdStatus;
import dev.batuhanyetgin.authservice.service.HouseholdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdControllerTest {

    @Mock
    private HouseholdService householdService;

    private HouseholdController controller;

    @BeforeEach
    void setUp() {
        controller = new HouseholdController(householdService);
    }

    private HouseholdResponse buildResponse() {
        return HouseholdResponse.builder()
                .id("household1").ownerId("owner1").partnerId("partner1")
                .status(HouseholdStatus.ACCEPTED).owner(true).build();
    }

    @Test
    @DisplayName("invite → delegates to service and returns 200 with response")
    void invite_returnsOk() {
        final InviteHouseholdRequest request = new InviteHouseholdRequest();
        request.setEmail("partner@example.com");
        when(householdService.invite(request)).thenReturn(buildResponse());

        final ResponseEntity<HouseholdResponse> response = controller.invite(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo("household1");
    }

    @Test
    @DisplayName("accept → delegates to service and returns 200 with response")
    void accept_returnsOk() {
        when(householdService.accept("household1")).thenReturn(buildResponse());

        final ResponseEntity<HouseholdResponse> response = controller.accept("household1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(householdService).accept("household1");
    }

    @Test
    @DisplayName("decline → delegates to service and returns 204 no content")
    void decline_returnsNoContent() {
        final ResponseEntity<Void> response = controller.decline("household1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(householdService).decline("household1");
    }

    @Test
    @DisplayName("leave → delegates to service and returns 204 no content")
    void leave_returnsNoContent() {
        final ResponseEntity<Void> response = controller.leave();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(householdService).leave();
    }

    @Nested
    @DisplayName("getCurrent")
    class GetCurrent {

        @Test
        @DisplayName("household present → 200 with response")
        void present_returnsOk() {
            when(householdService.getCurrent()).thenReturn(Optional.of(buildResponse()));

            final ResponseEntity<HouseholdResponse> response = controller.getCurrent();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getId()).isEqualTo("household1");
        }

        @Test
        @DisplayName("no household → 204 no content")
        void absent_returnsNoContent() {
            when(householdService.getCurrent()).thenReturn(Optional.empty());

            final ResponseEntity<HouseholdResponse> response = controller.getCurrent();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }
}
