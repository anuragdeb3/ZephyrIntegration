package com.utility;

import io.restassured.response.Response;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class ZephyrAPI {
    private final Properties config = new Properties();

    public ZephyrAPI() {
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Response createCycle(String name, int projectId, int versionId) {
        Map<String, Object> body = PayloadBuilder.buildCyclePayload(name, projectId, versionId);
        Map<String, String> headers = HeaderBuilder.buildHeaders("POST", false);
        String endpoint = config.getProperty("zephyr.createCycle.endpoint");
        return RestClient.post(config.getProperty("jira.baseUrl") + endpoint, headers, body,null);
    }

    public Response addTestToCycle(String issueId, String cycleId, String projectId) {
        Map<String, Object> body = PayloadBuilder.buildAddTestPayload(issueId, cycleId, projectId);
        Map<String, String> headers = HeaderBuilder.buildHeaders("POST", true);
        String endpoint = config.getProperty("zephyr.addTestToCycle.endpoint");
        return RestClient.post(config.getProperty("jira.baseUrl") + endpoint, headers, body,null);
    }
}