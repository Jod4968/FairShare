package com.fairshare.group;

import java.io.Serializable;
import java.util.Objects;

public class GroupMemberId implements Serializable {
    private Long groupId;
    private Long userId;

    public GroupMemberId() {}
    public GroupMemberId(Long groupId, Long userId) { this.groupId = groupId; this.userId = userId; }
    public Long getGroupId() { return groupId; }
    public Long getUserId() { return userId; }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GroupMemberId id)) return false;
        return Objects.equals(groupId, id.groupId) && Objects.equals(userId, id.userId);
    }
    @Override public int hashCode() { return Objects.hash(groupId, userId); }
}
