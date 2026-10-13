package kluniversity.com.expense.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TransactionDTO(
        Long id,
        LocalDateTime dateTime,
        Double amount,
        String description,
        String group,
        String payer,
        List<Participant> participants,
        String paymentStatus) {

    public record Participant(String name, Double share, boolean paid) {
    }
}
