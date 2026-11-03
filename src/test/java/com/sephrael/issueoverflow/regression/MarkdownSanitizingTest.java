package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that user markdown is sanitized before it reaches the page.
 * <p>
 * Issue descriptions, comments and change history are turned into HTML in the browser by Showdown.
 * Showdown passes HTML through, so every converted string must go through {@code sanitizeMarkdownHtml}
 * before it is assigned to {@code innerHTML}. That helper calls DOMPurify and also forbids
 * style tags and attributes and form controls.
 * <p>
 * JavaScript doesn't run in these tests. They check the page source and the served library.
 * The browser smoke test checks the behavior.
 */
class MarkdownSanitizingTest extends WebIntegrationTestBase {

    private static final String PURIFY = "<script src=\"/js/vendor/purify.min.js\"></script>";
    private static final String SANITIZER = "<script src=\"/js/sanitize-markdown.js\"></script>";

    @Test
    void issuePageSanitizesEveryConvertedMarkdownString() throws IOException {
        String page = template("templates/issues/issue-details.html");
        List<String> assignments = page.lines().filter(line -> line.contains("makeHtml(")).toList();

        assertThat(assignments).hasSize(3)
                .allMatch(line -> line.contains("innerHTML = sanitizeMarkdownHtml(") && line.contains(".makeHtml("));
        assertThat(page.indexOf(PURIFY)).isPositive().isLessThan(page.indexOf(SANITIZER));
        assertThat(page.indexOf(SANITIZER)).isLessThan(page.indexOf("innerHTML = sanitizeMarkdownHtml("));
    }

    // DOMPurify's defaults keep <style>, forms and style attributes; the helper forbids them.
    @Test
    void sanitizerForbidsStylesAndFormControls() throws Exception {
        String helper = mockMvc.perform(get("/js/sanitize-markdown.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(helper)
                .contains("function sanitizeMarkdownHtml(html)")
                .contains("FORBID_TAGS: ['style', 'form', 'input', 'button', 'textarea', 'select', 'option']")
                .contains("FORBID_ATTR: ['style']");
    }

    @Test
    void domPurifyIsServedFromThisSite() throws Exception {
        String library = mockMvc.perform(get("/js/vendor/purify.min.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(library).startsWith("/*! @license DOMPurify 3.4.17 ");
    }

    // The server never emits user markdown as markup: it arrives HTML-escaped in a hidden textarea.
    @Test
    void descriptionReachesThePageAsEscapedText() throws Exception {
        Fixture fixture = createFixture();
        inTransaction(() -> issueRepository.findById(fixture.issue.getId()).orElseThrow().setDescription("<img src=x onerror=alert(1)>"));
        MockHttpSession session = login(fixture.projectManager.getEmail());

        String page = mockMvc.perform(get("/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey()).session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page).contains("&lt;img src=x onerror=alert(1)&gt;").doesNotContain("<img src=x");
    }

    // EasyMDE previews render with marked, which also passes HTML through. Every editor sanitizes its preview, and
    // every page with an editor loads the sanitizer first and pins the EasyMDE version the option was checked on.
    @Test
    void editorPreviewsAreSanitized() throws IOException {
        String editors = template("static/js/markdown.js");

        assertThat(count(editors, "new EasyMDE(")).isEqualTo(3);
        assertThat(count(editors, "renderingConfig: sanitizedPreview")).isEqualTo(3);
        assertThat(editors).contains("sanitizerFunction: (renderedHTML) => sanitizeMarkdownHtml(renderedHTML)");

        for (String page : List.of("create-issue", "edit-issue", "issue-details")) {
            String source = template("templates/issues/" + page + ".html");
            assertThat(source).as(page)
                    .contains("https://unpkg.com/easymde@2.21.0/dist/easymde.min.js")
                    .contains("https://unpkg.com/easymde@2.21.0/dist/easymde.min.css")
                    .doesNotContain("https://unpkg.com/easymde/");
            assertThat(source.indexOf(PURIFY)).as(page).isPositive().isLessThan(source.indexOf(SANITIZER));
            assertThat(source.indexOf(SANITIZER)).as(page).isLessThan(source.indexOf("<script src=\"/js/markdown.js\"></script>"));
        }
    }

    private static int count(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    static String template(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
