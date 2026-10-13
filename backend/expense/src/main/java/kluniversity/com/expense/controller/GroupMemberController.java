
package kluniversity.com.expense.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kluniversity.com.expense.dto.AddMemberRequest;
import kluniversity.com.expense.entity.GroupMember;
import kluniversity.com.expense.service.GroupMemberService;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:5173")
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    public GroupMemberController(GroupMemberService groupMemberService) {
        this.groupMemberService = groupMemberService;
    }

    @PostMapping("/{groupId}/members")
    public GroupMember addMember(
            @PathVariable Long groupId,
            @RequestBody AddMemberRequest request,
            Authentication authentication) {

        return groupMemberService.addMember(
                groupId, request, authentication.getName());
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMember> getMembers(
            @PathVariable Long groupId,
            Authentication authentication) {

        return groupMemberService.getMembers(
                groupId, authentication.getName());
    }
}
