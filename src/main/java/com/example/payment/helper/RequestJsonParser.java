package com.example.payment.helper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.stream.Collectors;

public class RequestJsonParser {
    private RequestJsonParser() {
        throw new IllegalStateException("Utility class");
    }
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

    public static <T> T parse(HttpServletRequest request, Class<T> clazz) throws IOException {
        String jsonRequest = request.getReader().lines().collect(Collectors.joining());
        return mapper.readValue(jsonRequest, clazz);
    }
}
