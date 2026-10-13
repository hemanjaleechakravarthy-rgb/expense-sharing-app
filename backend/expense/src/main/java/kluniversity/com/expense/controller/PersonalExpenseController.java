package kluniversity.com.expense.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.CreateExpenseRequest;
import kluniversity.com.expense.entity.Expense;
import kluniversity.com.expense.service.ExpenseService;

@RestController
@RequestMapping("/api/personal/expenses")
@CrossOrigin(origins = "http://localhost:5173")
public class PersonalExpenseController {

    private final ExpenseService expenseService;

    public PersonalExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<Expense> getPersonalExpenses(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return expenseService.getPersonalExpenses(authentication.getName());
    }

    @PostMapping
    public Expense createPersonalExpense(
            @RequestBody CreateExpenseRequest request,
            Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return expenseService.createPersonalExpense(
                request, authentication.getName());
    }
}
