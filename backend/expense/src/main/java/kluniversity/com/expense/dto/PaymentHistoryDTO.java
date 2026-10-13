package kluniversity.com.expense.dto;

import java.time.LocalDateTime;

public record PaymentHistoryDTO(
        Long id,
        String description,
        String group,
        String paidTo,
        Double amount,
        LocalDateTime paidAt,
        String paymentId
) {
}
