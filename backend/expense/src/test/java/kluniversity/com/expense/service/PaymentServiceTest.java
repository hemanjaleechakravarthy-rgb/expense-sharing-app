package kluniversity.com.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.CreatePaymentRequest;
import kluniversity.com.expense.dto.VerifyPaymentRequest;
import kluniversity.com.expense.entity.ExpenseSplit;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.ExpenseSplitRepository;
import kluniversity.com.expense.repository.UserRepository;
import kluniversity.com.expense.repository.PaymentRepository;
import com.razorpay.Utils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

   @Mock private ExpenseSplitRepository expenseSplitRepository;
@Mock private UserRepository userRepository;
@Mock private PaymentRepository paymentRepository;
@Mock private RazorpayGateway razorpayGateway;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                expenseSplitRepository, userRepository, paymentRepository, razorpayGateway,
                "", "");
    }

    @Test
    void createOrderRejectsUsersOtherThanAssignedParticipant() {
        User requester = user(1L, "requester@example.test");
        User participant = user(2L, "participant@example.test");
        ExpenseSplit split = new ExpenseSplit();
        split.setId(42L);
        split.setAmount(25.00);
        split.setUser(participant);

        when(expenseSplitRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(split));
        when(userRepository.findByEmail(requester.getEmail()))
                .thenReturn(Optional.of(requester));

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setSplitId(42L);
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.createOrder(request, requester.getEmail()));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void createOrderRequiresServerCredentialsAndDoesNotCallProvider() {
        User requester = user(1L, "requester@example.test");
        ExpenseSplit split = new ExpenseSplit();
        split.setId(42L);
        split.setAmount(25.00);
        split.setUser(requester);

        when(expenseSplitRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(split));
        when(userRepository.findByEmail(requester.getEmail()))
                .thenReturn(Optional.of(requester));

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setSplitId(42L);
        PaymentService noCredentialsService = new PaymentService(
        expenseSplitRepository,
        userRepository,
        paymentRepository,
        new RazorpayGateway("", ""),
        "",
        "");
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noCredentialsService.createOrder(
                        request, requester.getEmail()));

        assertEquals(503, exception.getStatusCode().value());
        verify(expenseSplitRepository).findByIdForUpdate(42L);
    }

    @Test
    void signedCaptureWebhookRecoversPaymentAndIsIdempotent() throws Exception {
        User participant = user(1L, "participant@example.test");
        ExpenseSplit split = split(participant);
        PaymentService configuredService = configuredService();
        when(expenseSplitRepository.findByRazorpayOrderIdForUpdate("order_42"))
                .thenReturn(Optional.of(split));
        when(razorpayGateway.fetchOrder("order_42"))
                .thenReturn(orderJson());
        when(razorpayGateway.fetchPayment("pay_42"))
                .thenReturn(paymentJson());

        String body = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_42","order_id":"order_42"}}}}
                """.trim();
        String signature = Utils.getHash(body, "test-webhook-secret");

        var firstResult = configuredService.handleWebhook(body, signature);
        var retryResult = configuredService.handleWebhook(body, signature);

        assertEquals(true, firstResult.get("processed"));
        assertEquals(false, firstResult.get("alreadyProcessed"));
        assertEquals(true, retryResult.get("alreadyProcessed"));
        assertEquals(true, split.getPaid());
        assertEquals("pay_42", split.getRazorpayPaymentId());
        verify(expenseSplitRepository, org.mockito.Mockito.times(1)).save(split);
    }

    @Test
    void invalidWebhookSignatureDoesNotLookupOrUpdatePayment() {
        PaymentService configuredService = configuredService();
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configuredService.handleWebhook(
                        "{\"event\":\"payment.captured\"}", "invalid"));

        assertEquals(400, exception.getStatusCode().value());
        verify(expenseSplitRepository,
                org.mockito.Mockito.never())
                .findByRazorpayOrderIdForUpdate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void clientVerificationRejectsInvalidPaymentSignatureBeforeProviderLookup() {
        User participant = user(1L, "participant@example.test");
        ExpenseSplit split = split(participant);
        PaymentService configuredService = configuredService();
        when(expenseSplitRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(split));
        when(userRepository.findByEmail(participant.getEmail()))
                .thenReturn(Optional.of(participant));
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setSplitId(42L);
        request.setRazorpayOrderId("order_42");
        request.setRazorpayPaymentId("pay_42");
        request.setRazorpaySignature("invalid-signature");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configuredService.verifyPayment(
                        request, participant.getEmail()));

        assertEquals(400, exception.getStatusCode().value());
        verify(razorpayGateway, org.mockito.Mockito.never())
                .fetchOrder(org.mockito.ArgumentMatchers.anyString());
        verify(expenseSplitRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any(ExpenseSplit.class));
    }

    @Test
    void authenticatedStatusRetryConfirmsCapturedPayment() throws Exception {
        User participant = user(1L, "participant@example.test");
        ExpenseSplit split = split(participant);
        PaymentService configuredService = configuredService();
        when(expenseSplitRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(split));
        when(userRepository.findByEmail(participant.getEmail()))
                .thenReturn(Optional.of(participant));
        when(razorpayGateway.fetchOrder("order_42"))
                .thenReturn(orderJson());
        when(razorpayGateway.fetchPaymentsForOrder("order_42"))
                .thenReturn(List.of(paymentJson()));
        when(razorpayGateway.fetchPayment("pay_42"))
                .thenReturn(paymentJson());

        var result = configuredService.getPaymentStatus(42L, participant.getEmail());

        assertEquals(true, result.get("paid"));
        assertEquals(true, split.getPaid());
        assertEquals("pay_42", split.getRazorpayPaymentId());
        verify(expenseSplitRepository).save(split);
    }

    @Test
    void statusRetryRejectsUnassignedParticipant() {
        User requester = user(1L, "requester@example.test");
        ExpenseSplit split = split(user(2L, "participant@example.test"));
        when(expenseSplitRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(split));
        when(userRepository.findByEmail(requester.getEmail()))
                .thenReturn(Optional.of(requester));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.getPaymentStatus(42L, requester.getEmail()));

        assertEquals(403, exception.getStatusCode().value());
        verify(razorpayGateway, org.mockito.Mockito.never())
                .fetchOrder(org.mockito.ArgumentMatchers.anyString());
    }

    private ExpenseSplit split(User participant) {
        ExpenseSplit split = new ExpenseSplit();
        split.setId(42L);
        split.setAmount(25.00);
        split.setUser(participant);
        split.setRazorpayOrderId("order_42");
        return split;
    }

    private PaymentService configuredService() {
        return new PaymentService(
        expenseSplitRepository,
        userRepository,
        paymentRepository,
        razorpayGateway,
        "test-key-secret",
        "test-webhook-secret");
    }

    private JSONObject orderJson() throws org.json.JSONException {
        return new JSONObject()
                .put("receipt", "split_42")
                .put("amount", 2500)
                .put("currency", "INR")
                .put("status", "paid");
    }

    private JSONObject paymentJson() throws org.json.JSONException {
        return new JSONObject()
                .put("id", "pay_42")
                .put("order_id", "order_42")
                .put("amount", 2500)
                .put("currency", "INR")
                .put("status", "captured");
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}
