package com.fairshare.expense;

import com.fairshare.user.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {
    private final ExpenseService service;
    public ExpenseController(ExpenseService service) { this.service = service; }
    @GetMapping public List<ExpenseDtos.ExpenseResponse> list(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        return service.list(groupId, user);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ExpenseDtos.ExpenseResponse create(@PathVariable Long groupId, @Valid @RequestBody ExpenseDtos.CreateExpenseRequest request,
                                              @AuthenticationPrincipal User user) {
        return service.create(groupId, request, user);
    }
    @GetMapping("/{expenseId}")
    public ExpenseDtos.ExpenseResponse get(@PathVariable Long groupId, @PathVariable Long expenseId,
                                           @AuthenticationPrincipal User user) {
        return service.get(groupId, expenseId, user);
    }
    @DeleteMapping("/{expenseId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long groupId, @PathVariable Long expenseId, @AuthenticationPrincipal User user) {
        service.delete(groupId, expenseId, user);
    }
}
