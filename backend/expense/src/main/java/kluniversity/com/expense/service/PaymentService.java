package kluniversity.com.expense.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.razorpay.Utils;

import kluniversity.com.expense.dto.CreatePaymentRequest;
import kluniversity.com.expense.dto.VerifyPaymentRequest;
import kluniversity.com.expense.dto.PaymentHistoryDTO;
import kluniversity.com.expense.entity.Expense;
import kluniversity.com.expense.entity.ExpenseSplit;
import kluniversity.com.expense.entity.Payment;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.ExpenseSplitRepository;
import kluniversity.com.expense.repository.PaymentRepository;
import kluniversity.com.expense.repository.UserRepository;

@Service
public class PaymentService {

    private final ExpenseSplitRepository expenseSplitRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayGateway razorpayGateway;
    private final String keySecret;
    private final String webhookSecret;

    @Autowired
    public PaymentService(
            ExpenseSplitRepository expenseSplitRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            RazorpayGateway razorpayGateway,
            @Value("${razorpay.key.secret:}") String keySecret,
            @Value("${razorpay.webhook.secret:}") String webhookSecret) {
        this.expenseSplitRepository = expenseSplitRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.razorpayGateway = razorpayGateway;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public Map<String, Object> createOrder(
            CreatePaymentRequest request, String email) {
        if (request == null || request.getSplitId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Expense split is required");
        }

        ExpenseSplit split = expenseSplitRepository
                .findByIdForUpdate(request.getSplitId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Expense split not found"));

        requireAssignedParticipant(split, email);

        if (Boolean.TRUE.equals(split.getPaid())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This expense split is already paid");
        }

        long amountInPaise = amountInPaise(split);

        if (split.getRazorpayOrderId() != null) {
            JSONObject existingOrder =
                    razorpayGateway.fetchOrder(split.getRazorpayOrderId());

            validateOrder(existingOrder, split, amountInPaise);

            if ("paid".equals(existingOrder.optString("status"))) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This order is already paid; check payment status before retrying");
            }

            return orderResponse(split.getRazorpayOrderId(), amountInPaise);
        }

        String orderId = razorpayGateway.createOrder(
                amountInPaise, "INR", "split_" + split.getId());

        split.setRazorpayOrderId(orderId);
        expenseSplitRepository.save(split);

        return orderResponse(orderId, amountInPaise);
    }

    @Transactional
    public Map<String, Object> verifyPayment(
            VerifyPaymentRequest request, String email) {
        if (request == null
                || request.getSplitId() == null
                || isBlank(request.getRazorpayOrderId())
                || isBlank(request.getRazorpayPaymentId())
                || isBlank(request.getRazorpaySignature())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment verification data is incomplete");
        }

        ExpenseSplit split = expenseSplitRepository
                .findByIdForUpdate(request.getSplitId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Expense split not found"));

        requireAssignedParticipant(split, email);
        requireKeySecret();

        try {
            JSONObject signatureAttributes = new JSONObject();
            signatureAttributes.put(
                    "razorpay_order_id", request.getRazorpayOrderId());
            signatureAttributes.put(
                    "razorpay_payment_id", request.getRazorpayPaymentId());
            signatureAttributes.put(
                    "razorpay_signature", request.getRazorpaySignature());

            if (!Utils.verifyPaymentSignature(signatureAttributes, keySecret)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid payment signature");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Payment signature is invalid");
        }

        return confirmCapturedPayment(
                split,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId());
    }

    @Transactional
    public Map<String, Object> getPaymentStatus(Long splitId, String email) {
        if (splitId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Expense split is required");
        }

        ExpenseSplit split = expenseSplitRepository.findByIdForUpdate(splitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Expense split not found"));

        requireAssignedParticipant(split, email);

        if (Boolean.TRUE.equals(split.getPaid())) {
            return statusResponse(true, "PAID");
        }

        if (isBlank(split.getRazorpayOrderId())) {
            return statusResponse(false, "PENDING");
        }

        return synchronizeCapturedPayment(split);
    }

    @Transactional
    public Map<String, Object> handleWebhook(String body, String signature) {
        if (isBlank(body) || isBlank(signature)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Webhook payload or signature is missing");
        }

        if (isBlank(webhookSecret)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay webhook secret is not configured on the server");
        }

        try {
            if (!Utils.verifyWebhookSignature(body, signature, webhookSecret)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid Razorpay webhook signature");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Razorpay webhook signature is invalid");
        }

        try {
            JSONObject event = new JSONObject(body);

            if (!"payment.captured".equals(event.optString("event"))) {
                return Map.of("received", true, "processed", false);
            }

            JSONObject paymentEntity = event
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String orderId = paymentEntity.getString("order_id");
            String paymentId = paymentEntity.getString("id");

            ExpenseSplit split = expenseSplitRepository
                    .findByRazorpayOrderIdForUpdate(orderId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No expense split is associated with this Razorpay order"));

            Map<String, Object> result =
                    confirmCapturedPayment(split, orderId, paymentId);

            return Map.of(
                    "received", true,
                    "processed", true,
                    "alreadyProcessed",
                    Boolean.TRUE.equals(result.get("alreadyProcessed")));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (JSONException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Razorpay webhook payload is invalid");
        }
    }

    private Map<String, Object> synchronizeCapturedPayment(
            ExpenseSplit split) {
        String orderId = split.getRazorpayOrderId();
        long expectedAmount = amountInPaise(split);

        JSONObject order = razorpayGateway.fetchOrder(orderId);
        validateOrder(order, split, expectedAmount);

        if (!"paid".equals(order.optString("status"))) {
            return statusResponse(false, "PENDING");
        }

        List<JSONObject> payments =
                razorpayGateway.fetchPaymentsForOrder(orderId);

        JSONObject capturedPayment = payments.stream()
                .filter(payment ->
                        "captured".equals(payment.optString("status")))
                .findFirst()
                .orElse(null);

        if (capturedPayment == null) {
            return statusResponse(false, "PENDING");
        }

        return confirmCapturedPayment(
                split, orderId, capturedPayment.optString("id"));
    }

    private Map<String, Object> confirmCapturedPayment(
            ExpenseSplit split, String orderId, String paymentId) {
        if (isBlank(split.getRazorpayOrderId())
                || !split.getRazorpayOrderId().equals(orderId)
                || isBlank(paymentId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment order does not match this expense split");
        }

        long expectedAmount = amountInPaise(split);

        JSONObject order = razorpayGateway.fetchOrder(orderId);
        validateOrder(order, split, expectedAmount);

        JSONObject razorpayPayment =
                razorpayGateway.fetchPayment(paymentId);

        if (!orderId.equals(razorpayPayment.optString("order_id"))
                || razorpayPayment.optLong("amount") != expectedAmount
                || !"INR".equals(razorpayPayment.optString("currency"))
                || !"captured".equals(razorpayPayment.optString("status"))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment has not been captured for the expected amount");
        }

        if (Boolean.TRUE.equals(split.getPaid())) {
            if (paymentId.equals(split.getRazorpayPaymentId())) {
                return Map.of(
                        "success", true,
                        "paid", true,
                        "alreadyProcessed", true,
                        "status", "PAID",
                        "message", "Payment was already verified");
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This expense split has already been paid");
        }

        split.setPaid(true);
        split.setRazorpayPaymentId(paymentId);
        split.setPaidAt(java.time.LocalDateTime.now());
        expenseSplitRepository.save(split);
        return Map.of(
                "success", true,
                "paid", true,
                "alreadyProcessed", false,
                "status", "PAID",
                "message", "Payment verified successfully");
    }

    private Map<String, Object> orderResponse(
            String orderId, long amount) {
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("amount", amount);
        response.put("currency", "INR");
        response.put("keyId", razorpayGateway.getKeyId());
        return response;
    }

    private Map<String, Object> statusResponse(
            boolean paid, String status) {
        return Map.of(
                "success", true,
                "paid", paid,
                "alreadyProcessed", paid,
                "status", status);
    }

    private void validateOrder(
            JSONObject order, ExpenseSplit split, long expectedAmount) {
        if (!order.optString("receipt").equals("split_" + split.getId())
                || order.optLong("amount") != expectedAmount
                || !"INR".equals(order.optString("currency"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment order does not match this expense split");
        }
    }

    private void requireAssignedParticipant(
            ExpenseSplit split, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (split.getUser() == null
                || !split.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the assigned participant can access this payment");
        }
    }

    private long amountInPaise(ExpenseSplit split) {
        if (split.getAmount() == null
                || !Double.isFinite(split.getAmount())
                || split.getAmount() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expense split has an invalid amount");
        }

        try {
            return BigDecimal.valueOf(split.getAmount())
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expense split amount must use two decimal places");
        }
    }

    private void requireKeySecret() {
        if (isBlank(keySecret)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay credentials are not configured on the server");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryDTO> getPaymentHistory(
            Long groupId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        return expenseSplitRepository
                .findByUserIdAndExpenseGroupIdAndPaidTrueOrderByPaidAtDesc(
                        user.getId(), groupId)
                .stream()
                .map(split -> {
                    var expense = split.getExpense();

                    return new PaymentHistoryDTO(
                            split.getId(),
                            expense.getDescription(),
                            expense.getGroup().getName(),
                            expense.getPaidBy() == null
                                    ? "Unknown"
                                    : expense.getPaidBy().getName(),
                            split.getAmount(),
                            split.getPaidAt(),
                            split.getRazorpayPaymentId());
                })
                .sorted(Comparator.comparing(
                        PaymentHistoryDTO::paidAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}