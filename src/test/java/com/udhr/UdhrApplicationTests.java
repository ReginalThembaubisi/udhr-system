package com.udhr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * A minimal smoke test: the full Spring context (security config, JWT
 * secret validation, CORS/RestTemplate beans, DataSeeder) must wire up and
 * seed against a real database without errors. This is intentionally the
 * first test in the project -- see README.md's "Known limitations".
 */
@SpringBootTest
@ActiveProfiles("test")
class UdhrApplicationTests {

    @Test
    void contextLoads() {
    }
}
