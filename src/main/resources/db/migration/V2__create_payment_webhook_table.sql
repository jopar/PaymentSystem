-- Create table for PaymentWebhook
CREATE TABLE payment_webhook (
     id BIGSERIAL PRIMARY KEY,
     payment_id BIGINT NOT NULL,
     event_code VARCHAR(255),
     success BOOLEAN,
     psp_reference VARCHAR(255),
     event_date TIMESTAMPTZ,
     received_at TIMESTAMPTZ,
     raw_notification TEXT,
     CONSTRAINT fk_payment FOREIGN KEY (payment_id) REFERENCES payment(id) ON DELETE CASCADE
);

-- Add an index on payment_id for better performance in JOIN operations
CREATE INDEX idx_payment_id ON payment_webhook(payment_id);