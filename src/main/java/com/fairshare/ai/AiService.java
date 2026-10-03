package com.fairshare.ai;

import com.fairshare.auth.*;
import com.fairshare.expense.*;
import com.fairshare.group.*;
import com.fairshare.settlement.SettlementDtos;
import com.fairshare.settlement.SettlementService;
import com.fairshare.user.User;
import jakarta.transaction.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private final AiClient ai;
    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final ExpenseRepository expenses;
    private final ExpenseService expenseService;
    private final SettlementService settlementService;
    private final boolean enabled;

    public AiService(AiClient ai, GroupRepository groups, GroupMemberRepository members, ExpenseRepository expenses,
                     ExpenseService expenseService,
                     SettlementService settlementService, @Value("${ai.enabled:false}") boolean enabled) {
        this.ai = ai; this.groups = groups; this.members = members; this.expenses = expenses;
        this.expenseService = expenseService;
        this.settlementService = settlementService; this.enabled = enabled;
    }

    @Transactional
    public AiDtos.MessageResponse message(Long groupId, String text, User currentUser) {
        authorizedGroup(groupId, currentUser);
        if (!enabled) throw new AiUnavailableException();
        Map<Long, User> groupUsers = groupUsers(groupId);
        AiDtos.Intent intent = ai.interpret(text, groupUsers.values().stream().map(User::getFullName).toList());
        if (intent == null || intent.intent() == null || intent.intent() == AiIntent.UNKNOWN)
            return new AiDtos.MessageResponse("I can help with expenses, balances, and spending. What would you like to do?", AiIntent.UNKNOWN, false);
        return switch (intent.intent()) {
            case CREATE_EXPENSE -> createExpense(groupId, text, intent, currentUser, groupUsers);
            case QUERY_BALANCES -> balances(groupId, currentUser);
            case QUERY_GROUP_SPENDING -> spending(groupId, intent.period(), null, null, currentUser);
            case QUERY_PERSON_SPENDING -> {
                User target = resolveName(intent.targetUserName(), currentUser, groupUsers);
                yield spending(groupId, intent.period(), target, null, currentUser);
            }
            case QUERY_CATEGORY_EXPENSES -> spending(groupId, intent.period(), null, intent.category(), currentUser);
            case QUERY_MONTHLY_SPENDING -> spending(groupId, intent.period() == null ? AiPeriod.CURRENT_MONTH : intent.period(), null, null, currentUser);
            case UNKNOWN -> new AiDtos.MessageResponse("I need a little more detail to help with that.", AiIntent.UNKNOWN, false);
        };
    }

    private AiDtos.MessageResponse createExpense(Long groupId, String text, AiDtos.Intent intent,
                                                 User currentUser, Map<Long, User> users) {
        if (intent.payerName() != null && !intent.payerName().isBlank()
                && !isMe(intent.payerName()) && !intent.payerName().equalsIgnoreCase(currentUser.getFullName()))
            return new AiDtos.MessageResponse("Only the authenticated user can be the payer.", AiIntent.CREATE_EXPENSE, false);
        final long amountMinor;
        try {
            amountMinor = MonetaryAmountParser.toMinorUnits(text);
        } catch (InvalidRequestException exception) {
            return new AiDtos.MessageResponse(exception.getMessage(), AiIntent.CREATE_EXPENSE, false);
        }
        if (intent.description() == null || intent.description().isBlank())
            return new AiDtos.MessageResponse("Please provide an expense description.", AiIntent.CREATE_EXPENSE, false);
        List<String> names = intent.participantNames() == null ? List.of() : intent.participantNames();
        List<Long> ids = new ArrayList<>();
        for (String name : names) ids.add(resolveName(name, currentUser, users).getId());
        if (ids.isEmpty()) ids.add(currentUser.getId());
        if (!ids.contains(currentUser.getId())) ids.add(0, currentUser.getId());
        ExpenseCategory category = intent.category() == null ? ExpenseCategory.OTHER : intent.category();
        SplitType split = intent.splitType() == null ? SplitType.EQUAL : intent.splitType();
        List<ExpenseDtos.CustomParticipantRequest> custom = null;
        if (split == SplitType.CUSTOM && intent.customShares() != null)
            custom = intent.customShares().stream().map(share -> new ExpenseDtos.CustomParticipantRequest(
                    resolveName(share.participantName(), currentUser, users).getId(), share.shareMinor())).toList();
        expenseService.create(groupId, new ExpenseDtos.CreateExpenseRequest(intent.description(), amountMinor,
                category, split, split == SplitType.EQUAL ? ids : null, custom), currentUser);
        return new AiDtos.MessageResponse("Added " + money(amountMinor) + " " + intent.description().trim() + " expense.", AiIntent.CREATE_EXPENSE, true);
    }

    private AiDtos.MessageResponse balances(Long groupId, User user) {
        List<SettlementDtos.BalanceResponse> balances = settlementService.balances(groupId, user);
        SettlementDtos.BalanceResponse own = balances.stream().filter(balance -> balance.userId().equals(user.getId())).findFirst().orElseThrow();
        String message = own.balanceMinor() < 0 ? "You currently owe " + money(-own.balanceMinor()) + "."
                : own.balanceMinor() > 0 ? "You should receive " + money(own.balanceMinor()) + "." : "You are settled.";
        return new AiDtos.MessageResponse(message, AiIntent.QUERY_BALANCES, true);
    }

    private AiDtos.MessageResponse spending(Long groupId, AiPeriod period, User target, ExpenseCategory category, User currentUser) {
        long total = 0;
        for (Expense expense : expenses.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            if (!inPeriod(expense.getCreatedAt(), period)) continue;
            if (category != null && expense.getCategory() != category) continue;
            if (target != null && !expense.getPaidBy().getId().equals(target.getId())) continue;
            total = Math.addExact(total, expense.getAmountMinor());
        }
        String subject = target == null ? category == null ? "Your group spent " : "Your group spent on " + category.name().toLowerCase() + " "
                : target.getId().equals(currentUser.getId()) ? "You paid " : target.getFullName() + " paid ";
        return new AiDtos.MessageResponse(subject + money(total) + ".", target == null ? category == null ? AiIntent.QUERY_GROUP_SPENDING : AiIntent.QUERY_CATEGORY_EXPENSES : AiIntent.QUERY_PERSON_SPENDING, true);
    }

    private boolean inPeriod(Instant created, AiPeriod period) {
        if (period == null || period == AiPeriod.ALL_TIME) return true;
        YearMonth month = YearMonth.now(ZoneOffset.UTC).plusMonths(period == AiPeriod.LAST_MONTH ? -1 : 0);
        Instant start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return !created.isBefore(start) && created.isBefore(month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private User resolveName(String name, User current, Map<Long, User> users) {
        if (name == null || isMe(name)) return current;
        List<User> matches = users.values().stream().filter(user -> user.getFullName().equalsIgnoreCase(name.trim())).toList();
        if (matches.size() != 1) throw new InvalidRequestException(matches.isEmpty() ? "I couldn't find a group member named " + name + "." : "That member name is ambiguous.");
        return matches.get(0);
    }
    private boolean isMe(String name) { return name != null && Set.of("me", "myself", "i").contains(name.trim().toLowerCase()); }
    private String money(long minor) {
        long absolute = Math.abs(minor);
        String fractional = absolute % 100 == 0 ? "" : "." + String.format(Locale.ROOT, "%02d", absolute % 100);
        return (minor < 0 ? "-₹" : "₹") + (absolute / 100) + fractional;
    }
    private Map<Long, User> groupUsers(Long groupId) { return members.findByGroupIdOrderByJoinedAtAsc(groupId).stream().collect(Collectors.toMap(GroupMember::getUserId, GroupMember::getUser, (a,b) -> a, LinkedHashMap::new)); }
    private Group authorizedGroup(Long groupId, User user) {
        Group group = groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group not found"));
        if (!members.existsById(new GroupMemberId(groupId, user.getId()))) throw new UnauthorizedException("You do not have access to this group");
        return group;
    }
}
