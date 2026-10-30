package kluniversity.com.expense.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendExpenseReminder(
            String toEmail,
            String recipientName,
            String expenseDescription,
            String groupName,
            String paidByName,
            double amount) {

        if (toEmail == null || toEmail.isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("Expense Sharing - Payment Reminder");

        message.setText(
                "Hi " + recipientName + ",\n\n"
                + paidByName + " added an expense in the group "
                + groupName + ".\n\n"
                + "Expense: " + expenseDescription + "\n"
                + "Amount you owe: ₹"
                + String.format("%.2f", amount)
                + "\n\nPlease open the Expense Sharing app "
                + "to view the expense and settle your share.\n\n"
                + "Thank you,\n"
                + "Expense Sharing App"
        );

        mailSender.send(message);
    }
}