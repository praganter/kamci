package dev.batuhanyetgin.authservice.repositoryservice;

import dev.batuhanyetgin.authservice.document.HouseholdDocument;
import dev.batuhanyetgin.authservice.enums.HouseholdStatus;
import dev.batuhanyetgin.authservice.repository.HouseholdRepository;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdRepositoryServiceTest {

    @Mock
    private HouseholdRepository householdRepository;

    private HouseholdRepositoryService service;

    private HouseholdDocument buildHousehold(final String id, final HouseholdStatus status) {
        return HouseholdDocument.builder().id(id).ownerId("owner1").partnerId("partner1").status(status).build();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new HouseholdRepositoryService(householdRepository);
    }

    @Test
    @DisplayName("save → delegates to repository")
    void save_delegatesToRepository() {
        final HouseholdDocument document = buildHousehold("h1", HouseholdStatus.PENDING);
        when(householdRepository.save(document)).thenReturn(document);

        assertThat(service.save(document)).isEqualTo(document);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("found → returns document")
        void found_returnsDocument() {
            final HouseholdDocument document = buildHousehold("h1", HouseholdStatus.PENDING);
            when(householdRepository.findById("h1")).thenReturn(Optional.of(document));

            assertThat(service.findById("h1")).isEqualTo(document);
        }

        @Test
        @DisplayName("not found → throws BaseException")
        void notFound_throwsBaseException() {
            when(householdRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById("missing")).isInstanceOf(BaseException.class);
        }
    }

    @Test
    @DisplayName("delete → delegates to repository")
    void delete_delegatesToRepository() {
        final HouseholdDocument document = buildHousehold("h1", HouseholdStatus.PENDING);

        service.delete(document);

        verify(householdRepository).delete(document);
    }

    @Test
    @DisplayName("findAllForUser → merges owner and partner matches")
    void findAllForUser_mergesOwnerAndPartnerMatches() {
        final HouseholdDocument asOwner = buildHousehold("h1", HouseholdStatus.ACCEPTED);
        final HouseholdDocument asPartner = buildHousehold("h2", HouseholdStatus.PENDING);
        when(householdRepository.findByOwnerId("user1")).thenReturn(List.of(asOwner));
        when(householdRepository.findByPartnerId("user1")).thenReturn(List.of(asPartner));

        assertThat(service.findAllForUser("user1")).containsExactly(asOwner, asPartner);
    }

    @Nested
    @DisplayName("findAcceptedForUser")
    class FindAcceptedForUser {

        @Test
        @DisplayName("only ACCEPTED household is returned, PENDING filtered out")
        void onlyAccepted_returned() {
            final HouseholdDocument pending = buildHousehold("h1", HouseholdStatus.PENDING);
            final HouseholdDocument accepted = buildHousehold("h2", HouseholdStatus.ACCEPTED);
            when(householdRepository.findByOwnerId("user1")).thenReturn(List.of(pending, accepted));
            when(householdRepository.findByPartnerId("user1")).thenReturn(List.of());

            assertThat(service.findAcceptedForUser("user1")).contains(accepted);
        }

        @Test
        @DisplayName("no accepted household → empty")
        void noAccepted_empty() {
            final HouseholdDocument pending = buildHousehold("h1", HouseholdStatus.PENDING);
            when(householdRepository.findByOwnerId("user1")).thenReturn(List.of(pending));
            when(householdRepository.findByPartnerId("user1")).thenReturn(List.of());

            assertThat(service.findAcceptedForUser("user1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveForUser")
    class FindActiveForUser {

        @Test
        @DisplayName("PENDING household counts as active")
        void pending_countsAsActive() {
            final HouseholdDocument pending = buildHousehold("h1", HouseholdStatus.PENDING);
            when(householdRepository.findByOwnerId("user1")).thenReturn(List.of(pending));
            when(householdRepository.findByPartnerId("user1")).thenReturn(List.of());

            assertThat(service.findActiveForUser("user1")).contains(pending);
        }

        @Test
        @DisplayName("household with null status does not count as active")
        void nullStatus_doesNotCountAsActive() {
            final HouseholdDocument noStatus = buildHousehold("h1", null);
            when(householdRepository.findByOwnerId("user1")).thenReturn(List.of(noStatus));
            when(householdRepository.findByPartnerId("user1")).thenReturn(List.of());

            assertThat(service.findActiveForUser("user1")).isEmpty();
        }
    }
}
