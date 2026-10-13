
package kluniversity.com.expense.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kluniversity.com.expense.dto.GroupSummaryResponse;
import kluniversity.com.expense.service.GroupSummaryService;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:5173")
public class GroupSummaryController {

    private final GroupSummaryService groupSummaryService;

    public GroupSummaryController(
            GroupSummaryService groupSummaryService) {
        this.groupSummaryService = groupSummaryService;
    }

    @GetMapping("/{groupId}/summary")
    public GroupSummaryResponse getSummary(
            @PathVariable Long groupId,
            Authentication authentication) {

        return groupSummaryService.getSummary(
                groupId, authentication.getName());
    }
}
