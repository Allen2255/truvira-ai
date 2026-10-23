package com.truvira.security;

import com.truvira.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SPRING SECURITY setup. In simple words: a security guard that checks EVERY request
 * BEFORE it reaches our controllers.
 *
 * @Configuration = this class contains settings; its @Bean methods create objects Spring manages.
 */
@Configuration
public class SecurityConfig {

    /**
     * The FILTER CHAIN = the list of checks each request passes through, in order.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS: reuse the @CrossOrigin rules on our controllers, so React (5173) is still allowed.
            .cors(Customizer.withDefaults())

            // CSRF protection is needed when the browser sends login COOKIES automatically.
            // We send the login in an Authorization HEADER that only our own JavaScript adds,
            // so another website can't make the browser send it -> CSRF protection isn't needed here.
            .csrf(csrf -> csrf.disable())

            // STATELESS = no server-side login session: every request carries the username + password.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // WHO may call WHAT (checked top to bottom, first match wins):
            .authorizeHttpRequests(auth -> auth
                    // Browsers send an OPTIONS "preflight" before cross-origin calls, without the login.
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/error").permitAll()          // Spring's own error page
                    .requestMatchers("/api/**").authenticated()     // everything in our API needs a login
                    .anyRequest().denyAll())                        // anything else: no

            // HTTP BASIC login: header  "Authorization: Basic base64(username:password)"
            .httpBasic(basic -> basic.authenticationEntryPoint(jsonLoginRequired()))
            .exceptionHandling(e -> e.authenticationEntryPoint(jsonLoginRequired()));

        return http.build();
    }

    /**
     * What to answer when there's no valid login: 401 + our usual {"error": ...} JSON.
     * We deliberately DON'T send the "WWW-Authenticate: Basic" header, otherwise the browser
     * would pop up its own ugly login box instead of our React login form.
     */
    private AuthenticationEntryPoint jsonLoginRequired() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Login required: wrong or missing username/password\"}");
        };
    }

    /**
     * PASSWORD ENCODER: BCrypt. Turns a password into a salted, slow hash for storage,
     * and checks a typed password against a stored hash at login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * USER DETAILS SERVICE: how Spring Security finds a user by name.
     * We load our AppUser row and convert it into Spring's own "User" object.
     * A LAMBDA (username -> ...) implements the interface's single method.
     */
    @Bean
    public UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> users.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No user " + username));
    }
}
