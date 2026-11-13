package com.sephrael.issueoverflow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rules every Thymeleaf template follows, checked over the template files themselves (no Spring context). Plain HTML
 * comments are sent to every visitor, so dead markup inside them counts too.
 */
class TemplateRulesTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void noDeadCommentedOutLinks() {
        Map<Path, String> templates = templates();

        assertThat(templates).isNotEmpty();
        assertThat(filesContaining(templates, "github.com/sephraellumbres")).isEmpty();
        assertThat(filesContaining(templates, "Terms &amp; Conditions")).isEmpty();
        assertThat(filesContaining(templates, "30-40 HOURS")).isEmpty();
    }

    // Every template's path (relative to the templates folder) and its text.
    static Map<Path, String> templates() {
        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            return files.filter(path -> path.toString().endsWith(".html"))
                    .collect(Collectors.toMap(TEMPLATES::relativize, TemplateRulesTest::read));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    static List<Path> filesContaining(Map<Path, String> templates, String text) {
        return templates.entrySet().stream()
                .filter(entry -> entry.getValue().contains(text))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
