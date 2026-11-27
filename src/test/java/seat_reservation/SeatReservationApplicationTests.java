package seat_reservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import seat_reservation.config.SeatReservationTestcontainersConfig;

@SpringBootTest
@Import(SeatReservationTestcontainersConfig.class)
class SeatReservationApplicationTests {

	@Test
	void contextLoads() {
	}

}
