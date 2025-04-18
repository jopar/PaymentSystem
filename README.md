
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

src/ ├── controller/ # REST controllers for payment + webhook ├── service/ # Business logic ├── dao/ # Data access via NamedParameterJdbcTemplate ├── dto/ # DTOs for requests and responses ├── async/ # Asynchronous webhook handling ├── config/ # Adyen + Spring configuration ├── exceptions/ # Custom exceptions ├── logging/ # Logging wrappers
<!--stackedit_data:
eyJoaXN0b3J5IjpbLTIxODk3MjM0MCw4MjYzMDQ3NTNdfQ==
-->