package seat_reservation.testdata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "test-data")
public record LoadTestDataProperties(
		boolean enabled,
		boolean reset,
		String allowedDatabase,
		long performanceCount,
		int minSessionsPerPerformance,
		int maxSessionsPerPerformance,
		int minSeatsPerSession,
		int maxSeatsPerSession,
		LocalDate startDate,
		LocalDate endDate
) {

	public LoadTestDataProperties {
		List<String> errors = new ArrayList<>();
		
		if (allowedDatabase == null || allowedDatabase.isBlank()) {
			errors.add("[LoadTestDataProperties]: allowed-database: 로드 테스트 데이터베이스를 설정해야 합니다.");
		}
		
		if (startDate == null || endDate == null) {
			errors.add("[LoadTestDataProperties]: start-date, end-date: 테스트 생성 데이터의 시작일과 종료일을 설정해야 합니다.");
		} else if (startDate.isAfter(endDate)) {
			errors.add("[LoadTestDataProperties]: start-date: end-date 보다 늦을 수 없습니다.");
		}
		
		if (performanceCount < 1) {
			errors.add("[LoadTestDataProperties]: performance-count: 공연 수는 1 이상이어야 합니다.");
		}
		
		if (performanceCount > 100_000) {
			errors.add("[LoadTestDataProperties]: performance-count: 최대 공연 수는 100_000 제한입니다.");
		}
		
		if (maxSessionsPerPerformance > 10) {
			errors.add("[LoadTestDataProperties]: max-sessions-per-performance: 최대 공연 회차는 10 이하여야 합니다.");
		}
		
		if (minSessionsPerPerformance < 1) {
			errors.add("[LoadTestDataProperties]: min-sessions-per-performance: 최소 공연 회차는 1 이상이어야 합니다.");
		}
		
		if (maxSessionsPerPerformance < 1) {
			errors.add("[LoadTestDataProperties]: max-sessions-per-performance: 최대 공연 회차는 1 이상이어야 합니다.");
		}
		
		if (maxSessionsPerPerformance < minSessionsPerPerformance) {
			errors.add("[LoadTestDataProperties]: max-sessions-per-performance: 최대 공연 회차는 최소 공연 회차보다 이상이어야 합니다.");
		}
		
		if (minSeatsPerSession < 100) {
			errors.add("[LoadTestDataProperties]: min-seats-per-session: 최소 좌석 수는 100 이상이어야 합니다.");
		}
		
		if (maxSeatsPerSession < minSeatsPerSession) {
			errors.add("[LoadTestDataProperties]: max-seats-per-session: 최대 좌석 수는 최소 좌석 수 이상이어야 합니다.");
		}
		
		if (maxSeatsPerSession > 10_000) {
			errors.add("[LoadTestDataProperties]: max-seats-per-session: 최대 좌석 수는 10_000 이하여야 합니다.");
		}
		
		if (!errors.isEmpty()) {
			String message = "테스트 데이터 설정 검증 실패:\n" + String.join("\n", errors);
			throw new IllegalArgumentException(message);
		}
	}
}
