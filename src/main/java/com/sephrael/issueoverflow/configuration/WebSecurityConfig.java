package com.sephrael.issueoverflow.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Spring Security 6 default; remove with the Spring Boot 3 upgrade
                .shouldFilterAllDispatcherTypes(true)

                // paths that anyone can access
                .requestMatchers("/register", "/login", "/process_register").permitAll()
                .requestMatchers("/privacy", "/cookie-policy").permitAll()
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
                .requestMatchers("/issues/*/new", "/issues/*/edit/*", "/issues/*/delete/**", "/issues/new", "/issues/update")
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
                .permitAll()
            )
            // Spring Security 6 default; remove with the Spring Boot 3 upgrade
            .securityContext(context -> context.requireExplicitSave(true));

        return http.build();
    }
}
