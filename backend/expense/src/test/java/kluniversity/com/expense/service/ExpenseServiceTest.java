package kluniversity.com.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.CreateExpenseRequest;
import kluniversity.com.expense.entity.Expense;
import kluniversity.com.expense.entity.ExpenseSplit;
import kluniversity.com.expense.entity.Group;
import kluniversity.com.expense.entity.GroupMember;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.ExpenseRepository;
import kluniversity.com.expense.repository.ExpenseSplitRepository;
import kluniversity.com.expense.repository.GroupMemberRepository;
import kluniversity.com.expense.repository.GroupRepository;
import kluniversity.com.expense.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExpenseSplitRepository expenseSplitRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    private ExpenseService expenseService;

    @BeforeEach
void setUp() {
    expenseService = new ExpenseService(
            expenseRepository,
            expenseSplitRepository,
            groupRepository,
            groupMemberRepository,
            userRepository,
            emailService);
}

    @Test
    void equalSplitAllocatesRemainderInPaiseAndUsesEveryMember() {
        User owner = user(1L, "owner@example.test");
        User second = user(2L, "second@example.test");
        User third = user(3L, "third@example.test");
        Group group = new Group();
        group.setId(11L);

        when(userRepository.findByEmail(owner.getEmail()))
                .thenReturn(Optional.of(owner));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(groupRepository.existsById(11L)).thenReturn(true);
        when(groupRepository.findById(11L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(11L, 1L))
                .thenReturn(true);
        when(groupMemberRepository.findByGroupId(11L)).thenReturn(List.of(
                membership(group, owner), membership(group, second),
                membership(group, third)));
        when(expenseRepository.findBySubmissionKey("request-1"))
                .thenReturn(Optional.empty());
        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AtomicReference<List<ExpenseSplit>> savedSplitsRef =
                new AtomicReference<>();
        when(expenseSplitRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    List<ExpenseSplit> saved = invocation.getArgument(0);
                    savedSplitsRef.set(saved);
                    return saved;
                });

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription("Shared meal");
        request.setAmount(10.00);
        request.setPaidByUserId(owner.getId());

        expenseService.createEqualSplitExpense(
                11L, request, owner.getEmail(), "request-1");

        verify(expenseSplitRepository).saveAll(any());
        List<ExpenseSplit> savedSplits = savedSplitsRef.get();
        assertEquals(3, savedSplits.size());
        assertEquals(List.of(3.34, 3.33, 3.33),
                savedSplits.stream().map(ExpenseSplit::getAmount).toList());
    }

    @Test
    void groupExpenseListHidesUnsplitExpensesOwnedByOtherMembers() {
        User requester = user(1L, "requester@example.test");
        User other = user(2L, "other@example.test");
        Group group = new Group();
        group.setId(11L);
        Expense owned = expense(1L, group, requester);
        Expense privateExpense = expense(2L, group, other);
        Expense participantExpense = expense(3L, group, other);
        ExpenseSplit participantSplit = new ExpenseSplit();
        participantSplit.setUser(requester);

        when(userRepository.findByEmail(requester.getEmail()))
                .thenReturn(Optional.of(requester));
        when(groupRepository.existsById(11L)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(11L, 1L))
                .thenReturn(true);
        when(expenseRepository.findByGroupId(11L))
                .thenReturn(List.of(owned, privateExpense, participantExpense));
        when(expenseSplitRepository.findByExpenseId(2L)).thenReturn(List.of());
        when(expenseSplitRepository.findByExpenseId(3L))
                .thenReturn(List.of(participantSplit));

        assertEquals(
                List.of(owned, participantExpense),
                expenseService.getExpenses(11L, requester.getEmail()));
    }

    @Test
    void equalSplitRequiresAuthenticatedGroupMembershipBeforeSaving() {
        User owner = user(1L, "owner@example.test");
        when(userRepository.findByEmail(owner.getEmail()))
                .thenReturn(Optional.of(owner));
        when(groupRepository.existsById(11L)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(11L, 1L))
                .thenReturn(false);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription("Unauthorized");
        request.setAmount(12.00);
        request.setPaidByUserId(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> expenseService.createEqualSplitExpense(
                        11L, request, owner.getEmail(), "request-2"));

        assertEquals(403, exception.getStatusCode().value());
        verify(expenseRepository, never()).save(any(Expense.class));
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(email);
        return user;
    }

    private GroupMember membership(Group group, User user) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        return member;
    }

    private Expense expense(Long id, Group group, User owner) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setGroup(group);
        expense.setOwner(owner);
        return expense;
    }
}
