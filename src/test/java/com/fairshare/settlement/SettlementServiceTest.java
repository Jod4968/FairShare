package com.fairshare.settlement;

import com.fairshare.auth.*;
import com.fairshare.expense.*;
import com.fairshare.group.*;
import com.fairshare.user.User;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettlementServiceTest {
    @Mock GroupRepository groups;
    @Mock GroupMemberRepository members;
    @Mock ExpenseRepository expenses;
    @Mock ExpenseParticipantRepository participants;
    @Mock SettlementRepository settlements;
    private final User alice = user(1L, "alice@example.com", "Alice");
    private final User bob = user(2L, "bob@example.com", "Bob");
    private final User charlie = user(3L, "charlie@example.com", "Charlie");
    private final Group group = group(10L);
    private SettlementService service;

    @BeforeEach void setUp() {
        service = new SettlementService(groups, members, expenses, participants, settlements);
        when(groups.findById(10L)).thenReturn(Optional.of(group));
        when(members.existsById(any())).thenReturn(true);
        when(members.findByGroupIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(
                new GroupMember(group, alice), new GroupMember(group, bob), new GroupMember(group, charlie)));
        when(expenses.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(expense(100L, alice, 900L)));
        when(participants.findByExpenseIdOrderByUserIdAsc(100L)).thenReturn(List.of(
                participant(100L, alice, 300L), participant(100L, bob, 300L), participant(100L, charlie, 300L)));
        when(settlements.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
    }

    @Test void balancesUseExpensePaidAndOwedAmounts() {
        List<SettlementDtos.BalanceResponse> balances = service.balances(10L, alice);
        assertEquals(List.of(600L, -300L, -300L), balances.stream().map(SettlementDtos.BalanceResponse::balanceMinor).toList());
        assertEquals(0L, balances.stream().mapToLong(SettlementDtos.BalanceResponse::balanceMinor).sum());
    }

    @Test void suggestionsUseDeterministicGreedyOrdering() {
        List<SettlementDtos.SuggestionResponse> suggestions = service.suggestions(10L, alice);
        assertEquals(2, suggestions.size());
        assertEquals(2L, suggestions.get(0).fromUserId());
        assertEquals(300L, suggestions.get(0).amountMinor());
        assertEquals(3L, suggestions.get(1).fromUserId());
        assertEquals(1L, suggestions.get(1).toUserId());
    }

    @Test void completedSettlementReducesOutstandingBalance() {
        Settlement completed = new Settlement(group, bob, alice, 300L);
        completed.complete();
        when(settlements.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(completed));
        List<SettlementDtos.BalanceResponse> balances = service.balances(10L, alice);
        assertEquals(List.of(300L, 0L, -300L), balances.stream().map(SettlementDtos.BalanceResponse::balanceMinor).toList());
        assertEquals(List.of(3L), service.suggestions(10L, alice).stream().map(SettlementDtos.SuggestionResponse::fromUserId).toList());
    }

    @Test void pendingSettlementDoesNotChangeBalances() {
        when(settlements.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(new Settlement(group, bob, alice, 300L)));
        assertEquals(-300L, service.balances(10L, alice).get(1).balanceMinor());
    }

    @Test void nonMemberCannotReadBalancesOrCreateSettlement() {
        User outsider = user(99L, "outside@example.com", "Outside");
        when(members.existsById(new GroupMemberId(10L, 99L))).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> service.balances(10L, outsider));
        assertThrows(UnauthorizedException.class, () -> service.create(10L, new SettlementDtos.CreateSettlementRequest(1L, 100L), outsider));
    }

    @Test void selfAndOutsideRecipientAreRejected() {
        assertThrows(InvalidRequestException.class, () -> service.create(10L, new SettlementDtos.CreateSettlementRequest(1L, 100L), alice));
        assertThrows(UnauthorizedException.class, () -> service.create(10L, new SettlementDtos.CreateSettlementRequest(99L, 100L), alice));
    }

    @Test void payerCanCreateAndCompleteSettlementOnlyOnce() {
        Settlement settlement = new Settlement(group, bob, alice, 300L);
        setId(settlement, 55L);
        when(settlements.save(any(Settlement.class))).thenReturn(settlement);
        when(settlements.findByIdAndGroupId(55L, 10L)).thenReturn(Optional.of(settlement));
        SettlementDtos.SettlementResponse created = service.create(10L, new SettlementDtos.CreateSettlementRequest(1L, 300L), bob);
        assertEquals(2L, created.fromUserId());
        assertEquals("PENDING", created.status());
        SettlementDtos.SettlementResponse completed = service.complete(10L, 55L, alice);
        assertEquals("COMPLETED", completed.status());
        assertNotNull(completed.completedAt());
        assertThrows(ConflictException.class, () -> service.complete(10L, 55L, alice));
    }

    private static Expense expense(Long id, User payer, Long amount) {
        Expense expense = new Expense(group(10L), payer, amount, "Dinner", ExpenseCategory.FOOD, SplitType.EQUAL);
        setId(expense, id);
        return expense;
    }
    private static ExpenseParticipant participant(Long expenseId, User user, Long share) {
        Expense expense = expense(expenseId, user(1L, "alice@example.com", "Alice"), 900L);
        return new ExpenseParticipant(expense, user, share);
    }
    private static User user(Long id, String email, String name) {
        User user = new User(email, "hash", name); setId(user, id); return user;
    }
    private static Group group(Long id) {
        Group group = new Group("Flat", "CODE123456", user(1L, "alice@example.com", "Alice")); setId(group, id); return group;
    }
    private static void setId(Object entity, Long id) {
        try {
            Field field = entity instanceof Settlement ? Settlement.class.getDeclaredField("id")
                    : entity instanceof Expense ? Expense.class.getDeclaredField("id")
                    : entity instanceof Group ? Group.class.getDeclaredField("id") : User.class.getDeclaredField("id");
            field.setAccessible(true); field.set(entity, id);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
}
