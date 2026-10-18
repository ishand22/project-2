package com.entitleflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(@Value("${app.username}") String username, @Value("${app.password}") String password, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername(username).password(encoder.encode(password)).roles("ADMIN").build(),
            User.withUsername("operations").password(encoder.encode(password)).roles("OPERATIONS").build(),
            User.withUsername("viewer").password(encoder.encode(password)).roles("READ_ONLY").build());
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults()).httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "OPERATIONS", "READ_ONLY")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "OPERATIONS").anyRequest().authenticated()).build();
    }
    @Bean CorsConfigurationSource corsSource(@Value("${app.cors-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration(); config.setAllowedOrigins(List.of(origins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS")); config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", config); return source;
    }
}
