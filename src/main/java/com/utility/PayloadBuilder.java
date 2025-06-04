package com.utility;

import java.util.HashMap;
import java.util.Map;

public class PayloadBuilder {
    public static Map<String, Object> buildCyclePayload(String name, int projectId, int versionId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("projectId", projectId);
        payload.put("versionId", versionId);
        return payload;
    }

    public static Map<String, Object> buildAddTestPayload(String issueId, String cycleId, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("issueId", issueId);
        payload.put("cycleId", cycleId);
        payload.put("projectId", projectId);
        return payload;
    }
}