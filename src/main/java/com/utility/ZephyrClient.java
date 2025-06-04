/*
package com.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZephyrClient {
    private final String baseUrl;
    private final String accessKey;
    private final String secretKey;
    private final String accountId;

    public ZephyrClient(String baseUrl, String accessKey, String secretKey, String accountId) {
        this.baseUrl = baseUrl;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.accountId = accountId;
    }

    private HttpResponse sendRequest(String method, String uri, StringEntity entity) throws IOException {
        String jwt = JwtGenerator.generateJwt(method, uri, baseUrl, accessKey, secretKey);
        HttpClient client = HttpClients.createDefault();
        HttpRequestBase request;

        String fullUrl = baseUrl + uri;
        switch (method.toUpperCase()) {
            case "POST":
                HttpPost post = new HttpPost(fullUrl);
                if (entity != null) post.setEntity(entity);
                request = post;
                break;
            case "PUT":
                HttpPut put = new HttpPut(fullUrl);
                if (entity != null) put.setEntity(entity);
                request = put;
                break;
            default:
                request = new HttpGet(fullUrl);
        }

        request.setHeader("Authorization", jwt);
        request.setHeader("zapiAccessKey", accessKey);
        request.setHeader("Content-Type", "application/json");

        return client.execute(request);
    }

    public String createCycle(String name, int projectId, int versionId) throws IOException {
        String uri = "/public/rest/api/1.0/cycle";
        String payload = String.format("{"name":"%s", "projectId":%d, "versionId":%d}", name, projectId, versionId);
        HttpResponse response = sendRequest("POST", uri, new StringEntity(payload));
        return EntityUtils.toString(response.getEntity());
    }

    public Map<String, String> createExecutions(List<String> issueKeys, String cycleId, int projectId) throws IOException {
        String uri = "/public/rest/api/1.0/execution";
        String issues = issueKeys.stream().map(k -> """ + k + """).collect(Collectors.joining(","));
        String payload = String.format("{"issues":[%s],"cycleId":"%s","projectId":%d,"versionId":-1}", issues, cycleId, projectId);

        HttpResponse response = sendRequest("POST", uri, new StringEntity(payload));
        String json = EntityUtils.toString(response.getEntity());

        // Simplified parsing for demonstration purposes
        Map<String, String> executionMap = new HashMap<>();
        JsonNode rootNode = new ObjectMapper().readTree(json);
        rootNode.fields().forEachRemaining(entry -> {
            String executionId = entry.getKey();
            String issueKey = entry.getValue().path("issueKey").asText();
            executionMap.put(issueKey, executionId);
        });

        return executionMap;
    }

    public void updateExecution(String executionId, int status) throws IOException {
        String uri = "/public/rest/api/1.0/execution/" + executionId + "/execute";
        String payload = String.format("{"status":%d}", status);
        sendRequest("PUT", uri, new StringEntity(payload));
    }
}*/
