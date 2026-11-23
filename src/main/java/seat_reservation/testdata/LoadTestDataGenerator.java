package seat_reservation.testdata;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class LoadTestDataGenerator {

	private final JdbcTemplate jdbcTemplate;
	
	@Transactional
	public void generate(LoadTestDataProperties properties) {
		
	}
}
