package kluniversity.com.expense.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.ReminderResponse;
import kluniversity.com.expense.entity.ExpenseSplit;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.ExpenseSplitRepository;
import kluniversity.com.expense.repository.GroupMemberRepository;
import kluniversity.com.expense.repository.GroupRepository;
import kluniversity.com.expense.repository.UserRepository;

@Service
public class ReminderService {

    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public ReminderService(
            ExpenseSplitRepository expenseSplitRepository,
            GroupMemberRepository groupMemberRepository,
            GroupRepository groupRepository,
            UserRepository userRepository) {

        this.expenseSplitRepository = expenseSplitRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public List<ReminderResponse> getPendingReminders(
            Long groupId, String email) {

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (!groupRepository.existsById(groupId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Group not found");
        }

        if (!groupMemberRepository.existsByGroupIdAndUserId(
                groupId, currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this group");
        }

        List<ReminderResponse> reminders = new ArrayList<>();

        List<ExpenseSplit> splits =
                expenseSplitRepository.findByUserIdAndExpenseGroupId(
                        currentUser.getId(), groupId);

        for (ExpenseSplit split : splits) {

            if (Boolean.TRUE.equals(split.getPaid())) {
                continue;
            }

            if (split.getExpense() == null
                    || split.getExpense().getGroup() == null
                    || split.getUser() == null
                    || split.getExpense().getPaidBy() == null) {
                continue;
            }

            if (split.getUser().getId().equals(
                    split.getExpense().getPaidBy().getId())) {
                continue;
            }

            String fromUser = split.getUser().getName();
            String toUser = split.getExpense().getPaidBy().getName();
            String fromEmail = split.getUser().getEmail();
            String toEmail = split.getExpense().getPaidBy().getEmail();
            Double amount = split.getAmount();

            String message = fromUser + " owes " + toUser
                    + " ₹" + String.format("%.2f", amount)
                    + " for " + split.getExpense().getDescription();

            reminders.add(new ReminderResponse(
                    split.getId(),
                    fromUser,
                    toUser,
                    fromEmail,
                    toEmail,
                    amount,
                    message));
        }

        return reminders;
    }
}