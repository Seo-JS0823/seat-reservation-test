package seat_reservation.performance.application.transfer;

import java.time.LocalDateTime;
import java.util.UUID;

import seat_reservation.performance.domain.model.SeatGrade;
import seat_reservation.performance.domain.model.SeatStatus;

public record SessionSeatHold(
		Long seatId,
		Long sessionId,
		Integer seatNumber,
		SeatGrade grade,
		Long price,
		SeatStatus status,
		UUID currentHeldId,
		LocalDateTime heldExpiresAt,
		UUID holdId
) {

}
