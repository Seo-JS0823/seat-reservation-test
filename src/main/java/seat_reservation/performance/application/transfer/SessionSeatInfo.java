package seat_reservation.performance.application.transfer;

import seat_reservation.performance.domain.model.SeatStatus;
import seat_reservation.performance.domain.model.SessionSeat;

public record SessionSeatInfo(
		Long seatId,
		Integer seatNumber,
		String gradeDisplay,
		Long price,
		SeatStatus status
) {

	public static SessionSeatInfo of(SessionSeat seat) {
		return new SessionSeatInfo(
				seat.getSeatId(),
				seat.getSeatNumber(),
				seat.getGrade().getGradeDisplayName(),
				seat.getPrice(),
				seat.getStatus()
		);
	}
}
