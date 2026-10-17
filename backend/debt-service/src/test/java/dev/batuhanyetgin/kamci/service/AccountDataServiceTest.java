package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import dev.batuhanyetgin.kamci.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.UserIncomeRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountDataServiceTest {

    @Mock private CategoryRepositoryService categoryRS;
    @Mock private DebtRepositoryService debtRS;
    @Mock private UserIncomeRepositoryService userIncomeRS;

    private AccountDataService accountDataService;

    private static final String USER_ID = "user123";

    @BeforeEach
    void setUp() {
        accountDataService = new AccountDataService(categoryRS, debtRS, userIncomeRS);
    }

    private CategoryDocument ownedCategory(final String id) {
        return CategoryDocument.builder().id(id).ownerId(USER_ID).name("Owned").build();
    }

    private CategoryDocument categoryWithMembership(final String id, final String ownerId) {
        final List<MemberEmbedded> members = new ArrayList<>();
        members.add(MemberEmbedded.builder().userId(USER_ID).email("x@test.com").accessLevel(AccessLevelType.EDIT).build());
        members.add(MemberEmbedded.builder().userId("otherUser").email("y@test.com").accessLevel(AccessLevelType.EDIT).build());
        return CategoryDocument.builder().id(id).ownerId(ownerId).name("Shared").members(members).build();
    }

    @Test
    @DisplayName("deletes debts and categories owned by the user")
    void deletesOwnedCategoriesAndTheirDebts() {
        final CategoryDocument owned = ownedCategory("cat1");
        when(categoryRS.findByOwnerId(USER_ID)).thenReturn(List.of(owned));
        when(categoryRS.findByMemberUserId(USER_ID)).thenReturn(List.of());

        accountDataService.deleteUserData(USER_ID);

        verify(debtRS).deleteByCategoryId("cat1");
        verify(categoryRS).delete("cat1");
        verify(userIncomeRS).deleteByOwnerId(USER_ID);
    }

    @Test
    @DisplayName("removes the user as a member from categories owned by someone else, keeps other members")
    void removesMembershipFromSharedCategories() {
        final CategoryDocument shared = categoryWithMembership("cat2", "otherOwner");
        when(categoryRS.findByOwnerId(USER_ID)).thenReturn(List.of());
        when(categoryRS.findByMemberUserId(USER_ID)).thenReturn(List.of(shared));

        accountDataService.deleteUserData(USER_ID);

        verify(categoryRS, never()).delete("cat2");
        final ArgumentCaptor<CategoryDocument> captor = ArgumentCaptor.forClass(CategoryDocument.class);
        verify(categoryRS).save(captor.capture());
        assertThat(captor.getValue().getMembers()).extracting(MemberEmbedded::getUserId).containsExactly("otherUser");
    }

    @Test
    @DisplayName("user with no data → no-op, still clears income")
    void noData_isNoOp() {
        when(categoryRS.findByOwnerId(USER_ID)).thenReturn(List.of());
        when(categoryRS.findByMemberUserId(USER_ID)).thenReturn(List.of());

        accountDataService.deleteUserData(USER_ID);

        verify(debtRS, never()).deleteByCategoryId(any());
        verify(categoryRS, never()).delete(any());
        verify(userIncomeRS).deleteByOwnerId(USER_ID);
    }
}
