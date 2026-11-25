package com.picnic.psi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
final class StoreIntelligenceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads cleanly against H2
    }
}
