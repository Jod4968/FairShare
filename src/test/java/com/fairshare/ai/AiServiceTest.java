package com.fairshare.ai;

import com.fairshare.expense.*;
import com.fairshare.group.*;
import com.fairshare.settlement.*;
import com.fairshare.user.User;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiServiceTest {
    @Mock AiClient ai;
    @Mock GroupRepository groups;
    @Mock GroupMemberRepository members;
    @Mock ExpenseRepository expenses;
    @Mock ExpenseParticipantRepository participants;
    @Mock ExpenseService expenseService;
    @Mock SettlementService settlementService;
    private final User current = user(1L, "sagar@example.com", "Sagar");
    private final User rahul = user(2L, "rahul@example.com", "Rahul");
    private final Group group = group();

    @Test void createExpenseUsesAuthenticatedUserAndResolvedMembers() {
        AiService service = service(true);
        when(ai.interpret(anyString(), anyList())).thenReturn(new AiDtos.Intent(AiIntent.CREATE_EXPENSE, 2L,
                "dinner", ExpenseCategory.FOOD, SplitType.EQUAL, List.of("me", "Rahul"), null, null, null, "me"));
        AiDtos.MessageResponse response = service.message(10L, "I paid 900 rupees for dinner", current);
        assertTrue(response.success());
        ArgumentCaptor<ExpenseDtos.CreateExpenseRequest> request = ArgumentCaptor.forClass(ExpenseDtos.CreateExpenseRequest.class);
        verify(expenseService).create(eq(10L), request.capture(), same(current));
        assertEquals(90000L, request.getValue().amountMinor());
        assertEquals(List.of(1L, 2L), request.getValue().participantUserIds());
    }

    @Test void naturalLanguageAmountsAreConvertedToPersistedPaise() {
        assertAmount("I paid 200 rupees for dinner for me and Alexender", 20000L);
        assertAmount("I paid ₹200 for dinner for me and Alexender", 20000L);
        assertAmount("I paid 200rs for dinner for both of us", 20000L);
        assertAmount("I paid ₹125.50 for dinner", 12550L);
        assertAmount("I paid ₹900 for dinner", 90000L);
    }

    @Test void unknownAndMalformedIntentAreNotExecuted() {
        AiService service = service(true);
        when(ai.interpret(anyString(), anyList())).thenReturn(new AiDtos.Intent(AiIntent.UNKNOWN, null, null, null, null, null, null, null, null, null));
        assertFalse(service.message(10L, "nonsense", current).success());
        verifyNoInteractions(expenseService);
    }

    @Test void missingAmountIsRejected() {
        AiService service = service(true);
        when(ai.interpret(anyString(), anyList())).thenReturn(new AiDtos.Intent(AiIntent.CREATE_EXPENSE, null,
                "dinner", ExpenseCategory.FOOD, SplitType.EQUAL, List.of("me"), null, null, null, "me"));
        assertFalse(service.message(10L, "add dinner", current).success());
        verifyNoInteractions(expenseService);
    }

    @Test void unknownParticipantIsRejectedAndPayerCannotBeImpersonated() {
        AiService service = service(true);
        when(ai.interpret(eq("I paid 100 for unknown"), anyList())).thenReturn(new AiDtos.Intent(AiIntent.CREATE_EXPENSE, 100L,
                "x", ExpenseCategory.OTHER, SplitType.EQUAL, List.of("Nobody"), null, null, null, "me"));
        assertThrows(RuntimeException.class, () -> service.message(10L, "I paid 100 for unknown", current));
        when(ai.interpret(eq("impersonate"), anyList())).thenReturn(new AiDtos.Intent(AiIntent.CREATE_EXPENSE, 100L,
                "x", ExpenseCategory.OTHER, SplitType.EQUAL, List.of("me"), null, null, null, "Rahul"));
        assertFalse(service.message(10L, "impersonate", current).success());
        verifyNoInteractions(expenseService);
    }

    @Test void nonMemberAndUnavailableAssistantAreHandled() {
        AiService nonMemberService = service(true);
        when(members.existsById(new GroupMemberId(10L, 1L))).thenReturn(false);
        assertThrows(com.fairshare.auth.UnauthorizedException.class, () -> nonMemberService.message(10L, "hi", current));
        when(members.existsById(new GroupMemberId(10L, 1L))).thenReturn(true);
        assertThrows(AiUnavailableException.class, () -> service(false).message(10L, "hi", current));
    }

    private AiService service(boolean enabled) {
        when(groups.findById(10L)).thenReturn(Optional.of(group));
        when(members.existsById(new GroupMemberId(10L, 1L))).thenReturn(true);
        when(members.findByGroupIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(new GroupMember(group, current), new GroupMember(group, rahul)));
        return new AiService(ai, groups, members, expenses, expenseService, settlementService, enabled);
    }
    private void assertAmount(String text, long expectedMinor) {
        AiService service = service(true);
        clearInvocations(expenseService);
        when(ai.interpret(eq(text), anyList())).thenReturn(new AiDtos.Intent(AiIntent.CREATE_EXPENSE, 2L,
                "dinner", ExpenseCategory.FOOD, SplitType.EQUAL, List.of("me", "Rahul"), null, null, null, "me"));
        assertTrue(service.message(10L, text, current).success());
        ArgumentCaptor<ExpenseDtos.CreateExpenseRequest> request = ArgumentCaptor.forClass(ExpenseDtos.CreateExpenseRequest.class);
        verify(expenseService).create(eq(10L), request.capture(), same(current));
        assertEquals(expectedMinor, request.getValue().amountMinor());
    }
    private static User user(Long id, String email, String name) { User user = new User(email, "hash", name); setId(user, id); return user; }
    private static Group group() { Group group = new Group("Flat", "CODE123456", user(1L, "sagar@example.com", "Sagar")); setId(group, 10L); return group; }
    private static void setId(Object object, Long id) {
        try { Field field = object instanceof Group ? Group.class.getDeclaredField("id") : User.class.getDeclaredField("id"); field.setAccessible(true); field.set(object, id); }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
}
