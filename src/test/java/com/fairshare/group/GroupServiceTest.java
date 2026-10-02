package com.fairshare.group;

import com.fairshare.auth.ConflictException;
import com.fairshare.auth.NotFoundException;
import com.fairshare.auth.UnauthorizedException;
import com.fairshare.user.User;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class GroupServiceTest {
    @Mock GroupRepository groups;
    @Mock GroupMemberRepository members;
    private final User creator = new User("creator@example.com", "hash", "Creator");
    private final User other = new User("other@example.com", "hash", "Other");

    @BeforeEach void assignUserIds() {
        setId(creator, 10L);
        setId(other, 11L);
    }

    @Test void createAddsCreatorMembershipAndSecureJoinCode() {
        GroupService service = new GroupService(groups, members);
        when(groups.save(any(Group.class))).thenAnswer(invocation -> {
            Group group = invocation.getArgument(0);
            setId(group, 1L);
            return group;
        });
        GroupDtos.GroupResponse result = service.create(new GroupDtos.GroupCreateRequest("Flat"), creator);
        assertEquals(10, result.joinCode().length());
        assertTrue(result.joinCode().matches("[A-Z2-9]+"));
        verify(members).save(any(GroupMember.class));
    }

    @Test void validJoinCreatesMembership() {
        GroupService service = new GroupService(groups, members);
        Group group = group(1L, "Flat", "ABC1234567", creator);
        when(groups.findByJoinCode("ABC1234567")).thenReturn(Optional.of(group));
        when(members.existsById(any())).thenReturn(false);
        assertEquals(1L, service.join(new GroupDtos.GroupJoinRequest("abc1234567"), other).id());
        verify(members).save(any(GroupMember.class));
    }

    @Test void invalidJoinCodeIsRejected() {
        GroupService service = new GroupService(groups, members);
        when(groups.findByJoinCode("INVALID")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.join(new GroupDtos.GroupJoinRequest("invalid"), other));
    }

    @Test void duplicateMembershipIsRejected() {
        GroupService service = new GroupService(groups, members);
        when(groups.findByJoinCode("ABC1234567")).thenReturn(Optional.of(group(1L, "Flat", "ABC1234567", creator)));
        when(members.existsById(any())).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.join(new GroupDtos.GroupJoinRequest("ABC1234567"), other));
    }

    @Test void nonMemberCannotAccessDetailsOrMembers() {
        GroupService service = new GroupService(groups, members);
        when(groups.findById(1L)).thenReturn(Optional.of(group(1L, "Flat", "ABC1234567", creator)));
        when(members.existsById(new GroupMemberId(1L, other.getId()))).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> service.get(1L, other));
        assertThrows(UnauthorizedException.class, () -> service.listMembers(1L, other));
    }

    @Test void memberCanAccessDetailsAndMembers() {
        GroupService service = new GroupService(groups, members);
        Group group = group(1L, "Flat", "ABC1234567", creator);
        when(groups.findById(1L)).thenReturn(Optional.of(group));
        when(members.existsById(new GroupMemberId(1L, creator.getId()))).thenReturn(true);
        when(members.findByGroupIdOrderByJoinedAtAsc(1L)).thenReturn(java.util.List.of());
        assertEquals("Flat", service.get(1L, creator).name());
        assertTrue(service.listMembers(1L, creator).isEmpty());
    }

    @Test void memberCanLeave() {
        GroupService service = new GroupService(groups, members);
        when(groups.findById(1L)).thenReturn(Optional.of(group(1L, "Flat", "ABC1234567", creator)));
        when(members.existsById(new GroupMemberId(1L, creator.getId()))).thenReturn(true);
        service.leave(1L, creator);
        verify(members).deleteById(new GroupMemberId(1L, creator.getId()));
    }

    private static Group group(Long id, String name, String code, User creator) {
        Group group = new Group(name, code, creator);
        setId(group, id);
        return group;
    }
    private static void setId(Object entity, Long id) {
        try {
            var field = entity instanceof Group ? Group.class.getDeclaredField("id") : User.class.getDeclaredField("id");
            field.setAccessible(true); field.set(entity, id);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
}
