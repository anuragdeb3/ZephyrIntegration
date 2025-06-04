package com.utility;

import io.restassured.response.Response;

public class ResponseHandler {
    public static String getValueByKey(Response response, String key) {
        return response.jsonPath().getString(key);
    }
}