package kluniversity.com.expense.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.CreateExpenseSplitRequest;
import kluniversity.com.expense.entity.Expense;
import kluniversity.com.expense.entity.ExpenseSplit;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.ExpenseRepository;
import kluniversity.com.expense.repository.ExpenseSplitRepository;
import kluniversity.com.expense.repository.GroupMemberRepository;
import kluniversity.com.expense.repository.UserRepository;

@Service
public class ExpenseSplitService {

    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    public ExpenseSplitService(
            ExpenseSplitRepository expenseSplitRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            GroupMemberRepository groupMemberRepository) {
        this.expenseSplitRepository = expenseSplitRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Expense getExpense(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private boolean isOwner(Expense expense, User user) {
        return expense.getOwner() != null
                && expense.getOwner().getId().equals(user.getId());
    }

    private void authorizeExpenseAccess(Expense expense, User user) {
        if (isOwner(expense, user)) {
            return;
        }

        boolean isParticipant = expenseSplitRepository
                .findByExpenseId(expense.getId())
                .stream()
                .anyMatch(split -> split.getUser() != null
                        && split.getUser().getId().equals(user.getId()));

        if (!isParticipant) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this expense");
        }
    }

    @Transactional
    public List<ExpenseSplit> createSplits(
            Long expenseId,
            List<CreateExpenseSplitRequest> requests,
            String email) {

        User creator = getUser(email);
        Expense expense = getExpense(expenseId);

        if (!isOwner(expense, creator)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the expense owner can create splits");
        }

        if (expense.getGroup() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Personal expenses cannot be split into a group");
        }

        if (expense.getAmount() == null
                || !Double.isFinite(expense.getAmount())
                || expense.getAmount() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Expense amount is invalid");
        }

        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one split is required");
        }

        if (expenseSplitRepository.existsByExpenseId(expenseId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Splits already exist for this expense");
        }

        Set<Long> participantIds = new HashSet<>();
        long totalSplitCents = 0;

        for (CreateExpenseSplitRequest request : requests) {
            if (request.getUserId() == null
                    || request.getAmount() == null
                    || !Double.isFinite(request.getAmount())
                    || request.getAmount() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each split needs a user and a positive amount");
            }

            if (!participantIds.add(request.getUserId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A user cannot appear more than once in the split");
            }

            User participant = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Split participant not found"));

            if (!groupMemberRepository.existsByGroupIdAndUserId(
                    expense.getGroup().getId(), participant.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Every participant must belong to the expense group");
            }

            try {
                totalSplitCents += BigDecimal.valueOf(request.getAmount())
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .movePointRight(2)
                        .longValueExact();
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Split amounts must have at most two decimal places");
            }
        }

        long expenseCents;
        try {
            expenseCents = BigDecimal.valueOf(expense.getAmount())
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expense amount must have at most two decimal places");
        }
        if (totalSplitCents != expenseCents) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Split amounts must equal the expense amount");
        }

        List<ExpenseSplit> splits = new ArrayList<>();

        for (CreateExpenseSplitRequest request : requests) {
            User participant = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Split participant not found"));

            ExpenseSplit split = new ExpenseSplit();
            split.setExpense(expense);
            split.setUser(participant);
            split.setAmount(request.getAmount());
            split.setPaid(false);
            splits.add(split);
        }

        return expenseSplitRepository.saveAll(splits);
    }

    public List<ExpenseSplit> getSplits(Long expenseId, String email) {
        User user = getUser(email);
        Expense expense = getExpense(expenseId);
        authorizeExpenseAccess(expense, user);
        return expenseSplitRepository.findByExpenseId(expenseId);
    }

    public ExpenseSplit markAsPaid(Long splitId, String email) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A split can only be marked paid after payment verification");
    }
}