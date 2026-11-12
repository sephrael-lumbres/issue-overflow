package com.sephrael.issueoverflow.configuration;

import com.sephrael.issueoverflow.CustomUserDetails;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Logs out a session that no longer matches its account.
 * <p>
 * A session keeps the authorities and password hash it was granted at login.
 * On every request from a logged-in user, this filter compares the session with the stored user.
 * It logs the session out and redirects to {@code /login?expired} (a JSON API request gets a 401 problem-details body instead) when:
 * <ul>
 *   <li>the user was deleted or disabled</li>
 *   <li>the user changed their email address</li>
 *   <li>the user no longer has the role the session was granted</li>
 *   <li>the user's password was changed or reset</li>
 * </ul>
 * Runs before authorization and costs one lookup by email per request.
 * Not a bean, so the servlet container doesn't register it a second time outside the security chain.
 */
public class StaleSessionFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public StaleSessionFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();

        if(authentication instanceof UsernamePasswordAuthenticationToken && authentication.isAuthenticated()
                && isStale(userRepository.findByEmail(authentication.getName()), authentication)) {
            logoutHandler.logout(request, response, authentication);

            if(ApiSecurityErrors.API_REQUESTS.matches(request))
                ApiSecurityErrors.write(response, HttpStatus.UNAUTHORIZED, ApiSecurityErrors.SESSION_ENDED);
            else
                response.sendRedirect(request.getContextPath() + "/login?expired");
            return;
        }

        filterChain.doFilter(request, response);
    }

    static boolean isStale(User user, Authentication authentication) {
        if(user == null || !user.isEnabled() || user.getRole() == null)
            return true;

        // CustomUserDetails is not a CredentialsContainer, so the session keeps the password hash it logged in with
        if(authentication.getPrincipal() instanceof CustomUserDetails details
                && !Objects.equals(details.getPassword(), user.getPassword()))
            return true;

        String role = user.getRole().getName();
        return authentication.getAuthorities().stream().noneMatch(authority -> role.equals(authority.getAuthority()));
    }
}
