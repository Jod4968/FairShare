package com.fairshare.expense;

import com.fairshare.auth.ConflictException;
import com.fairshare.auth.InvalidRequestException;
import com.fairshare.auth.NotFoundException;
import com.fairshare.auth.UnauthorizedException;
import com.fairshare.group.*;
import com.fairshare.user.User;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ExpenseService {
    private final ExpenseRepository expenses;
    private final ExpenseParticipantRepository participants;
    private final GroupRepository groups;
    private final GroupMemberRepository members;

    public ExpenseService(ExpenseRepository expenses, ExpenseParticipantRepository participants,
                          GroupRepository groups, GroupMemberRepository members) {
        this.expenses = expenses; this.participants = participants;
        this.groups = groups; this.members = members;
    }

    @Transactional
    public ExpenseDtos.ExpenseResponse create(Long groupId, ExpenseDtos.CreateExpenseRequest request, User payer) {
        validateRequest(request);
        Group group = authorizedGroup(groupId, payer);
        List<Long> userIds = resolveParticipantIds(request);
        if (userIds.isEmpty()) throw new ConflictException("At least one participant is required");
        if (new HashSet<>(userIds).size() != userIds.size()) throw new ConflictException("Participants must be unique");
        Map<Long, User> groupUsers = members.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .collect(Collectors.toMap(GroupMember::getUserId, GroupMember::getUser));
        if (!groupUsers.keySet().containsAll(userIds)) throw new UnauthorizedException("Every participant must belong to the group");
        Map<Long, Long> shares = calculateShares(request, userIds);
        Expense expense = expenses.save(new Expense(group, payer, request.amountMinor(),
                request.description().trim(), request.category(), request.splitType()));
        for (Long userId : userIds) participants.save(new ExpenseParticipant(expense, groupUsers.get(userId), shares.get(userId)));
        return response(expense);
    }

    @Transactional
    public List<ExpenseDtos.ExpenseResponse> list(Long groupId, User user) {
        authorizedGroup(groupId, user);
        return expenses.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(this::response).toList();
    }

    @Transactional
    public ExpenseDtos.ExpenseResponse get(Long groupId, Long expenseId, User user) {
        authorizedGroup(groupId, user);
        return response(findExpense(groupId, expenseId));
    }

    @Transactional
    public void delete(Long groupId, Long expenseId, User user) {
        authorizedGroup(groupId, user);
        Expense expense = findExpense(groupId, expenseId);
        if (!expense.getPaidBy().getId().equals(user.getId())) throw new UnauthorizedException("Only the payer can delete this expense");
        expenses.delete(expense);
    }

    private List<Long> resolveParticipantIds(ExpenseDtos.CreateExpenseRequest request) {
        if (request.splitType() == SplitType.EQUAL) return request.participantUserIds() == null ? List.of() : request.participantUserIds();
        if (request.participants() == null) return List.of();
        return request.participants().stream().map(ExpenseDtos.CustomParticipantRequest::userId).toList();
    }

    private Map<Long, Long> calculateShares(ExpenseDtos.CreateExpenseRequest request, List<Long> userIds) {
        Map<Long, Long> shares = new LinkedHashMap<>();
        if (request.splitType() == SplitType.CUSTOM) {
            for (ExpenseDtos.CustomParticipantRequest participant : request.participants()) {
                if (shares.put(participant.userId(), participant.shareMinor()) != null)
                    throw new ConflictException("Participants must be unique");
            }
            long total;
            try {
                total = shares.values().stream().reduce(0L, Math::addExact);
            } catch (ArithmeticException exception) {
                throw new InvalidRequestException("Custom shares are too large");
            }
            if (total != request.amountMinor()) throw new ConflictException("Custom shares must sum exactly to the expense amount");
            return shares;
        }

        long base = request.amountMinor() / userIds.size();
        long remainder = request.amountMinor() % userIds.size();
        for (int index = 0; index < userIds.size(); index++)
            shares.put(userIds.get(index), base + (index < remainder ? 1 : 0));
        return shares;
    }

    private void validateRequest(ExpenseDtos.CreateExpenseRequest request) {
        if (request.amountMinor() == null || request.amountMinor() <= 0) throw new InvalidRequestException("Amount must be greater than zero");
        if (request.description() == null || request.description().isBlank()) throw new InvalidRequestException("Description is required");
        if (request.category() == null) throw new InvalidRequestException("Category is required");
        if (request.splitType() == null) throw new InvalidRequestException("Split type is required");
        if (request.splitType() == SplitType.CUSTOM && request.participants() != null)
            request.participants().forEach(participant -> {
                if (participant == null || participant.userId() == null || participant.shareMinor() == null || participant.shareMinor() <= 0)
                    throw new InvalidRequestException("Custom participant shares must be positive");
            });
    }

    private Group authorizedGroup(Long groupId, User user) {
        Group group = groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group not found"));
        if (!members.existsById(new GroupMemberId(groupId, user.getId())))
            throw new UnauthorizedException("You do not have access to this group");
        return group;
    }

    private Expense findExpense(Long groupId, Long expenseId) {
        return expenses.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));
    }

    private ExpenseDtos.ExpenseResponse response(Expense expense) {
        List<ExpenseDtos.ParticipantResponse> expenseParticipants = participants.findByExpenseIdOrderByUserIdAsc(expense.getId()).stream()
                .map(participant -> new ExpenseDtos.ParticipantResponse(participant.getUserId(),
                        participant.getUser().getEmail(), participant.getUser().getFullName(), participant.getShareMinor())).toList();
        return new ExpenseDtos.ExpenseResponse(expense.getId(), expense.getGroup().getId(), expense.getPaidBy().getId(),
                expense.getPaidBy().getFullName(), expense.getAmountMinor(), expense.getDescription(),
                expense.getCategory(), expense.getSplitType(), expense.getCreatedAt(), expense.getUpdatedAt(), expenseParticipants);
    }
}
