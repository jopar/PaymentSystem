package com.example.payment.repository;

import com.example.payment.model.PaymentWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, Long> {
}
