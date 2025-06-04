package com.utility;

import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class RestClient {

    public static Response get(String uri, Map<String, String> headers, Map<String, String> queryParams) {
        io.restassured.specification.RequestSpecification request = given().headers(headers);

        if (queryParams != null && !queryParams.isEmpty()) {
            request.queryParams(queryParams);
        }

        return request.get(uri);
    }

    public static Response post(String uri, Map<String, String> headers, Object body, Map<String, String> queryParams) {
        io.restassured.specification.RequestSpecification request = given().headers(headers).body(body);

        if (queryParams != null && !queryParams.isEmpty()) {
            request.queryParams(queryParams);
        }

        return request.post(uri);
    }

    public static Response put(String uri, Map<String, String> headers, Object body, Map<String, String> queryParams) {

        io.restassured.specification.RequestSpecification request = given().headers(headers).body(body);
        if (queryParams != null && !queryParams.isEmpty()) {
            request.queryParams(queryParams);
        }

        return request.put(uri);
    }
}