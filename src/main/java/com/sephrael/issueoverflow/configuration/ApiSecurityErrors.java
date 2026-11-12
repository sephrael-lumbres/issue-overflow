package com.sephrael.issueoverflow.configuration;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security-layer answers for JSON API requests (paths under /api/).
 * <p>
 * Spring Security rejects some requests before any controller runs, so {@code ApiExceptionHandler} never sees them.
 * For pages, it redirects to the login page or shows the HTML 403 page. For {@code /api/**} requests, this class
 * answers with a 401 or 403 status and a problem-details body (RFC 9457) instead.
 * <p>
 * The bodies are built from fixed messages, so they are written as strings, without a JSON library.
 */
final class ApiSecurityErrors {

    // JSON requests are recognized by path alone: the issue page's scripts call /api/..., whatever Accept header they send
    static final RequestMatcher API_REQUESTS = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

    static final String NOT_LOGGED_IN = "Not logged in. Please log in to continue.";
    static final String SESSION_ENDED = "Your session has expired. Please log in again to view this page.";
    static final String ACCESS_DENIED = "Access Denied. You don't have permission to access this resource.";
    static final String CSRF_REJECTED = "Invalid or missing CSRF token. Request aborted.";

    private ApiSecurityErrors() {
    }

    // 401 for a request without a logged-in session
    static AuthenticationEntryPoint entryPoint() {
        return (request, response, exception) -> write(response, HttpStatus.UNAUTHORIZED, NOT_LOGGED_IN);
    }

    // 403 for a role the rules don't allow, or a missing or wrong CSRF token. Pages get Spring Security's default
    // handler (the HTML 403 page). It is the chain's one access-denied handler, so the CSRF filter uses it too.
    static AccessDeniedHandler accessDeniedHandler() {
        AccessDeniedHandler pages = new AccessDeniedHandlerImpl();
        return (request, response, exception) -> {
            if(API_REQUESTS.matches(request))
                write(response, HttpStatus.FORBIDDEN, exception instanceof CsrfException ? CSRF_REJECTED : ACCESS_DENIED);
            else
                pages.handle(request, response, exception);
        };
    }

    static void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // no "type": RFC 9457 reads a missing type as about:blank, which Spring 7 ProblemDetail bodies also leave out
        response.getWriter().write("{\"title\":\"" + status.getReasonPhrase()
                + "\",\"status\":" + status.value() + ",\"detail\":\"" + detail + "\"}");
    }
}
