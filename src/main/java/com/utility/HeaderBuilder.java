package com.utility;

import java.util.HashMap;
import java.util.Map;

public class HeaderBuilder {
    public static Map<String, String> buildHeaders(String methodType, boolean hasQueryParams) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Basic <base64-encoded-credentials>");

        headers.put("X-Method-Type", methodType);
        return headers;
    }
}