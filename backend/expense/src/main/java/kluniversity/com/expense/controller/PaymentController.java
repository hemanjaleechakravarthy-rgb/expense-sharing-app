package kluniversity.com.expense.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.dto.CreatePaymentRequest;
import kluniversity.com.expense.dto.VerifyPaymentRequest;
import kluniversity.com.expense.service.PaymentService;
import kluniversity.com.expense.dto.PaymentHistoryDTO;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody CreatePaymentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.createOrder(
                request, requireEmail(authentication)));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody VerifyPaymentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.verifyPayment(
                request, requireEmail(authentication)));
    }

    @GetMapping("/{splitId}/status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable Long splitId,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(
                splitId, requireEmail(authentication)));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String body,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = "X-Razorpay-Signature", required = false)
                    String signature) {
        return ResponseEntity.ok(paymentService.handleWebhook(body, signature));
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return authentication.getName();
    }

    @GetMapping("/history/{groupId}")
public ResponseEntity<List<PaymentHistoryDTO>> getPaymentHistory(
        @PathVariable Long groupId,
        Authentication authentication) {

    return ResponseEntity.ok(
            paymentService.getPaymentHistory(
                    groupId, requireEmail(authentication)));
}
}
