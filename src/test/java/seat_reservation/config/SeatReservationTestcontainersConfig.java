package seat_reservation.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class SeatReservationTestcontainersConfig {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgreContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
	}
}
