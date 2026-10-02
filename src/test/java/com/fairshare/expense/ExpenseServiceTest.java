package com.fairshare.expense;

import com.fairshare.auth.*;
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
class ExpenseServiceTest {
    @Mock ExpenseRepository expenses;
    @Mock ExpenseParticipantRepository participants;
    @Mock GroupRepository groups;
    @Mock GroupMemberRepository members;
    private final User payer = user(1L, "payer@example.com", "Payer");
    private final User second = user(2L, "second@example.com", "Second");
    private final User third = user(3L, "third@example.com", "Third");
    private final Group group = group(10L);
    private ExpenseService service;

    @BeforeEach void setUp() {
        service = new ExpenseService(expenses, participants, groups, members);
        when(groups.findById(10L)).thenReturn(Optional.of(group));
        when(members.existsById(new GroupMemberId(10L, 1L))).thenReturn(true);
        when(members.findByGroupIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(
                new GroupMember(group, payer), new GroupMember(group, second), new GroupMember(group, third)));
        when(expenses.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            setId(expense, 50L);
            return expense;
        });
        when(participants.findByExpenseIdOrderByUserIdAsc(50L)).thenReturn(List.of());
    }

    @Test void equalSplitExactDivision() {
        ExpenseDtos.ExpenseResponse response = create(900L, List.of(1L, 2L, 3L));
        assertEquals(900L, savedShares().stream().mapToLong(Long::longValue).sum());
        assertEquals(List.of(300L, 300L, 300L), savedShares());
        assertEquals(SplitType.EQUAL, response.splitType());
    }

    @Test void equalSplitDistributesRemainderToEarliestParticipants() {
        create(1000L, List.of(1L, 2L, 3L));
        assertEquals(List.of(334L, 333L, 333L), savedShares());
    }

    @Test void customSplitAcceptsExactPositiveShares() {
        ExpenseDtos.CreateExpenseRequest request = custom(1000L, List.of(
                new ExpenseDtos.CustomParticipantRequest(1L, 400L),
                new ExpenseDtos.CustomParticipantRequest(2L, 350L),
                new ExpenseDtos.CustomParticipantRequest(3L, 250L)));
        service.create(10L, request, payer);
        assertEquals(List.of(400L, 350L, 250L), savedShares());
    }

    @Test void customSplitRejectsIncorrectSum() {
        assertThrows(ConflictException.class, () -> service.create(10L, custom(1000L, List.of(
                new ExpenseDtos.CustomParticipantRequest(1L, 400L),
                new ExpenseDtos.CustomParticipantRequest(2L, 350L),
                new ExpenseDtos.CustomParticipantRequest(3L, 200L))), payer));
        verify(expenses, never()).save(any());
    }

    @Test void duplicateParticipantIsRejected() {
        assertThrows(ConflictException.class, () -> create(1000L, List.of(1L, 1L)));
    }

    @Test void participantOutsideGroupIsRejected() {
        assertThrows(UnauthorizedException.class, () -> create(1000L, List.of(1L, 99L)));
        verify(expenses, never()).save(any());
    }

    @Test void nonMemberCannotCreateOrViewExpenses() {
        User outsider = user(99L, "outside@example.com", "Outside");
        when(members.existsById(new GroupMemberId(10L, 99L))).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> service.create(10L, new ExpenseDtos.CreateExpenseRequest(
                "Dinner", 1000L, ExpenseCategory.FOOD, SplitType.EQUAL, List.of(1L), null), outsider));
        assertThrows(UnauthorizedException.class, () -> service.list(10L, outsider));
    }

    @Test void memberCanCreateAndViewExpenses() {
        create(1000L, List.of(1L));
        when(expenses.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        assertNotNull(service.list(10L, payer));
        verify(expenses).save(any(Expense.class));
    }

    @Test void singleExpenseRequiresMatchingGroup() {
        Expense expense = expense(50L, payer);
        when(expenses.findByIdAndGroupId(50L, 10L)).thenReturn(Optional.of(expense));
        assertEquals(50L, service.get(10L, 50L, payer).id());
        when(expenses.findByIdAndGroupId(51L, 10L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(10L, 51L, payer));
    }

    @Test void onlyPayerCanDelete() {
        when(members.existsById(new GroupMemberId(10L, 2L))).thenReturn(true);
        when(expenses.findByIdAndGroupId(50L, 10L)).thenReturn(Optional.of(expense(50L, payer)));
        assertThrows(UnauthorizedException.class, () -> service.delete(10L, 50L, second));
        service.delete(10L, 50L, payer);
        verify(expenses).delete(any(Expense.class));
    }

    @Test void creationIsTransactional() throws NoSuchMethodException {
        assertTrue(Arrays.stream(ExpenseService.class.getMethod("create", Long.class, ExpenseDtos.CreateExpenseRequest.class, User.class)
                .getAnnotations()).anyMatch(annotation -> annotation.annotationType().getName().equals("jakarta.transaction.Transactional")));
    }

    private ExpenseDtos.ExpenseResponse create(long amount, List<Long> userIds) {
        return service.create(10L, new ExpenseDtos.CreateExpenseRequest("Dinner", amount,
                ExpenseCategory.FOOD, SplitType.EQUAL, userIds, null), payer);
    }

    private ExpenseDtos.CreateExpenseRequest custom(long amount, List<ExpenseDtos.CustomParticipantRequest> custom) {
        return new ExpenseDtos.CreateExpenseRequest("Dinner", amount, ExpenseCategory.FOOD, SplitType.CUSTOM, null, custom);
    }

    private List<Long> savedShares() {
        ArgumentCaptor<ExpenseParticipant> captor = ArgumentCaptor.forClass(ExpenseParticipant.class);
        verify(participants, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream().map(ExpenseParticipant::getShareMinor).toList();
    }

    private static Expense expense(Long id, User user) {
        Expense expense = new Expense(group(10L), user, 1000L, "Dinner", ExpenseCategory.FOOD, SplitType.EQUAL);
        setId(expense, id);
        return expense;
    }
    private static User user(Long id, String email, String name) {
        User user = new User(email, "hash", name);
        setId(user, id);
        return user;
    }
    private static Group group(Long id) {
        Group group = new Group("Flat", "CODE123456", user(1L, "payer@example.com", "Payer"));
        setId(group, id);
        return group;
    }
    private static void setId(Object entity, Long id) {
        try {
            Field field = entity instanceof Expense ? Expense.class.getDeclaredField("id")
                    : entity instanceof Group ? Group.class.getDeclaredField("id") : User.class.getDeclaredField("id");
            field.setAccessible(true); field.set(entity, id);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
}
