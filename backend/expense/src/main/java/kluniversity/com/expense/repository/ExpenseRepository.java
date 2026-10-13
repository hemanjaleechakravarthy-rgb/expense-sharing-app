package kluniversity.com.expense.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kluniversity.com.expense.entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Fetch expenses belonging to a group
    List<Expense> findByGroupId(Long groupId);

    // Fetch expenses paid by a user
    List<Expense> findByPaidById(Long userId);

    List<Expense> findByOwnerIdAndGroupIsNull(Long ownerId);

    java.util.Optional<Expense> findBySubmissionKey(String submissionKey);
}