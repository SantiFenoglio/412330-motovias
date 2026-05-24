package _0.motovias_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // usa H2 en lugar de PostgreSQL
class MotoviasBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
