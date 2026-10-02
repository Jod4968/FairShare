package com.fairshare.group;

import com.fairshare.auth.ConflictException;
import com.fairshare.auth.NotFoundException;
import com.fairshare.auth.UnauthorizedException;
import com.fairshare.user.User;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {
    private static final char[] JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final SecureRandom random = new SecureRandom();

    public GroupService(GroupRepository groups, GroupMemberRepository members) {
        this.groups = groups; this.members = members;
    }

    @Transactional
    public GroupDtos.GroupResponse create(GroupDtos.GroupCreateRequest request, User user) {
        Group group = groups.save(new Group(request.name().trim(), uniqueJoinCode(), user));
        members.save(new GroupMember(group, user));
        return groupDto(group);
    }

    @Transactional(readOnly = true)
    public List<GroupDtos.GroupResponse> list(User user) {
        return members.findByUserIdOrderByJoinedAtDesc(user.getId()).stream()
                .map(member -> groupDto(groups.getReferenceById(member.getGroupId()))).toList();
    }

    @Transactional
    public GroupDtos.GroupResponse join(GroupDtos.GroupJoinRequest request, User user) {
        String joinCode = request.joinCode().trim().toUpperCase();
        Group group = groups.findByJoinCode(joinCode)
                .orElseThrow(() -> new NotFoundException("Invalid join code"));
        GroupMemberId membershipId = new GroupMemberId(group.getId(), user.getId());
        if (members.existsById(membershipId)) throw new ConflictException("You are already a member of this group");
        members.save(new GroupMember(group, user));
        return groupDto(group);
    }

    @Transactional(readOnly = true)
    public GroupDtos.GroupResponse get(Long groupId, User user) {
        Group group = authorizedGroup(groupId, user);
        return groupDto(group);
    }

    @Transactional(readOnly = true)
    public List<GroupDtos.GroupMemberResponse> listMembers(Long groupId, User user) {
        authorizedGroup(groupId, user);
        return members.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .map(member -> new GroupDtos.GroupMemberResponse(member.getUserId(), member.getUser().getEmail(),
                        member.getUser().getFullName(), member.getJoinedAt())).collect(Collectors.toList());
    }

    @Transactional
    public void leave(Long groupId, User user) {
        authorizedGroup(groupId, user);
        members.deleteById(new GroupMemberId(groupId, user.getId()));
        // The creator may leave; ownership is retained as historical metadata and is not an admin role.
    }

    private Group authorizedGroup(Long groupId, User user) {
        Group group = groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group not found"));
        if (!members.existsById(new GroupMemberId(groupId, user.getId())))
            throw new UnauthorizedException("You do not have access to this group");
        return group;
    }

    private GroupDtos.GroupResponse groupDto(Group group) {
        return new GroupDtos.GroupResponse(group.getId(), group.getName(), group.getJoinCode(),
                group.getCreatedBy().getId(), group.getCreatedAt(), group.getUpdatedAt());
    }

    private String uniqueJoinCode() {
        String code;
        do {
            StringBuilder value = new StringBuilder(10);
            for (int i = 0; i < 10; i++) value.append(JOIN_CODE_ALPHABET[random.nextInt(JOIN_CODE_ALPHABET.length)]);
            code = value.toString();
        } while (groups.findByJoinCode(code).isPresent());
        return code;
    }
}
