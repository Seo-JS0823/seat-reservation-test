package seat_reservation.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import seat_reservation.support.PostgresServerLog;
import seat_reservation.support.ServerLog;

@TestConfiguration(proxyBeanMethods = false)
public class SeatReservationTestcontainersConfig {

	@SuppressWarnings("resource")
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgreContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"))
				.withCommand(PostgresServerLog.command());
	}
	
	@Bean
	ServerLog serverLog(PostgreSQLContainer postgresContainer) {
		return new PostgresServerLog(postgresContainer);
	}
}
