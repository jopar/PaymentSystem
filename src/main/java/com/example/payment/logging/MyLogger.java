package com.example.payment.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

public class MyLogger {
    private final Logger logger;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MyLogger(Logger logger) {
        this.logger = logger;
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String message, Object context) {
        if (logger.isInfoEnabled()) {
            logger.info(formatMessage(message, context));
        }
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Object context) {
        if (logger.isErrorEnabled()) {
            logger.error(formatMessage(message, context));
        }
    }

    private String formatMessage(String message, Object context) {
        if (context != null) {
            try {
                String json = objectMapper.writeValueAsString(context);
                return String.format("%s. Context: %s", message, json);
            } catch (JsonProcessingException e) {
                return String.format("%s. Context: [unserializable object: %s]", message, e.getMessage());
            }
        } else {
            return message;
        }
    }
}
