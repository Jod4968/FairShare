package com.fairshare.group;

import com.fairshare.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "group_members")
@IdClass(GroupMemberId.class)
public class GroupMember {
    @Id @Column(name = "group_id")
    private Long groupId;
    @Id @Column(name = "user_id")
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected GroupMember() {}
    public GroupMember(Group group, User user) {
        this.group = group; this.user = user;
        this.groupId = group.getId(); this.userId = user.getId();
    }
    @PrePersist void joined() { joinedAt = Instant.now(); }
    public Long getGroupId() { return groupId; }
    public Long getUserId() { return userId; }
    public User getUser() { return user; }
    public Instant getJoinedAt() { return joinedAt; }
}
