
package kluniversity.com.expense.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.CreateExpenseRequest;
import kluniversity.com.expense.entity.Expense;
import kluniversity.com.expense.service.ExpenseService;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:5173")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/{groupId}/expenses")
    public Expense createExpense(
            @PathVariable Long groupId,
            @RequestBody CreateExpenseRequest request,
            Authentication authentication) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in first");
        }

        return expenseService.createExpense(
                groupId, request, authentication.getName());
    }

    @PostMapping("/{groupId}/expenses/equal-split")
    public Expense createEqualSplitExpense(
            @PathVariable Long groupId,
            @RequestHeader("Idempotency-Key") String submissionKey,
            @RequestBody CreateExpenseRequest request,
            Authentication authentication) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return expenseService.createEqualSplitExpense(
                groupId, request, authentication.getName(), submissionKey);
    }

    @GetMapping("/{groupId}/expenses")
    public List<Expense> getExpenses(
            @PathVariable Long groupId,
            Authentication authentication) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in first");
        }

        return expenseService.getExpenses(
                groupId, authentication.getName());
    }

    @GetMapping("/{groupId}/transactions")
    public List<kluniversity.com.expense.dto.TransactionDTO> getTransactions(
            @PathVariable Long groupId,
            Authentication authentication) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return expenseService.getTransactions(
                groupId, authentication.getName());
    }
}
