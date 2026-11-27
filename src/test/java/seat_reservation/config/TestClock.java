package seat_reservation.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestClock {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	
	public static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 1, 12, 0);
	
	@Bean
	@Primary
	Clock fixedClock() {
		return Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
	}
}
