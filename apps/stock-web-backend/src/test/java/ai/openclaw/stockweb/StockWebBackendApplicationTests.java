package ai.openclaw.stockweb;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StockWebBackendApplicationTests {

	@Test
	void applicationTestHarnessLoads() {
		assertEquals("ai.openclaw.stockweb", StockWebBackendApplication.class.getPackageName());
	}

}
