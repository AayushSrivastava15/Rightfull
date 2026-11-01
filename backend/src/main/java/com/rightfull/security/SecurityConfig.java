package com.rightfull.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

/**
 * Spring Security Configuration with Supabase OAuth2 Resource Server & JWT validation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final String jwksUri;
    private final String issuer;
    private final String expectedAudience;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            @Value("${supabase.jwt.jwks-url:https://kcyccddrzsfxgrkeenop.supabase.co/auth/v1/.well-known/jwks.json}") String jwksUri,
            @Value("${supabase.jwt.issuer:https://kcyccddrzsfxgrkeenop.supabase.co/auth/v1}") String issuer,
            @Value("${supabase.jwt.expected-audience:authenticated}") String expectedAudience,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {
        this.jwksUri = jwksUri;
        this.issuer = issuer;
        this.expectedAudience = expectedAudience;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/health/**",
                                "/api/v1/licenses/validate",
                                "/api/v1/licenses/activate",
                                "/api/v1/licenses/deactivate",
                                "/api/v1/licenses/heartbeat",
                                "/favicon.ico",
                                "/static/**",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(org.springframework.core.env.Environment env) {
        if (env.matchesProfiles("test")) {
            log.info("Test profile detected: initializing offline dummy JwtDecoder");
            return token -> {
                throw new org.springframework.security.oauth2.jwt.BadJwtException("Offline test decoder: provide mocked Jwt in test suite");
            };
        }

        log.info("Configuring Supabase NimbusJwtDecoder with JWKS URI: {}", jwksUri);

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithms(algs -> {
                    algs.add(SignatureAlgorithm.ES256);
                    algs.add(SignatureAlgorithm.RS256);
                })
                .build();

        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(60));
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = new OAuth2TokenValidator<>() {
            @Override
            public OAuth2TokenValidatorResult validate(Jwt token) {
                Object aud = token.getClaims().get("aud");
                if (aud instanceof String s && (s.equals(expectedAudience) || s.equals("authenticated"))) {
                    return OAuth2TokenValidatorResult.success();
                } else if (aud instanceof Collection<?> list) {
                    for (Object item : list) {
                        if (expectedAudience.equals(item) || "authenticated".equals(item)) {
                            return OAuth2TokenValidatorResult.success();
                        }
                    }
                }
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_audience", "The token audience does not match expected audience: " + expectedAudience, null)
                );
            }
        };

        OAuth2TokenValidator<Jwt> subjectValidator = new OAuth2TokenValidator<>() {
            @Override
            public OAuth2TokenValidatorResult validate(Jwt token) {
                String sub = token.getSubject();
                if (sub == null || sub.isBlank()) {
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_subject", "Subject claim (sub) is missing or empty", null)
                    );
                }
                try {
                    UUID.fromString(sub);
                    return OAuth2TokenValidatorResult.success();
                } catch (IllegalArgumentException e) {
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_subject", "Subject claim (sub) is not a valid UUID: " + sub, null)
                    );
                }
            }
        };

        OAuth2TokenValidator<Jwt> delegatingValidator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                issuerValidator,
                audienceValidator,
                subjectValidator
        );

        decoder.setJwtValidator(delegatingValidator);
        return decoder;
    }
}
