package com.example.payment.config;

public class AdyenConfig {

    private String apiKey;
    private String environment;
    private String merchantAccount;
    private String webhookUsername;
    private String webhookPassword;
    private String webhookHMAC;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getMerchantAccount() {
        return merchantAccount;
    }

    public void setMerchantAccount(String merchantAccount) {
        this.merchantAccount = merchantAccount;
    }

    public String getWebhookUsername() {
        return webhookUsername;
    }

    public void setWebhookUsername(String webhookUsername) {
        this.webhookUsername = webhookUsername;
    }

    public String getWebhookPassword() {
        return webhookPassword;
    }

    public void setWebhookPassword(String webhookPassword) {
        this.webhookPassword = webhookPassword;
    }

    public String getWebhookHMAC() {
        return webhookHMAC;
    }

    public void setWebhookHMAC(String webhookHMAC) {
        this.webhookHMAC = webhookHMAC;
    }
}
