package kluniversity.com.expense.dto;

public class BalanceResponse {

    private String fromUser;
    private String toUser;
    private Double amount;
    private Boolean paid;

    public BalanceResponse() {
    }

    public BalanceResponse(
            String fromUser,
            String toUser,
            Double amount,
            Boolean paid) {

        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
        this.paid = paid;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }
}