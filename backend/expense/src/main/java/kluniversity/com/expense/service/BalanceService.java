
package kluniversity.com.expense.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.BalanceResponse;
import kluniversity.com.expense.entity.ExpenseSplit;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.ExpenseSplitRepository;
import kluniversity.com.expense.repository.GroupMemberRepository;
import kluniversity.com.expense.repository.GroupRepository;
import kluniversity.com.expense.repository.UserRepository;

@Service
public class BalanceService {

    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public BalanceService(
            ExpenseSplitRepository expenseSplitRepository,
            GroupMemberRepository groupMemberRepository,
            GroupRepository groupRepository,
            UserRepository userRepository) {
        this.expenseSplitRepository = expenseSplitRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public List<BalanceResponse> getBalances(Long groupId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (!groupRepository.existsById(groupId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Group not found");
        }

        if (!groupMemberRepository
                .existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this group");
        }

        Map<String, Double> balanceMap = new HashMap<>();
        Map<String, String> fromUserNames = new HashMap<>();
        Map<String, String> toUserNames = new HashMap<>();
        Map<String, Boolean> paidStates = new HashMap<>();

        List<ExpenseSplit> splits =
                expenseSplitRepository.findByExpenseGroupId(groupId);

        for (ExpenseSplit split : splits) {
            if (split.getUser() == null || split.getExpense() == null
                    || split.getExpense().getPaidBy() == null
                    || split.getAmount() == null) {
                continue;
            }
            Long fromUserId = split.getUser().getId();
            Long toUserId = split.getExpense().getPaidBy().getId();

            if (fromUserId.equals(toUserId)) {
                continue;
            }

            boolean paid = Boolean.TRUE.equals(split.getPaid());
            String key = fromUserId + "-" + toUserId + "-" + paid;

            fromUserNames.put(key, split.getUser().getName());
            toUserNames.put(key, split.getExpense().getPaidBy().getName());
            paidStates.put(key, paid);
            balanceMap.merge(key, split.getAmount(), Double::sum);
        }

        List<BalanceResponse> balances = new ArrayList<>();

        for (Map.Entry<String, Double> entry : balanceMap.entrySet()) {
            double amount = entry.getValue();

            if (amount > 0.01) {
                String key = entry.getKey();
                balances.add(new BalanceResponse(
                        fromUserNames.get(key),
                        toUserNames.get(key),
                        amount,
                        paidStates.get(key)));
            }
        }

        return balances;
    }
}
