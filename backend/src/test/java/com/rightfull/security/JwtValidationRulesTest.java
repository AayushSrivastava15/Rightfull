package com.rightfull.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtValidationRulesTest {

    private final String expectedIssuer = "https://kcyccddrzsfxgrkeenop.supabase.co/auth/v1";
    private final String expectedAudience = "authenticated";

    private DelegatingOAuth2TokenValidator<Jwt> createValidator() {
        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(60));
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(expectedIssuer);

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
                        new OAuth2Error("invalid_audience", "Invalid audience", null)
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

        return new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                issuerValidator,
                audienceValidator,
                subjectValidator
        );
    }

    @Test
    @DisplayName("Valid JWT claims passes validation successfully")
    void validJwtPassesValidation() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("valid.token")
                .header("alg", "ES256")
                .subject(sub.toString())
                .issuer(expectedIssuer)
                .claim("aud", "authenticated")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = createValidator().validate(jwt);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Expired JWT fails validation")
    void expiredJwtFailsValidation() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("expired.token")
                .header("alg", "ES256")
                .subject(sub.toString())
                .issuer(expectedIssuer)
                .claim("aud", "authenticated")
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = createValidator().validate(jwt);
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Incorrect issuer fails validation")
    void incorrectIssuerFailsValidation() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("wrong.issuer.token")
                .header("alg", "ES256")
                .subject(sub.toString())
                .issuer("https://malicious-issuer.com/auth")
                .claim("aud", "authenticated")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = createValidator().validate(jwt);
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Incorrect audience fails validation")
    void incorrectAudienceFailsValidation() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("wrong.aud.token")
                .header("alg", "ES256")
                .subject(sub.toString())
                .issuer(expectedIssuer)
                .claim("aud", "unauthorized_app")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = createValidator().validate(jwt);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> "invalid_audience".equals(e.getErrorCode()));
    }

    @Test
    @DisplayName("Invalid UUID subject fails validation")
    void invalidSubjectFailsValidation() {
        Jwt jwt = Jwt.withTokenValue("bad.sub.token")
                .header("alg", "ES256")
                .subject("not-a-valid-uuid-12345")
                .issuer(expectedIssuer)
                .claim("aud", "authenticated")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = createValidator().validate(jwt);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> "invalid_subject".equals(e.getErrorCode()));
    }
}

