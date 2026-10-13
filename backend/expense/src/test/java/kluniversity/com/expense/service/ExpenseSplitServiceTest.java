package kluniversity.com.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.repository.ExpenseRepository;
import kluniversity.com.expense.repository.ExpenseSplitRepository;
import kluniversity.com.expense.repository.GroupMemberRepository;
import kluniversity.com.expense.repository.UserRepository;

class ExpenseSplitServiceTest {

    @Test
    void manualMarkPaidRouteCannotBypassProviderVerification() {
        ExpenseSplitRepository splits = mock(ExpenseSplitRepository.class);
        ExpenseSplitService service = new ExpenseSplitService(
                splits,
                mock(ExpenseRepository.class),
                mock(UserRepository.class),
                mock(GroupMemberRepository.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.markAsPaid(17L, "payer@example.test"));

        assertEquals(400, exception.getStatusCode().value());
        verify(splits, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
