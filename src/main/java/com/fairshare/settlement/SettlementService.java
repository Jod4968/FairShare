package com.fairshare.settlement;

import com.fairshare.auth.*;
import com.fairshare.expense.*;
import com.fairshare.group.*;
import com.fairshare.user.User;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SettlementService {
    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final ExpenseRepository expenses;
    private final ExpenseParticipantRepository participants;
    private final SettlementRepository settlements;

    public SettlementService(GroupRepository groups, GroupMemberRepository members, ExpenseRepository expenses,
                             ExpenseParticipantRepository participants, SettlementRepository settlements) {
        this.groups = groups; this.members = members; this.expenses = expenses;
        this.participants = participants; this.settlements = settlements;
    }

    @Transactional
    public List<SettlementDtos.BalanceResponse> balances(Long groupId, User requester) {
        Group group = authorizedGroup(groupId, requester);
        return calculateBalances(groupId, group);
    }

    @Transactional
    public List<SettlementDtos.SuggestionResponse> suggestions(Long groupId, User requester) {
        Group group = authorizedGroup(groupId, requester);
        Map<Long, Long> balances = outstandingBalanceMap(groupId, group);
        Map<Long, User> users = groupUsers(groupId);
        List<MutableBalance> debtors = balances.entrySet().stream().filter(entry -> entry.getValue() < 0)
                .map(entry -> new MutableBalance(users.get(entry.getKey()), entry.getValue())).toList();
        List<MutableBalance> creditors = balances.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .map(entry -> new MutableBalance(users.get(entry.getKey()), entry.getValue())).toList();
        List<SettlementDtos.SuggestionResponse> result = new ArrayList<>();
        int debtorIndex = 0, creditorIndex = 0;
        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            MutableBalance debtor = debtors.get(debtorIndex);
            MutableBalance creditor = creditors.get(creditorIndex);
            long amount = Math.min(Math.negateExact(debtor.amount), creditor.amount);
            if (amount > 0) result.add(new SettlementDtos.SuggestionResponse(debtor.user.getId(), debtor.user.getFullName(),
                    creditor.user.getId(), creditor.user.getFullName(), amount));
            debtor.amount += amount; creditor.amount -= amount;
            if (debtor.amount == 0) debtorIndex++;
            if (creditor.amount == 0) creditorIndex++;
        }
        return result;
    }

    @Transactional
    public SettlementDtos.SettlementResponse create(Long groupId, SettlementDtos.CreateSettlementRequest request, User fromUser) {
        Group group = authorizedGroup(groupId, fromUser);
        if (request.toUserId().equals(fromUser.getId())) throw new InvalidRequestException("You cannot settle with yourself");
        User toUser = groupUsers(groupId).get(request.toUserId());
        if (toUser == null) throw new UnauthorizedException("Recipient must belong to the group");
        Settlement settlement = settlements.save(new Settlement(group, fromUser, toUser, request.amountMinor()));
        return response(settlement);
    }

    @Transactional
    public List<SettlementDtos.SettlementResponse> list(Long groupId, User requester) {
        authorizedGroup(groupId, requester);
        return settlements.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(this::response).toList();
    }

    @Transactional
    public SettlementDtos.SettlementResponse complete(Long groupId, Long settlementId, User requester) {
        authorizedGroup(groupId, requester);
        Settlement settlement = settlements.findByIdAndGroupId(settlementId, groupId)
                .orElseThrow(() -> new NotFoundException("Settlement not found"));
        if (settlement.getCompletedAt() != null) throw new ConflictException("Settlement is already completed");
        settlement.complete();
        return response(settlements.save(settlement));
    }

    private List<SettlementDtos.BalanceResponse> calculateBalances(Long groupId, Group group) {
        Map<Long, User> users = groupUsers(groupId);
        Map<Long, Long> paid = users.keySet().stream().collect(Collectors.toMap(Function.identity(), ignored -> 0L, (a, b) -> a, LinkedHashMap::new));
        Map<Long, Long> owed = new LinkedHashMap<>(paid);
        for (Expense expense : expenses.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            paid.compute(expense.getPaidBy().getId(), (key, value) -> Math.addExact(value, expense.getAmountMinor()));
            for (ExpenseParticipant participant : participants.findByExpenseIdOrderByUserIdAsc(expense.getId()))
                owed.compute(participant.getUserId(), (key, value) -> Math.addExact(value, participant.getShareMinor()));
        }
        Map<Long, Long> completedEffect = completedSettlementEffects(groupId);
        return users.values().stream().sorted(Comparator.comparing(User::getId)).map(user -> {
            long amountPaid = paid.get(user.getId()), amountOwed = owed.get(user.getId());
            long balance = Math.addExact(Math.subtractExact(amountPaid, amountOwed), completedEffect.getOrDefault(user.getId(), 0L));
            return new SettlementDtos.BalanceResponse(user.getId(), user.getFullName(), amountPaid, amountOwed, balance);
        }).toList();
    }

    private Map<Long, Long> outstandingBalanceMap(Long groupId, Group group) {
        return calculateBalances(groupId, group).stream().collect(Collectors.toMap(SettlementDtos.BalanceResponse::userId,
                SettlementDtos.BalanceResponse::balanceMinor, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, Long> completedSettlementEffects(Long groupId) {
        Map<Long, Long> effects = new HashMap<>();
        for (Settlement settlement : settlements.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            if (settlement.getCompletedAt() != null) {
                // A completed transfer reduces the debtor's negative balance and the
                // creditor's positive balance without changing historical expenses.
                effects.merge(settlement.getFromUser().getId(), settlement.getAmountMinor(), Math::addExact);
                effects.merge(settlement.getToUser().getId(), -settlement.getAmountMinor(), Math::addExact);
            }
        }
        return effects;
    }

    private Map<Long, User> groupUsers(Long groupId) {
        return members.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .collect(Collectors.toMap(GroupMember::getUserId, GroupMember::getUser, (a, b) -> a, LinkedHashMap::new));
    }

    private Group authorizedGroup(Long groupId, User user) {
        Group group = groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group not found"));
        if (!members.existsById(new GroupMemberId(groupId, user.getId())))
            throw new UnauthorizedException("You do not have access to this group");
        return group;
    }

    private SettlementDtos.SettlementResponse response(Settlement settlement) {
        return new SettlementDtos.SettlementResponse(settlement.getId(), settlement.getFromUser().getId(),
                settlement.getFromUser().getFullName(), settlement.getToUser().getId(), settlement.getToUser().getFullName(),
                settlement.getAmountMinor(), settlement.getCreatedAt(), settlement.getCompletedAt(),
                settlement.getCompletedAt() == null ? "PENDING" : "COMPLETED");
    }

    private static final class MutableBalance {
        private final User user;
        private long amount;
        private MutableBalance(User user, long amount) { this.user = user; this.amount = amount; }
    }
}
