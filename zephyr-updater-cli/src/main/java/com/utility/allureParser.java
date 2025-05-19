package com.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class AllureParser {
    public static List<TestCaseResult> parseResults(String directory) throws IOException {
        List<TestCaseResult> results = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        Files.list(Paths.get(directory)).filter(p -> p.toString().endsWith(".json")).forEach(file -> {
            try {
                JsonNode root = mapper.readTree(file.toFile());
                String status = root.path("status").asText();
                JsonNode labels = root.path("labels");
                String testCaseId = null;

                for (JsonNode label : labels) {
                    if ("testCaseId".equals(label.path("name").asText())) {
                        testCaseId = label.path("value").asText();
                        break;
                    }
                }

                if (testCaseId != null) {
                    results.add(new TestCaseResult(testCaseId, status));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return results;
    }
}