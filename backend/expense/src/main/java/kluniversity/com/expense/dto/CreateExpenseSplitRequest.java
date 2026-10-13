package kluniversity.com.expense.dto;

public class CreateExpenseSplitRequest {

    private Long userId;
    private Double amount;

    public CreateExpenseSplitRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}