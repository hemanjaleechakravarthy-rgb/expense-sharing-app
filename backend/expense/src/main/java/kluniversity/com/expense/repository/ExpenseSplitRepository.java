
package kluniversity.com.expense.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kluniversity.com.expense.entity.ExpenseSplit;

public interface ExpenseSplitRepository
        extends JpaRepository<ExpenseSplit, Long> {

    boolean existsByExpenseId(Long expenseId);

    List<ExpenseSplit> findByExpenseId(Long expenseId);

    List<ExpenseSplit> findByExpenseGroupId(Long groupId);

    List<ExpenseSplit> findByUserIdAndExpenseGroupId(
            Long userId, Long groupId);

    List<ExpenseSplit> findByUserIdAndExpenseGroupIdAndPaidTrueOrderByPaidAtDesc(
            Long userId, Long groupId);

    // NEW: Fetch all unpaid splits for a user across every group
    List<ExpenseSplit> findByUserIdAndPaidFalse(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select split from ExpenseSplit split where split.id = :splitId")
    java.util.Optional<ExpenseSplit> findByIdForUpdate(
            @Param("splitId") Long splitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select split from ExpenseSplit split where split.razorpayOrderId = :orderId")
    java.util.Optional<ExpenseSplit> findByRazorpayOrderIdForUpdate(
            @Param("orderId") String orderId);
}
