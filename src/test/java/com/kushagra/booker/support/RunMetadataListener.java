package com.kushagra.booker.support;

import com.kushagra.booker.config.ApiConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.testng.IExecutionListener;

public final class RunMetadataListener implements IExecutionListener {
    @Override
    public void onExecutionStart() {
        Path directory = Path.of(System.getProperty("allure.results.directory", "target/allure-results"));
        Properties environment = new Properties();
        environment.setProperty("Base URL", ApiConfig.load().baseUrl());
        environment.setProperty("Java", System.getProperty("java.version"));
        environment.setProperty("OS", System.getProperty("os.name"));
        environment.setProperty("Execution", "Sequential; independent owned bookings; no automatic retries");
        try {
            Files.createDirectories(directory);
            try (var stream = Files.newOutputStream(directory.resolve("environment.properties"))) {
                environment.store(stream, "Non-sensitive run metadata");
            }
            try (var categories = getClass().getResourceAsStream("/allure-categories.json")) {
                if (categories != null) Files.copy(categories, directory.resolve("categories.json"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write Allure run metadata", exception);
        }
    }
}
