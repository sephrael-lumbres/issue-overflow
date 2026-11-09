package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A GET on a POST-only route (a delete link opened in a new tab, for example) is a 405.
 * Without an error/405 template, Spring Boot showed its Whitelabel page.
 * MockMvc doesn't run the servlet container's error dispatch, so this asks the
 * error view resolver directly; the smoke test checks the page in a browser.
 */
class MethodNotAllowedPageTest extends WebIntegrationTestBase {

    @Autowired
    private ErrorViewResolver errorViewResolver;

    @Test
    void methodNotAllowedHasItsOwnErrorPage() {
        ModelAndView errorView = errorViewResolver.resolveErrorView(new MockHttpServletRequest(), HttpStatus.METHOD_NOT_ALLOWED, Map.of());

        assertThat(errorView).isNotNull();
        assertThat(errorView.getViewName()).isEqualTo("error/405");
    }
}
