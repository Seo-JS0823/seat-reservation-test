package seat_reservation.performance.domain.exception;

import seat_reservation.global.exception.BusinessException;
import seat_reservation.global.exception.ErrorCode;

public class SeatException extends BusinessException {

	public SeatException(ErrorCode error) {
		super(error);
	}
}
