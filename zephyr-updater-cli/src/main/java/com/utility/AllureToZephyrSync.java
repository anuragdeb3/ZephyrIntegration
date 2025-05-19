package com.utility;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class AllureToZephyrSync {
    public static void main(String[] args) throws IOException {
        ZephyrClient client = new ZephyrClient(
            ConfigReader.get("baseUrl"),
            ConfigReader.get("accessKey"),
            ConfigReader.get("secretKey"),
            ConfigReader.get("accountId")
        );

        int projectId = ProjectConfig.getProjectId();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String cycleResponse = client.createCycle("Automation Cycle - " + today, projectId, -1);
        String cycleId = new ObjectMapper().readTree(cycleResponse).path("id").asText();

        List<TestCaseResult> testResults = AllureParser.parseResults("allure-results/");
        List<String> issueKeys = testResults.stream().map(r -> r.id).collect(Collectors.toList());

        Map<String, String> executions = client.createExecutions(issueKeys, cycleId, projectId);

        for (TestCaseResult result : testResults) {
            int zephyrStatus = mapStatus(result.status);
            String executionId = executions.get(result.id);
            client.updateExecution(executionId, zephyrStatus);
        }

        System.out.println("✅ Zephyr sync completed.");
    }

    private static int mapStatus(String allureStatus) {
        switch (allureStatus.toLowerCase()) {
            case "passed": return 1;
            case "failed": return 2;
            case "broken": return 4;
            default: return -1;
        }
    }
}