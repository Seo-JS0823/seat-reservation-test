package seat_reservation.testdata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "test-data")
public record LoadTestDataProperties(
		boolean enabled,
		boolean reset,
		String allowedDatabase,
		long performanceCount,
		long performanceCountLimit,
		int minSessionsPerPerformance,
		int maxSessionsPerPerformance,
		int sessionsPerPerformanceLimit,
		int minSeatsPerSession,
		int maxSeatsPerSession,
		LocalDate startDate,
		LocalDate endDate
) {

	private static final String ERROR_PERFIX_FORMATTED = "[LoadTestDataProperties]: %s: %s";
	
	public LoadTestDataProperties {
		if (enabled) {
			List<String> errors = new ArrayList<>();
			
			if (!StringUtils.hasText(allowedDatabase)) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"allowedDatabase",
						"테스트 데이터베이스를 설정해야 합니다."
				));
			}
			
			if (startDate == null || endDate == null) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"start-date, end-date",
						"테스트 생성 데이터의 시작일과 종료일을 설정해야 합니다."
				));
			} else if (startDate.isAfter(endDate)) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"start-date",
						"end-date보다 늦을 수 없습니다."
				));
			}
			
			if (performanceCount < 1) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"performance-count",
						"공연 수는 1 이상이어야 합니다."
				));
			} else if (performanceCount > performanceCountLimit) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"performance-acount",
						"공연 수는 max-performance-count 이하여야 합니다."
				));
			}
			
			if (minSessionsPerPerformance < 1) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"min-sessions-per-performance",
						"최소 공연 회차는 1 이상이어야 합니다."
				));
			}
			
			if (maxSessionsPerPerformance < 1) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"max-sessions-per-performance",
						"최대 공연 회차는 1 이상이어야 합니다."
				));
			} else if (maxSessionsPerPerformance > sessionsPerPerformanceLimit) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"max-sessions-per-performance",
						"최대 공연 회차는 10 이하여야 합니다."
				));
			}
			
			if (maxSessionsPerPerformance < minSessionsPerPerformance) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"max-sessions-per-performance",
						"최대 공연 회차는 최소 공연 회차 이상이어야 합니다."
				));
			}
			
			if (maxSeatsPerSession < minSeatsPerSession) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"max-seats-per-session",
						"최대 좌석 수는 최소 좌석 수 이상이어야 합니다."
				));
			}
			
			boolean validSeatRange = (maxSeatsPerSession - minSeatsPerSession) % 10 != 0;
			
			if (validSeatRange) {
				errors.add(ERROR_PERFIX_FORMATTED.formatted(
						"max-seats-per-session",
						"최소 좌석 수와의 차이는 10의 배수여야 합니다."
				));
			}
			
			if (!errors.isEmpty()) {
				throw new IllegalStateException("테스트 데이터 설정 검증 실패:\n" + String.join("\n", errors));
			}
		}
	}
}
