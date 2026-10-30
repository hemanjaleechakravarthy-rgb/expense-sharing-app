package kluniversity.com.expense.service;

import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.CreateExpenseRequest;
import kluniversity.com.expense.dto.ExpenseShareRequest;
import kluniversity.com.expense.dto.TransactionDTO;
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
public class ExpenseService {

    private static final Logger LOGGER =
            Logger.getLogger(ExpenseService.class.getName());
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ExpenseService(
        ExpenseRepository expenseRepository,
        ExpenseSplitRepository expenseSplitRepository,
        GroupRepository groupRepository,
        GroupMemberRepository groupMemberRepository,
        UserRepository userRepository,
        EmailService emailService) {

    this.expenseRepository = expenseRepository;
    this.expenseSplitRepository = expenseSplitRepository;
    this.groupRepository = groupRepository;
    this.groupMemberRepository = groupMemberRepository;
    this.userRepository = userRepository;
    this.emailService = emailService;
}

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"));
    }

    private User getAuthorizedGroupMember(Long groupId, String email) {

        User user = getUserByEmail(email);

        if (!groupRepository.existsById(groupId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Group not found");
        }

        if (!groupMemberRepository.existsByGroupIdAndUserId(
                groupId, user.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this group");
        }

        return user;
    }

    public Expense createExpense(
            Long groupId,
            CreateExpenseRequest request,
            String email) {

        User owner = getAuthorizedGroupMember(groupId, email);

        if (request == null
                || request.getDescription() == null
                || request.getDescription().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Description is required");
        }

        if (request.getAmount() == null
                || !Double.isFinite(request.getAmount())
                || request.getAmount() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Amount must be greater than zero");
        }

        requireTwoDecimalAmount(request.getAmount());

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Group not found"));

        if (request.getPaidByUserId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payer is required");
        }

        User paidBy = userRepository.findById(
                request.getPaidByUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payer not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(
                groupId, paidBy.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payer must be a member of this group");
        }

        Expense expense = new Expense();

        expense.setDescription(request.getDescription().trim());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(paidBy);
        expense.setGroup(group);
        expense.setOwner(owner);

        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense createEqualSplitExpense(
            Long groupId,
            CreateExpenseRequest request,
            String email,
            String submissionKey) {

        User owner = getAuthorizedGroupMember(groupId, email);

        if (submissionKey == null
                || submissionKey.isBlank()
                || submissionKey.length() > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid submission key is required");
        }

        if (request == null
                || request.getDescription() == null
                || request.getDescription().isBlank()
                || request.getAmount() == null
                || !Double.isFinite(request.getAmount())
                || request.getAmount() <= 0
                || request.getPaidByUserId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Description, positive amount, and payer are required");
        }

        requireTwoDecimalAmount(request.getAmount());

        long totalCents = BigDecimal.valueOf(request.getAmount())
                .movePointRight(2)
                .longValueExact();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Group not found"));

        User paidBy = userRepository.findById(
                request.getPaidByUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payer not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(
                groupId, paidBy.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payer must be a member of this group");
        }

        List<User> participants = groupMemberRepository
                .findByGroupId(groupId)
                .stream()
                .map(member -> member.getUser())
                .filter(user -> user != null)
                .sorted(Comparator.comparing(User::getId))
                .distinct()
                .toList();

        if (participants.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The group has no members");
        }

        /*
         * Calculate split amounts.
         *
         * Custom splits are used when provided.
         * Otherwise, the original equal-split logic is used.
         */
        Map<Long, Long> splitAmounts = new LinkedHashMap<>();

        List<ExpenseShareRequest> requestedSplits =
                request.getSplits();

        if (requestedSplits != null && !requestedSplits.isEmpty()) {

            if (requestedSplits.size() != participants.size()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A split amount is required for every group member");
            }

            for (ExpenseShareRequest requestedSplit : requestedSplits) {

                if (requestedSplit == null
                        || requestedSplit.getUserId() == null
                        || requestedSplit.getAmount() == null
                        || !Double.isFinite(
                                requestedSplit.getAmount())
                        || requestedSplit.getAmount() < 0) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Each split must have a valid user and amount");
                }

                requireTwoDecimalAmount(
                        requestedSplit.getAmount());

                Long userId = requestedSplit.getUserId();

                boolean isGroupMember = participants.stream()
                        .anyMatch(user ->
                                user.getId().equals(userId));

                if (!isGroupMember) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "All split users must belong to this group");
                }

                long amountCents = BigDecimal
                        .valueOf(requestedSplit.getAmount())
                        .movePointRight(2)
                        .longValueExact();

                if (splitAmounts.putIfAbsent(
                        userId, amountCents) != null) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Duplicate user in expense splits");
                }
            }

            long splitTotalCents = splitAmounts.values()
                    .stream()
                    .mapToLong(Long::longValue)
                    .sum();

            if (splitTotalCents != totalCents) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The sum of split amounts must equal the expense total");
            }

        } else {

            // Original equal-split calculation.
            long baseCents = totalCents / participants.size();
            long extraCents = totalCents % participants.size();

            for (int index = 0;
                    index < participants.size();
                    index++) {

                long amountCents =
                        baseCents + (index < extraCents ? 1 : 0);

                splitAmounts.put(
                        participants.get(index).getId(),
                        amountCents);
            }
        }

        /*
         * Idempotency check.
         */
        var existing = expenseRepository.findBySubmissionKey(
                submissionKey);

        if (existing.isPresent()) {

            Expense previous = existing.get();

            boolean basicDetailsMatch =
                    previous.getGroup() != null
                    && previous.getGroup().getId().equals(groupId)
                    && previous.getOwner() != null
                    && previous.getOwner().getId().equals(owner.getId())
                    && previous.getDescription() != null
                    && previous.getDescription().equals(
                            request.getDescription().trim())
                    && previous.getAmount() != null
                    && Double.compare(
                            previous.getAmount(),
                            request.getAmount()) == 0
                    && previous.getPaidBy() != null
                    && Objects.equals(
                            previous.getPaidBy().getId(),
                            request.getPaidByUserId());

            if (!basicDetailsMatch) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Submission key is already in use");
            }

            List<ExpenseSplit> previousSplits =
                    expenseSplitRepository.findByExpenseId(
                            previous.getId());

            boolean previousSplitsMatch =
                    previousSplits.size() == splitAmounts.size()
                    && previousSplits.stream().allMatch(split -> {

                        if (split.getUser() == null
                                || split.getAmount() == null) {
                            return false;
                        }

                        Long userId = split.getUser().getId();

                        if (!splitAmounts.containsKey(userId)) {
                            return false;
                        }

                        long previousAmountCents = BigDecimal
                                .valueOf(split.getAmount())
                                .movePointRight(2)
                                .longValueExact();

                        return previousAmountCents
                                == splitAmounts.get(userId);
                    });

            if (!previousSplitsMatch) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Submission key is already in use");
            }

            return previous;
        }

        /*
         * Create the expense.
         */
        Expense expense = new Expense();

        expense.setDescription(
                request.getDescription().trim());

        expense.setAmount(request.getAmount());
        expense.setPaidBy(paidBy);
        expense.setGroup(group);
        expense.setOwner(owner);
        expense.setSubmissionKey(submissionKey);

        Expense savedExpense = expenseRepository.save(expense);

        /*
         * Create expense splits.
         *
         * FIX:
         * The participant who paid the original expense
         * is marked as paid automatically.
         *
         * Other participants remain pending.
         */
        List<ExpenseSplit> newSplits = new ArrayList<>();

        for (User participant : participants) {

            Long userId = participant.getId();
            Long amountCents = splitAmounts.get(userId);

            ExpenseSplit split = new ExpenseSplit();

            split.setExpense(savedExpense);
            split.setUser(participant);
            split.setAmount(amountCents / 100.0);

            // Check whether this participant paid the expense.
            boolean alreadyPaid =
                    paidBy.getId().equals(participant.getId());

            split.setPaid(alreadyPaid);

            if (alreadyPaid) {
                split.setPaidAt(LocalDateTime.now());
            } else {
                split.setPaidAt(null);
            }

            newSplits.add(split);
        }

        expenseSplitRepository.saveAll(newSplits);

