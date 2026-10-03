package com.fairshare.ai;

import com.fairshare.user.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups/{groupId}/assistant")
public class AiController {
    private final AiService service;
    public AiController(AiService service) { this.service = service; }
    @PostMapping("/messages")
    public AiDtos.MessageResponse message(@PathVariable Long groupId, @Valid @RequestBody AiDtos.MessageRequest request,
                                          @AuthenticationPrincipal User user) {
        return service.message(groupId, request.message(), user);
    }
}
