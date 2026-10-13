package kluniversity.com.expense.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kluniversity.com.expense.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}