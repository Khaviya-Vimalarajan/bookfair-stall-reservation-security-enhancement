package com.bookfair.Stall_Reservation.config;

import com.bookfair.Stall_Reservation.security.OidcUserMappingFilter;
import com.bookfair.Stall_Reservation.security.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtValidators;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final OidcUserMappingFilter oidcUserMappingFilter;


    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Value("${app.security.oauth2.audience}")
    private String audience;

    @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173}")
    private String allowedOrigins;

    public SecurityConfig(OidcUserMappingFilter oidcUserMappingFilter) {
        this.oidcUserMappingFilter = oidcUserMappingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF for stateless bearer token endpoints (no session cookies used)
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> {
                            String baseCsp = "default-src 'self'; " +
                                    "script-src 'self'; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "img-src 'self' data: blob:; " +
                                    "font-src 'self' data:; " +
                                    "connect-src 'self'";
                            if (StringUtils.hasText(issuerUri)) {
                                baseCsp += " " + issuerUri;
                            }
                            baseCsp += "; frame-ancestors 'none'; base-uri 'self'; form-action 'self';";
                            csp.policyDirectives(baseCsp);
                        })
                )
                // Stateless session management
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // URL access rules
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints
                        .requestMatchers("/api/public/**").permitAll()     
                        .requestMatchers("/api/events/**").permitAll()     
                        .requestMatchers("/ws/**", "/uploads/**").permitAll() 

                        // Exhibition Organizer only
                        .requestMatchers("/api/admin/**", "/api/organizer/**")
                        .hasAuthority("ROLE_EXHIBITION_ORGANIZER")

                        // Stall Vendor & Exhibition Organizer
                        .requestMatchers("/api/profile/**", "/api/reservations/**", "/api/vendor/**")
                        .hasAnyAuthority("ROLE_STALL_VENDOR", "ROLE_EXHIBITION_ORGANIZER")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // Configure OAuth2 Resource Server for access token validation
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // Centralized error entrypoint for unauthenticated requests
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"message\": \"Unauthenticated\", \"error\": \"Unauthorized\"}");
                        })
                )
                // Map token to database User entity after token authentication
                .addFilterAfter(new RateLimitingFilter(), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(oidcUserMappingFilter, RateLimitingFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (!StringUtils.hasText(issuerUri)) {
            throw new IllegalStateException(
                    "OIDC issuer configuration is missing!"
            );
        }

        NimbusJwtDecoder decoder =
                (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        // Standard validation: issuer, expiry, not-before, etc.
        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(issuerUri);

        // Ensure the token was issued specifically for the BookFair API
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (jwt.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "The required audience is missing",
                    null
            );

            return OAuth2TokenValidatorResult.failure(error);
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                )
        );

        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName(
                "https://bookfair-app/roles"
        );

        JwtAuthenticationConverter jwtAuthenticationConverter =
                new JwtAuthenticationConverter();

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                grantedAuthoritiesConverter
        );

        return jwtAuthenticationConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Set allowed origins from configuration securely
        if (StringUtils.hasText(allowedOrigins)) {
            config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        } else {
            config.setAllowedOrigins(List.of("http://localhost:5173"));
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
