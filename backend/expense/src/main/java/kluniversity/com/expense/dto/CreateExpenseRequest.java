
package kluniversity.com.expense.dto;

import java.util.List;

public class CreateExpenseRequest {

    private String description;
    private Double amount;
    private Long paidByUserId;
    private List<ExpenseShareRequest> splits;

    public CreateExpenseRequest() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Long getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public List<ExpenseShareRequest> getSplits() {
        return splits;
    }

    public void setSplits(List<ExpenseShareRequest> splits) {
        this.splits = splits;
    }
}
