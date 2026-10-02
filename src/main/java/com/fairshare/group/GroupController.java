package com.fairshare.group;

import com.fairshare.user.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService service;
    public GroupController(GroupService service) { this.service = service; }

    @GetMapping public List<GroupDtos.GroupResponse> list(@AuthenticationPrincipal User user) { return service.list(user); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public GroupDtos.GroupResponse create(@Valid @RequestBody GroupDtos.GroupCreateRequest request,
                                          @AuthenticationPrincipal User user) { return service.create(request, user); }
    @PostMapping("/join")
    public GroupDtos.GroupResponse join(@Valid @RequestBody GroupDtos.GroupJoinRequest request,
                                        @AuthenticationPrincipal User user) { return service.join(request, user); }
    @GetMapping("/{groupId}")
    public GroupDtos.GroupResponse get(@PathVariable Long groupId, @AuthenticationPrincipal User user) { return service.get(groupId, user); }
    @GetMapping("/{groupId}/members")
    public List<GroupDtos.GroupMemberResponse> members(@PathVariable Long groupId, @AuthenticationPrincipal User user) { return service.listMembers(groupId, user); }
    @DeleteMapping("/{groupId}/membership") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long groupId, @AuthenticationPrincipal User user) { service.leave(groupId, user); }
}
