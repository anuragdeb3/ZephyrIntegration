package com.utility;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class ApiClient {

    private static final String BASE_URL = ConfigManager.get("zephyr.baseUrl");
    private static final String ACCESS_KEY = ConfigManager.get("zephyr.accessKey");
    private static final String SECRET_KEY = ConfigManager.get("zephyr.secretKey");
    private static final String CLIENT_ID = ConfigManager.get("zephyr.clientId");
    private static final boolean MOCK_MODE = ConfigManager.getBoolean("mock.mode");
    private static final int TIMEOUT = ConfigManager.getInt("timeout.ms");
    private static final int MAX_RETRIES = ConfigManager.getInt("max.retries");

    static {
        RestAssured.config = RestAssured.config()
                .httpClient(RestAssured.config().getHttpClientConfig()
                        .setParam("http.connection.timeout", TIMEOUT)
                        .setParam("http.socket.timeout", TIMEOUT)
                        .setParam("http.connection-manager.timeout", (long) TIMEOUT));
    }

    public static Response sendRequest(String method, String endpoint, String body) {
        if (MOCK_MODE) {
            System.out.println("⚠️ MOCK MODE ENABLED - Skipping actual HTTP call to: " + endpoint);
            return createMockResponse();
        }

        String url = BASE_URL + endpoint;
        Map<String, String> headers = buildAuthHeaders(url);

        int attempt = 0;
        Response response = null;

        while (attempt < MAX_RETRIES) {
            try {
                RequestSpecification request = given()
                        .headers(headers)
                        .log().all()
                        .body(body != null ? body : "");

                response = request
                        .request(Method.valueOf(method.toUpperCase()), url)
                        .then()
                        .log().all()
                        .extract()
                        .response();

                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    return response;
                }

                System.out.printf("❗Attempt %d failed: %d %s%n", attempt + 1,
                        response.getStatusCode(), response.getStatusLine());

            } catch (Exception e) {
                System.out.printf("❗Attempt %d failed with exception: %s%n", attempt + 1, e.getMessage());
            }

            attempt++;
        }

        throw new RuntimeException("❌ All attempts failed for endpoint: " + endpoint);
    }

    private static Map<String, String> buildAuthHeaders(String uri) {
        Map<String, String> headers = new HashMap<>();
        headers.put("zapiAccessKey", ACCESS_KEY);
        headers.put("zapiSecretKey", SECRET_KEY);
       // headers.put("Authorization", WrapperFunctions.getJWT(uri, CLIENT_ID));
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private static Response createMockResponse() {
        return given()
                .body("{ \"id\": \"mock-id-123\", \"status\": \"success\" }")
                .when()
                .get("https://mock.api/response")
                .then()
                .extract()
                .response();
    }
}
