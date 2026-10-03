package com.fairshare.settlement;

import com.fairshare.group.Group;
import com.fairshare.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "settlements")
public class Settlement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;
    @Column(name = "amount_minor", nullable = false) private Long amountMinor;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;

    protected Settlement() {}
    public Settlement(Group group, User fromUser, User toUser, Long amountMinor) {
        this.group = group; this.fromUser = fromUser; this.toUser = toUser; this.amountMinor = amountMinor;
    }
    @PrePersist void created() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public User getFromUser() { return fromUser; }
    public User getToUser() { return toUser; }
    public Long getAmountMinor() { return amountMinor; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void complete() { completedAt = Instant.now(); }
}
