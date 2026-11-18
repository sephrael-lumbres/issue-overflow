package com.sephrael.issueoverflow.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The image is built from the Dockerfile at the repository root (Maven runs the tests there);
 * Heroku's files are gone, so nothing describes a second way to run the app.
 */
class DeploymentFilesTest {

    @Test
    void theImagesArePinnedToOneTemurinBuild() throws IOException {
        List<String> fromLines = Files.readAllLines(Path.of("Dockerfile")).stream()
                .filter(line -> line.startsWith("FROM "))
                .toList();

        assertThat(fromLines).hasSize(2);
        // a full version and build, like 21.0.13_9, never the moving 21-jre tag
        assertThat(fromLines).allMatch(line -> line.matches("FROM eclipse-temurin:21\\.\\d+\\.\\d+_\\d+-(jdk|jre)-resolute( AS build)?"));
        assertThat(fromLines.stream().map(line -> line.replaceAll(".*:(21\\.\\d+\\.\\d+_\\d+)-.*", "$1")).distinct())
                .hasSize(1);
    }

    @Test
    void herokuFilesAreGone() {
        assertThat(Path.of("Procfile")).doesNotExist();
        assertThat(Path.of("system.properties")).doesNotExist();
    }
}
