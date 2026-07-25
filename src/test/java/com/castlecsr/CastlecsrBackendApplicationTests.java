package com.castlecsr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // usa application-test.properties con H2 (no requiere PostgreSQL local)
class CastlecsrBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}