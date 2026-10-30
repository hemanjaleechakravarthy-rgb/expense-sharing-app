package kluniversity.com.expense;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:expense_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=local-only-test-jwt-secret-012345678901234567890",
        "razorpay.key.id=test_key_id",
        "razorpay.key.secret=test_key_secret",
		"razorpay.webhook.secret=test_webhook_secret"
})
class ExpenseApplicationTests {

    @Test
    void contextLoads() {
    }
}