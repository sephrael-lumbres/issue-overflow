package com.sephrael.issueoverflow.configuration;

import com.github.benmanes.caffeine.cache.Ticker;
import com.sephrael.issueoverflow.service.Emails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Limits posts to the forms that repeated tries can abuse: login, registration, password reset,
 * joining an organization, resending the verification email, and the contact form.
 * <p>
 * Each limit counts posts to one form per key (the client's IP address, the email address typed, or the
 * signed-in account) in a fixed window kept in memory. A post over a limit gets 429 with Retry-After before login
 * or the controller handles it. Limits are checked in order, so a post stopped by one counts toward none after it.
 * A key over 255 characters is skipped, so a client can't fill memory with huge values.
 * <p>
 * The client's IP address is the connection's, unless app.rate-limit.client-ip-header names a header that the
 * hosting proxy overwrites on every request. X-Forwarded-For is never read, because a client can send its own.
 * <p>
 * Not a bean, so the servlet container doesn't register it a second time outside the security chain.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final int MAX_KEY_LENGTH = 255;

    private final String clientIpHeader;
    private final List<Limit> limits;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public RateLimitFilter(RateLimitSettings settings) {
        this(settings, Ticker.systemTicker());
    }

    RateLimitFilter(RateLimitSettings settings, Ticker ticker) {
        this.clientIpHeader = settings.clientIpHeader();
        this.limits = List.of(
                new Limit("/login", Key.CLIENT_IP, 20, TEN_MINUTES, ticker),
                new Limit("/login", Key.EMAIL, 10, TEN_MINUTES, ticker),
                new Limit("/reset-password", Key.CLIENT_IP, 5, ONE_HOUR, ticker),
                new Limit("/reset-password", Key.EMAIL, 3, ONE_HOUR, ticker),
                new Limit("/process_register", Key.CLIENT_IP, 5, ONE_HOUR, ticker),
                new Limit("/organization/joining", Key.CLIENT_IP, 20, ONE_HOUR, ticker),
                new Limit("/organization/joining", Key.ACCOUNT, 10, ONE_HOUR, ticker),
                new Limit("/resend-verification-email", Key.ACCOUNT, 3, ONE_HOUR, ticker),
                new Limit("/contact-us/send-message", Key.CLIENT_IP, 5, ONE_HOUR, ticker));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        for(Limit limit : limits) {
            if(!limit.requests().matches(request))
                continue;

            String key = keyFor(limit.key(), request);
            if(key == null || key.length() > MAX_KEY_LENGTH)
                continue;

            long secondsToWait = limit.counter().secondsToWait(key);
            if(secondsToWait > 0) {
                response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(secondsToWait));
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // null when the request has no such key (no email address typed, nobody signed in): that limit doesn't apply
    private String keyFor(Key key, HttpServletRequest request) {
        return switch(key) {
            case CLIENT_IP -> clientIp(request);
            case EMAIL -> {
                String email = Emails.normalize(request.getParameter("email"));
                yield StringUtils.hasText(email) ? email : null;
            }
            case ACCOUNT -> {
                Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
                yield authentication instanceof UsernamePasswordAuthenticationToken && authentication.isAuthenticated()
                        ? authentication.getName() : null;
            }
        };
    }

    private String clientIp(HttpServletRequest request) {
        if(StringUtils.hasText(clientIpHeader)) {
            String address = request.getHeader(clientIpHeader);
            if(StringUtils.hasText(address))
                return address.strip();
        }

        return request.getRemoteAddr();
    }

    private enum Key { CLIENT_IP, EMAIL, ACCOUNT }

    private record Limit(RequestMatcher requests, Key key, FixedWindowCounter counter) {

        Limit(String path, Key key, int maxPosts, Duration window, Ticker ticker) {
            this(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, path), key,
                    new FixedWindowCounter(maxPosts, window, ticker));
        }
    }
}
