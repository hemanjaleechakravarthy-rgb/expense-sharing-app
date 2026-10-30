
package kluniversity.com.expense.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kluniversity.com.expense.dto.ReminderResponse;
import kluniversity.com.expense.service.ReminderService;

@RestController
@RequestMapping("/api/reminders")
@CrossOrigin(origins = "http://localhost:5173")
public class MyReminderController {

    private final ReminderService reminderService;

    public MyReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    // Get pending reminders across all groups
    @GetMapping
    public List<ReminderResponse> getMyPendingReminders(
            Authentication authentication) {

        return reminderService.getMyPendingReminders(
                authentication.getName());
    }
}
