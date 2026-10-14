package kluniversity.com.expense.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NEW: Store the Razorpay payment ID uniquely
    @Column(unique = true)
    private String razorpayPaymentId;

    @ManyToOne
    private User fromUser;

    @ManyToOne
    private User toUser;

    private double amount;

    private String status;

    private LocalDate date;

    public Long getId() {
        return id;
    }

    // NEW: Getter for Razorpay payment ID
    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public User getFromUser() {
        return fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // NEW: Setter for Razorpay payment ID
    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}