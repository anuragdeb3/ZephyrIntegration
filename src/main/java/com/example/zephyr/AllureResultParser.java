
package com.example.zephyr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AllureResultParser {

    private static final String ALLURE_RESULTS_DIR = "allure-results";

    public static Map<String, String> parseResults() {
        Map<String, String> testResults = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        File dir = new File(ALLURE_RESULTS_DIR);
        Pattern pattern = Pattern.compile("CBD-\\d+");

        for (File file : Objects.requireNonNull(dir.listFiles((d, name) -> name.endsWith("-result.json")))) {
            try {
                JsonNode root = mapper.readTree(file);
                String status = root.path("status").asText();
                JsonNode labels = root.path("labels");
                for (JsonNode label : labels) {
                    if ("tag".equals(label.path("name").asText())) {
                        String tag = label.path("value").asText();
                        Matcher matcher = pattern.matcher(tag);
                        if (matcher.find()) {
                            String testCaseKey = matcher.group();
                            testResults.put(testCaseKey, status);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return testResults;
    }
}
