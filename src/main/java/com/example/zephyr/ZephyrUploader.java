
package com.example.zephyr;

import java.util.Map;

public class ZephyrUploader {

    public static void uploadResults(Map<String, String> testResults) {
        // TODO: Implement authentication using JWT as per Zephyr Squad Cloud REST API
        // TODO: For each test case key and status in testResults, send the result to Zephyr
        // Reference: https://zephyrsquad.docs.apiary.io/
    }

    public static void main(String[] args) {
        Map<String, String> testResults = AllureResultParser.parseResults();
        uploadResults(testResults);
    }
}
