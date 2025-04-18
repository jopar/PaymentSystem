-- Create table for Payment
CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    merchant_reference VARCHAR(255),
    psp_reference VARCHAR(255),
    amount DECIMAL(255),
    currency VARCHAR(10),
    reference VARCHAR(255),
    payment_method VARCHAR(100),
    status VARCHAR(100),
    auth_code VARCHAR(100),
    failure_message TEXT,
    create_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);