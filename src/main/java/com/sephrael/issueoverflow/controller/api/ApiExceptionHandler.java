package com.sephrael.issueoverflow.controller.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Error answers for the JSON controllers in this package: a problem-details body (RFC 9457, {@code application/problem+json})
 * with the right status. Pages (Thymeleaf controllers elsewhere) keep their HTML error pages, because this advice applies only to this package.
 * <ul>
 *   <li>{@code OrganizationScope.notFound()} and other {@code ResponseStatusException}s: their status, handled by
 *   {@link ResponseEntityExceptionHandler}. Another organization's record and a missing one get the same body.</li>
 *   <li>An invalid request body: 400, with an {@code errors} list of field names and the failed constraint (no message text).</li>
 *   <li>Anything unexpected: 500, logged here, with no detail from the exception in the body.</li>
 * </ul>
 * 401 and 403 from the security rules never reach a controller; ApiSecurityErrors in the configuration package writes them.
 */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // One entry in a 400 answer's "errors" list: the field and the constraint it failed, for example NotBlank or Size.
    public record InvalidField(String field, String constraint) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<InvalidField> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), error.getCode()))
                .toList();

        ProblemDetail body = exception.getBody();
        body.setDetail("The request has invalid fields.");
        body.setProperty("errors", errors);

        return handleExceptionInternal(exception, body, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        log.error("Unexpected error in a JSON request", exception);

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
