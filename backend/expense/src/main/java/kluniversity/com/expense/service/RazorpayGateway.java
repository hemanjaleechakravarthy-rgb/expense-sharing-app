package kluniversity.com.expense.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;

@Component
public class RazorpayGateway {

    private final String keyId;
    private final String keySecret;

    public RazorpayGateway(
            @Value("${razorpay.key.id:}") String keyId,
            @Value("${razorpay.key.secret:}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public String getKeyId() {
        return keyId;
    }


    public String createOrder(long amount, String currency, String receipt) {
        try {
        JSONObject request = new JSONObject();
        request.put("amount", amount);
        request.put("currency", currency);
        request.put("receipt", receipt);

        return client().orders.create(request).toJson().getString("id");
    } catch (ResponseStatusException exception) {
        throw exception;
    } catch (Exception exception) {
        System.err.println("Razorpay create-order error: "
                + exception.getClass().getSimpleName()
                + ": " + exception.getMessage());
        throw providerError("Unable to create a payment order");
    }
}


    public JSONObject fetchOrder(String orderId) {
        try {
            Order order = client().orders.fetch(orderId);
            return order.toJson();
        } catch (Exception exception) {
            throw providerError("Unable to confirm the payment order");
        }
    }

    public JSONObject fetchPayment(String paymentId) {
        try {
            return client().payments.fetch(paymentId).toJson();
        } catch (Exception exception) {
            throw providerError("Unable to confirm the payment");
        }
    }

    public List<JSONObject> fetchPaymentsForOrder(String orderId) {
        try {
            List<JSONObject> payments = new ArrayList<>();
            client().orders.fetchPayments(orderId)
                    .forEach(payment -> payments.add(payment.toJson()));
            return payments;
        } catch (Exception exception) {
            throw providerError("Unable to check payments for this order");
        }
    }

    private RazorpayClient client() throws Exception {
        if (isBlank(keyId) || isBlank(keySecret)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay credentials are not configured on the server");
        }
        return new RazorpayClient(keyId, keySecret);
    }

    private ResponseStatusException providerError(String message) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
