package com.utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ProjectConfig {
    private static final Properties properties = new Properties();

    static {
        try {
            properties.load(new FileInputStream("project.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load project.properties", e);
        }
    }

    public static int getProjectId() {
        return Integer.parseInt(properties.getProperty("projectId"));
    }
}