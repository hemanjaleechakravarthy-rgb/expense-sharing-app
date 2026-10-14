# Expense Sharing backend configuration

The backend reads credentials from environment variables; do not put them in
`application.properties` or frontend source:

- `SPRING_DATASOURCE_PASSWORD`: password for the configured PostgreSQL database.
- `JWT_SECRET`: at least 32 bytes of private random data. Changing this invalidates
  previously issued JWTs; users can sign in again. The backend now checks this
  at startup and will not start with a missing or too-short value.
- `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`: Razorpay API keys for the selected
  test or live account. Only the public key ID is sent to Checkout.
- `RAZORPAY_WEBHOOK_SECRET`: the secret configured for this application's
  Razorpay webhook endpoint (`/api/payments/webhook`).

For local PowerShell development, set these variables in the shell that starts
Spring Boot, then run `.\mvnw.cmd spring-boot:run` from this directory.
To generate a temporary local JWT secret for that shell:

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Keep the same secret when restarting the backend if existing login sessions
should remain valid.

The existing JPA `update` setting is retained. The implementation adds nullable
submission-key and Razorpay order/payment-reference columns to existing expenses
and splits; no table or existing row is dropped or reset.

Razorpay Standard Checkout now requests UPI as a payment method. Razorpay
documents UPI Intent on mobile web and automatic QR display on desktop web;
all Checkout methods enabled for the merchant are left available; available
methods still depend on the account's activation and Checkout configuration.
Razorpay's UPI Collect flow was deprecated effective
28 February 2026, so this integration does not request Collect. Confirm UPI is
enabled for the merchant account in Razorpay Dashboard before use. Configure a
`payment.captured` webhook pointing to `/api/payments/webhook` and set its
webhook secret in the backend environment. No real-money transaction is needed
to build or test this project.
