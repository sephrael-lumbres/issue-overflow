package com.sephrael.issueoverflow.configuration;

import com.sephrael.issueoverflow.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    // Spring Security builds the DaoAuthenticationProvider from the single
    // UserDetailsService bean (CustomUserDetailsService) and this encoder.
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository userRepository) throws Exception {
        http
            // logs out sessions whose role or account changed since login, before the rules below see them
            .addFilterBefore(new StaleSessionFilter(userRepository), AuthorizationFilter.class)
            .authorizeHttpRequests(authorize -> authorize
                // paths that anyone can access
                .requestMatchers("/register", "/login", "/process_register").permitAll()
                .requestMatchers("/privacy").permitAll()
                .requestMatchers("/getting-started", "/getting-started/*", "/knowledge-base/**", "/contact-us/**").permitAll()
                .requestMatchers("/reset-password", "/change-password*", "/verify-email*").permitAll()
                .requestMatchers("/error", "/js/**", "/assets/**", "/css/**").permitAll()

                // Update and Removal operations on Users allowed only to Project Managers
                .requestMatchers("/users/edit/*", "/users/update/*", "/users/remove/**").hasAuthority("Project Manager")

                // Update operations on Organization allowed only to Project Managers
                .requestMatchers("/organization/update").hasAuthority("Project Manager")

                // CRUD operations on Projects allowed only to Project Managers
                .requestMatchers("/projects/new", "/projects/save", "/projects/delete/*").hasAnyAuthority("Project Manager")

                // CRUD operations on Issues allowed to Project Managers, Admins, and Developers
                .requestMatchers("/issues/*/new", "/issues/*/edit/*", "/issues/*/delete/**", "/issues/new", "/issues/update", "/issues/*/view/*/status")
                    .hasAnyAuthority("Project Manager", "Admin", "Developer")

                // view Issues allowed to all Roles
                .requestMatchers("/issues/*/view/*").hasAnyAuthority("Project Manager", "Admin", "Developer", "Guest")

                // deletion of Files only allowed to PMs, Admins, and Developers
                .requestMatchers("/storage/*/*/delete/*").hasAnyAuthority("Project Manager", "Admin", "Developer")

                // CRUD operations on Comments allowed to all Roles
                .requestMatchers("/issues/*/view/*/comment/**").hasAnyAuthority("Project Manager", "Admin", "Developer", "Guest")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/dashboard")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login")
                .permitAll())
            // the JSON API answers 401 and 403 with a problem-details body; pages keep the login redirect and error pages
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(ApiSecurityErrors.entryPoint(), ApiSecurityErrors.API_REQUESTS)
                // every other request keeps the login redirect, whatever its Accept header
                .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), AnyRequestMatcher.INSTANCE)
                .accessDeniedHandler(ApiSecurityErrors.accessDeniedHandler()))
            .requestCache(cache -> cache.requestCache(pageRequestCache()));

        return http.build();
    }

    // Saves the page a logged-out user asked for, so login can send them back to it. Only GETs are saved, never /api/ calls
    // (login would land on raw JSON) or the /favicon.ico and /.well-known/ requests that browsers and their tools make on their own.
    private static RequestCache pageRequestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setRequestMatcher(new AndRequestMatcher(
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/**"),
                new NegatedRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/favicon.ico")),
                new NegatedRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/.well-known/**")),
                new NegatedRequestMatcher(ApiSecurityErrors.API_REQUESTS)));
        return requestCache;
    }
}
