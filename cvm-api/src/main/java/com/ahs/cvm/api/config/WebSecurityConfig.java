package com.ahs.cvm.api.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Minimal-Konfiguration der Web-Security fuer die Bootstrap-Iteration.
 *
 * <p>Freigabe der Actuator- und OpenAPI-Endpunkte. Fachliche REST-Endpunkte folgen
 * ab Iteration 02, sie werden dann JWT-geschuetzt ueber Keycloak.
 *
 * <p>CORS: Dev-Server (Angular auf Port 4200) greift per Bearer-Token auf das
 * Backend (8081) zu. Erlaubte Origins werden ueber
 * {@code cvm.security.cors.allowed-origins} konfiguriert (Komma-getrennt).
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final List<String> allowedOrigins;

    public WebSecurityConfig(
            @Value("${cvm.security.cors.allowed-origins:http://localhost:4200}")
                    String allowedOrigins) {
        this.allowedOrigins = List.of(allowedOrigins.split("\\s*,\\s*"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                        "/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/info",
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * Konvertiert das eingehende JWT in ein {@link
     * org.springframework.security.authentication.AbstractAuthenticationToken}
     * und extrahiert dabei die Keycloak-Realm-Rollen als Authorities
     * (Iteration 23, CVM-54).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new KeycloakJwtAuthoritiesConverter());
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
