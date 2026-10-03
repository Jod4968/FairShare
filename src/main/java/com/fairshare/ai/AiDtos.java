package com.fairshare.ai;

import com.fairshare.expense.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class AiDtos {
    private AiDtos() {}
    public record MessageRequest(@NotBlank String message) {}
    public record Intent(AiIntent intent, Long amountMinor, String description, ExpenseCategory category,
                         SplitType splitType, List<String> participantNames, List<CustomShare> customShares,
                         String targetUserName, AiPeriod period, String payerName) {}
    public record CustomShare(String participantName, Long shareMinor) {}
    public record MessageResponse(String message, AiIntent intent, boolean success) {}
}
