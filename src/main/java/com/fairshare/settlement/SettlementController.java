package com.fairshare.settlement;

import com.fairshare.user.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class SettlementController {
    private final SettlementService service;
    public SettlementController(SettlementService service) { this.service = service; }
    @GetMapping("/balances")
    public List<SettlementDtos.BalanceResponse> balances(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        return service.balances(groupId, user);
    }
    @GetMapping("/settlements/suggestions")
    public List<SettlementDtos.SuggestionResponse> suggestions(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        return service.suggestions(groupId, user);
    }
    @PostMapping("/settlements") @ResponseStatus(HttpStatus.CREATED)
    public SettlementDtos.SettlementResponse create(@PathVariable Long groupId, @Valid @RequestBody SettlementDtos.CreateSettlementRequest request,
                                                    @AuthenticationPrincipal User user) {
        return service.create(groupId, request, user);
    }
    @GetMapping("/settlements")
    public List<SettlementDtos.SettlementResponse> list(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        return service.list(groupId, user);
    }
    @PostMapping("/settlements/{settlementId}/complete")
    public SettlementDtos.SettlementResponse complete(@PathVariable Long groupId, @PathVariable Long settlementId,
                                                      @AuthenticationPrincipal User user) {
        return service.complete(groupId, settlementId, user);
    }
}
