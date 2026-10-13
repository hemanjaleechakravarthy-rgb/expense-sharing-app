package kluniversity.com.expense.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kluniversity.com.expense.dto.CreateGroupRequest;
import kluniversity.com.expense.entity.Group;
import kluniversity.com.expense.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:5173")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // Create a new group
    @PostMapping
    public Group createGroup(
            @RequestBody CreateGroupRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        return groupService.createGroup(request, email);
    }
    // Get groups belonging to the logged-in user
    @GetMapping
    public List<Group> getGroups(Authentication authentication) {
        String email = authentication.getName();
        return groupService.getGroupsByUser(email);
    }
    @PutMapping("/{groupId}")
    public Group renameGroup(
        @PathVariable Long groupId,
        @RequestBody CreateGroupRequest request,
        Authentication authentication) {

    if (authentication == null) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Please log in first");
    }
    return groupService.renameGroup(groupId, request, authentication.getName());
}
}