// Send email reminders to participants who still owe their share.
for (ExpenseSplit split : newSplits) {

    // Skip the payer and participants without an email address.
    if (Boolean.TRUE.equals(split.getPaid())
            || split.getUser() == null
            || split.getUser().getEmail() == null
            || split.getUser().getEmail().isBlank()) {

        continue;
    }

    try {
        emailService.sendExpenseReminder(
                split.getUser().getEmail(),
                split.getUser().getName(),
                savedExpense.getDescription(),
                savedExpense.getGroup().getName(),
                savedExpense.getPaidBy().getName(),
                split.getAmount());

    } catch (Exception exception) {

        LOGGER.log(
                Level.SEVERE,
                "Could not send expense reminder email to "
                        + split.getUser().getEmail(),
                exception);
    }
}

return savedExpense;
    }

    private void requireTwoDecimalAmount(double amount) {

        try {
            BigDecimal.valueOf(amount)
                    .setScale(2, RoundingMode.UNNECESSARY);

        } catch (ArithmeticException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Amount must have at most two decimal places");
        }
    }

    public List<Expense> getExpenses(
            Long groupId,
            String email) {

        User user = getAuthorizedGroupMember(groupId, email);

        return visibleExpenses(groupId, user);
    }

    public List<TransactionDTO> getTransactions(
            Long groupId,
            String email) {

        User user = getAuthorizedGroupMember(groupId, email);

        return visibleExpenses(groupId, user)
                .stream()
                .map(expense -> {

                    List<ExpenseSplit> splits =
                            expenseSplitRepository.findByExpenseId(
                                    expense.getId());

                    List<TransactionDTO.Participant> participants =
                            splits.stream()
                                    .filter(split ->
                                            split.getUser() != null)
                                    .map(split ->
                                            new TransactionDTO.Participant(
                                                    split.getUser().getName(),
                                                    split.getAmount(),
                                                    Boolean.TRUE.equals(
                                                            split.getPaid())))
                                    .toList();

                    String paymentStatus = splits.isEmpty()
                            ? "NOT_SPLIT"
                            : splits.stream().allMatch(
                                    split -> Boolean.TRUE.equals(
                                            split.getPaid()))
                                    ? "PAID"
                                    : "PENDING";

                    return new TransactionDTO(
                            expense.getId(),
                            expense.getCreatedAt(),
                            expense.getAmount(),
                            expense.getDescription(),
                            expense.getGroup().getName(),
                            expense.getPaidBy() == null
                                    ? "Unknown"
                                    : expense.getPaidBy().getName(),
                            participants,
                            paymentStatus);
                })
                .toList();
    }

    public List<Expense> getPersonalExpenses(String email) {

        User user = getUserByEmail(email);

        return expenseRepository.findByOwnerIdAndGroupIsNull(
                user.getId());
    }

    @Transactional
    public Expense createPersonalExpense(
            CreateExpenseRequest request,
            String email) {

        User owner = getUserByEmail(email);

        if (request.getDescription() == null
                || request.getDescription().isBlank()
                || request.getAmount() == null
                || !Double.isFinite(request.getAmount())
                || request.getAmount() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A description and positive amount are required");
        }

        requireTwoDecimalAmount(request.getAmount());

        Expense expense = new Expense();

        expense.setDescription(
                request.getDescription().trim());

        expense.setAmount(request.getAmount());
        expense.setOwner(owner);
        expense.setPaidBy(owner);

        return expenseRepository.save(expense);
    }

    private List<Expense> visibleExpenses(
            Long groupId,
            User user) {

        return expenseRepository.findByGroupId(groupId)
                .stream()
                .filter(expense -> {

                    if (expense.getOwner() != null
                            && expense.getOwner().getId().equals(
                                    user.getId())) {
                        return true;
                    }

                    return expenseSplitRepository
                            .findByExpenseId(expense.getId())
                            .stream()
                            .anyMatch(split ->
                                    split.getUser() != null
                                    && split.getUser().getId().equals(
                                            user.getId()));
                })
                .collect(Collectors.toList());
    }
}