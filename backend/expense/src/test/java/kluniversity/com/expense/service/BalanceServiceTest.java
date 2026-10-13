package kluniversity.com.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kluniversity.com.expense.dto.BalanceResponse;
import kluniversity.com.expense.entity.Expense;
import kluniversity.com.expense.entity.ExpenseSplit;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.ExpenseSplitRepository;
import kluniversity.com.expense.repository.GroupMemberRepository;
import kluniversity.com.expense.repository.GroupRepository;
import kluniversity.com.expense.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock private ExpenseSplitRepository expenseSplitRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private UserRepository userRepository;

    @Test
    void balanceRefreshIncludesBothPaidAndOutstandingSplitAmounts() {
        User viewer = user(1L, "viewer@example.test");
        User payer = user(2L, "payer@example.test");
        Expense expense = new Expense();
        expense.setPaidBy(payer);
        ExpenseSplit paid = split(expense, viewer, 4.25, true);
        ExpenseSplit outstanding = split(expense, viewer, 12.50, false);

        when(userRepository.findByEmail(viewer.getEmail()))
                .thenReturn(Optional.of(viewer));
        when(groupRepository.existsById(17L)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(17L, 1L))
                .thenReturn(true);
        when(expenseSplitRepository.findByExpenseGroupId(17L))
                .thenReturn(List.of(paid, outstanding));

        BalanceService service = new BalanceService(
                expenseSplitRepository,
                groupMemberRepository,
                groupRepository,
                userRepository);
        List<BalanceResponse> balances =
                service.getBalances(17L, viewer.getEmail());

        assertEquals(2, balances.size());
        assertEquals(4.25, balances.stream()
                .filter(balance -> balance.getPaid())
                .findFirst().orElseThrow().getAmount());
        assertEquals(12.50, balances.stream()
                .filter(balance -> !balance.getPaid())
                .findFirst().orElseThrow().getAmount());
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(email);
        return user;
    }

    private ExpenseSplit split(
            Expense expense, User user, double amount, boolean paid) {
        ExpenseSplit split = new ExpenseSplit();
        split.setExpense(expense);
        split.setUser(user);
        split.setAmount(amount);
        split.setPaid(paid);
        return split;
    }
}
