package kluniversity.com.expense.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.GroupSummaryResponse;
import kluniversity.com.expense.entity.Expense;
import kluniversity.com.expense.entity.ExpenseSplit;
import kluniversity.com.expense.entity.Group;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.ExpenseRepository;
import kluniversity.com.expense.repository.ExpenseSplitRepository;
import kluniversity.com.expense.repository.GroupMemberRepository;
import kluniversity.com.expense.repository.GroupRepository;
import kluniversity.com.expense.repository.UserRepository;

@Service
public class GroupSummaryService {

    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseSplitRepository expenseSplitRepository;

    public GroupSummaryService(
            GroupRepository groupRepository,
            ExpenseRepository expenseRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository,
            ExpenseSplitRepository expenseSplitRepository) {

        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.expenseSplitRepository = expenseSplitRepository;
    }

    public GroupSummaryResponse getSummary(Long groupId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Group not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(
                groupId, user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this group");
        }

        
List<Expense> allExpenses =
        expenseRepository.findByGroupId(groupId);

double totalExpenses = 0.0;
double myContributions = 0.0;
double myShare = 0.0;
double myPendingPayments = 0.0;

int successfulPaymentCount = 0;

for (Expense expense : allExpenses) {

    List<ExpenseSplit> splits =
            expenseSplitRepository.findByExpenseId(expense.getId());

    boolean isOwner = expense.getOwner() != null
            && expense.getOwner().getId().equals(user.getId());

    boolean isParticipant = splits.stream().anyMatch(split ->
            split.getUser() != null
            && split.getUser().getId().equals(user.getId()));

    if (!isOwner && !isParticipant) {
        continue;
    }

    totalExpenses += expense.getAmount();

    // Count paid splits from this expense
    for (ExpenseSplit split : splits) {
        if (Boolean.TRUE.equals(split.getPaid())) {
            successfulPaymentCount++;
        }
    }

    if (expense.getPaidBy() != null
            && expense.getPaidBy().getId().equals(user.getId())) {
        myContributions += expense.getAmount();
    }

    if (isParticipant) {
        for (ExpenseSplit split : splits) {
            if (split.getUser() != null
                    && split.getUser().getId().equals(user.getId())) {

                myShare += split.getAmount();

                if (!Boolean.TRUE.equals(split.getPaid())) {
                    myPendingPayments += split.getAmount();
                }
            }
        }
    }
}

GroupSummaryResponse response = new GroupSummaryResponse(
        group.getId(),
        group.getName(),
        totalExpenses,
        successfulPaymentCount);

response.setMyContributions(myContributions);
response.setMyShare(myShare);
response.setMyPendingPayments(myPendingPayments);

return response;

    }
}