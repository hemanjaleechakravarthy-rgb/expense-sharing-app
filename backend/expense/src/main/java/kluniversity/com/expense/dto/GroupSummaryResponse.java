package kluniversity.com.expense.dto;

public class GroupSummaryResponse {

    private Long groupId;
    private String groupName;
    private Double totalExpenses;
    private Integer totalExpenseCount;

    private Double myContributions;
    private Double myShare;
    private Double myPendingPayments;

    public GroupSummaryResponse() {
    }

    public GroupSummaryResponse(
            Long groupId,
            String groupName,
            Double totalExpenses,
            Integer totalExpenseCount) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.totalExpenses = totalExpenses;
        this.totalExpenseCount = totalExpenseCount;
        this.myContributions = 0.0;
        this.myShare = 0.0;
        this.myPendingPayments = 0.0;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Double getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(Double totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public Integer getTotalExpenseCount() {
        return totalExpenseCount;
    }

    public void setTotalExpenseCount(Integer totalExpenseCount) {
        this.totalExpenseCount = totalExpenseCount;
    }

    public Double getMyContributions() {
        return myContributions;
    }

    public void setMyContributions(Double myContributions) {
        this.myContributions = myContributions;
    }

    public Double getMyShare() {
        return myShare;
    }

    public void setMyShare(Double myShare) {
        this.myShare = myShare;
    }

    public Double getMyPendingPayments() {
        return myPendingPayments;
    }

    public void setMyPendingPayments(Double myPendingPayments) {
        this.myPendingPayments = myPendingPayments;
    }
}