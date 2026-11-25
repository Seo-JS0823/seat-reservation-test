package seat_reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import seat_reservation.performance.application.properties.SeatProperties;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(SeatProperties.class)
public class SeatReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeatReservationApplication.class, args);
	}

}
