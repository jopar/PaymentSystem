
# Payment System with Adyen Integration

[GitHub Repository](https://github.com/jopar/PaymentSystem)

This is a Spring Boot-based backend system for processing online payments using [Adyen](https://www.adyen.com/). It supports multiple payment methods, secure webhook handling, and storage of transaction data for later reporting or analysis.

> ⚠️ This project currently does not include a frontend.

---

## 🚀 Features

- 🔐 Secure payment creation and status tracking
- 💳 Supports Adyen iDEAL and card payments
- 🔄 Webhook endpoint for asynchronous Adyen notifications
- ✅ HMAC verification and Basic Auth for webhook security
- 🧾 Persists webhook events and payment details to the database
- 🔍 Easily testable with in-memory H2 database
- 📊 Clean separation between controller, service, and DAO layers

---

## 🧰 Technologies Used

- Java 17
- Spring Boot 2.7 (with XML-based config)
- Adyen Java SDK
- PostgreSQL (production)
- H2 (testing)
- JUnit 5 + Mockito

---
## 📁 Project Structure

```text
src/
├── controller/       # REST controllers for payment + webhook
├── service/          # Business logic
├── dao/              # Data access via NamedParameterJdbcTemplate
├── dto/              # DTOs for requests and responses
├── async/            # Asynchronous webhook handling
├── config/           # Adyen + Spring configuration
├── exceptions/       # Custom exceptions
├── logging/          # Logging wrappers
```

---

## 🔌 API Endpoints

### `POST /api/payment/adyen`
 1. Creates a new payment request
 2. Accepts: `PaymentRequestDTO`
 3. Returns: redirect URL to Adyen checkout
### Example:
 
 1. 
 ```
curl -X POST http://<URL_FOR_SERVER>:<PORT>/api/payments/adyen/pay \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethodDetails": {
      "type": "scheme",
      "encryptedCardNumber": "test_4111111111111111",
      "encryptedSecurityCode": "test_737",
      "encryptedExpiryYear": "test_2030",
      "encryptedExpiryMonth": "test_03"
    },
    "amount": "500",
    "currency": "USD",
    "referenceNumber": "ref12345",
    "returnURL": "http://example.com/return"
  }'
```
2. 
```
curl -X POST http://localhost:8080/api/payments/adyen/pay \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethodDetails": {
      "type": "ideal"
    },
    "amount": "500",
    "currency": "EUR",
    "referenceNumber": "ref12345",
    "returnURL": "https://payments.andrazgregorcic.eu/api/payments/adyen/return"
  }'
```

### `POST /api/webhook/adyen`
- Adyen webhook endpoint
- Validates HMAC + Basic Auth
- Persists `NotificationRequestItem`s and updates associated payment

---

## 🔐 Webhook Security

- **Basic Authentication**: Validated against configured `username:password`
- **HMAC Signature**: Verified using Adyen-provided HMAC key

---
## 📬 Webhook Handling

Webhooks are validated with:

- ✅ **Basic Auth** (`Authorization` header)
- ✅ **HMAC signature** (per Adyen's recommendation)

Notification items are:

- 📝 Saved to the database
- 🔄 Used to update the corresponding payment status


## 🗄️ Database Tables

### `payment`
| Column         | Description                  |
|----------------|------------------------------|
| id             | Internal ID                  |
| reference      | Merchant reference           |
| psp_reference  | Adyen PSP reference          |
| status         | Current payment status       |
| amount         | Payment amount               |
| currency       | ISO 3-letter currency code   |
| created_at     | Creation timestamp           |
| updated_at     | Last update timestamp        |

### `payment_webhook`
| Column         | Description                            |
|----------------|----------------------------------------|
| id             | Internal ID                            |
| payment_id     | Foreign key to payment                 |
| event_code     | Adyen event type (e.g. AUTHORISATION)  |
| psp_reference  | Adyen's reference for this event       |
| success        | Was the event successful?              |
| event_date     | Date of the Adyen event                |
| received_at    | When the system received the webhook   |
| raw_notification | Full JSON payload                    |

---

## 🧪 Testing

- 🗄️ Uses **H2 in-memory database** for integration tests  
- 🧱 Schema initialized with `schema.sql`  
- 🧪 Unit tests written with **JUnit 5** and **Mockito**  
- 🔗 Integration tests validate **real beans** and full application flow

### ✅ Running Tests

```bash
./mvnw test
```

## 🧪 Running the Application

```bash
./mvnw spring-boot:run
```
<!--stackedit_data:
eyJoaXN0b3J5IjpbLTI3NDU2MTQzNCw2MjI3MTEzNzAsLTEwND
U2MzA1MzIsLTIwODk5NzU1NzQsMTA2Nzc1MTIyNiw4MjYzMDQ3
NTNdfQ==
-->