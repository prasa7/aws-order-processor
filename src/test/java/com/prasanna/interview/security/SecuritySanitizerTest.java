package com.prasanna.interview.security;

import com.prasanna.interview.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecuritySanitizerTest {

    @Test
    void masksEmailAddress() {
        SecuritySanitizer sanitizer = new SecuritySanitizer(TestFixtures.properties());

        assertThat(sanitizer.maskEmail("ada.lovelace@example.com")).isEqualTo("a***@example.com");
    }

    @Test
    void optionallyMasksCustomerId() {
        SecuritySanitizer sanitizer = new SecuritySanitizer(
                TestFixtures.properties(false, false, 262_144, true, true)
        );

        assertThat(sanitizer.sanitizeCustomerId("customer-12345")).isEqualTo("**********2345");
    }

    @Test
    void stripsControlCharactersFromErrors() {
        SecuritySanitizer sanitizer = new SecuritySanitizer(TestFixtures.properties());

        assertThat(sanitizer.sanitizeErrorMessage("bad\u0000message")).isEqualTo("badmessage");
    }
}
