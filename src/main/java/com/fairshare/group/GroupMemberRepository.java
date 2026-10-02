package com.fairshare.group;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    List<GroupMember> findByUserIdOrderByJoinedAtDesc(Long userId);
    List<GroupMember> findByGroupIdOrderByJoinedAtAsc(Long groupId);
}
