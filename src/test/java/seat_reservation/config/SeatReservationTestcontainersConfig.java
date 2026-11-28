package seat_reservation.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class SeatReservationTestcontainersConfig {

	@SuppressWarnings("resource")
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgreContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"))
				.withCommand(
						"postgres",
						"-c", "fsync=off",
						
						// 서버가 받은 SQL을 전부 로그로 남기는 설정
						"-c", "log_statement=all",
						
						// 줄마다 밀리초 시각과 프로세스 번호를 붙인다
						"-c", "log_line_prefix=%m [%p] "
				);
	}
}
