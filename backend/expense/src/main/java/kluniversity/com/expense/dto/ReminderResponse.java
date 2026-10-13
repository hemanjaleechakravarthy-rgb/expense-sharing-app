package kluniversity.com.expense.dto;

public class ReminderResponse {

    private Long splitId;
    private String fromUser;
    private String toUser;
    private String fromEmail;
    private String toEmail;
    private Double amount;
    private String message;

    public ReminderResponse() {
    }

    public ReminderResponse(
            Long splitId,
            String fromUser,
            String toUser,
            String fromEmail,
            String toEmail,
            Double amount,
            String message) {

        this.splitId = splitId;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.fromEmail = fromEmail;
        this.toEmail = toEmail;
        this.amount = amount;
        this.message = message;
    }

    public Long getSplitId() {
        return splitId;
    }

    public void setSplitId(Long splitId) {
        this.splitId = splitId;
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

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}