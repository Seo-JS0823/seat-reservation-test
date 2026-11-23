package seat_reservation.performance.application.transfer;

import java.time.LocalDateTime;

public record PerformanceSessionInfo(
		Long sessionId,
		Long performanceId,
		LocalDateTime startAt
) {

}
