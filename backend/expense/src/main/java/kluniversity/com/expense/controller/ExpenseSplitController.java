package kluniversity.com.expense.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.CreateExpenseSplitRequest;
import kluniversity.com.expense.entity.ExpenseSplit;
import kluniversity.com.expense.service.ExpenseSplitService;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "http://localhost:5173")
public class ExpenseSplitController {

    private final ExpenseSplitService expenseSplitService;

    public ExpenseSplitController(ExpenseSplitService expenseSplitService) {
        this.expenseSplitService = expenseSplitService;
    }

    private String getEmail(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return authentication.getName();
    }

    @PostMapping("/{expenseId}/splits")
    public List<ExpenseSplit> createSplits(
            @PathVariable Long expenseId,
            @RequestBody List<CreateExpenseSplitRequest> requests,
            Authentication authentication) {

        return expenseSplitService.createSplits(
                expenseId, requests, getEmail(authentication));
    }

    @GetMapping("/{expenseId}/splits")
    public List<ExpenseSplit> getSplits(
            @PathVariable Long expenseId,
            Authentication authentication) {

        return expenseSplitService.getSplits(
                expenseId, getEmail(authentication));
    }

    @PutMapping("/splits/{splitId}/pay")
    public ExpenseSplit markAsPaid(
            @PathVariable Long splitId,
            Authentication authentication) {

        return expenseSplitService.markAsPaid(
                splitId, getEmail(authentication));
    }
}