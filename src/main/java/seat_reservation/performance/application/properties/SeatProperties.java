package seat_reservation.performance.application.properties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seat.hold")
public record SeatProperties(
		Duration ttl,
		Duration expirationInterval,
		int expirationBatchSize
) {

	private static final String ERROR_PERFIX_FORMATTED = "[SeatProperties]: seat.hold.%s: %s";
	
	public SeatProperties {
		List<String> errors = new ArrayList<>();
		
		if (ttl == null || ttl.isNegative() || ttl.isZero()) {
			errors.add(ERROR_PERFIX_FORMATTED.formatted(
					"ttl",
					"좌석 임시 점유 가능한 시간 올바르게 설정해주세요."
			));
		}
		
		if (expirationInterval == null || expirationInterval.isNegative() || expirationInterval.isZero()) {
			errors.add(ERROR_PERFIX_FORMATTED.formatted(
					"expiration-interval",
					"점유 만료 스케줄링 실행 간격을 설정해주세요."
			));
		}
		
		if (expirationBatchSize <= 0) {
			errors.add(ERROR_PERFIX_FORMATTED.formatted(
					"expiration-batch-size",
					"점유 만료 스케줄링 처리 사이즈는 1 이상이어야 합니다."
			));
		}
		
		if (!errors.isEmpty()) {
			throw new IllegalStateException("좌석 설정 검증 실패:\n" + String.join("\n", errors));
		}
	}
}
