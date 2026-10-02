package com.fairshare.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class GroupDtos {
    private GroupDtos() {}
    public record GroupCreateRequest(@NotBlank @Size(max = 120) String name) {}
    public record GroupJoinRequest(@NotBlank @Size(max = 32) String joinCode) {}
    public record GroupResponse(Long id, String name, String joinCode, Long createdBy,
                                Instant createdAt, Instant updatedAt) {}
    public record GroupMemberResponse(Long userId, String email, String fullName, Instant joinedAt) {}
}
