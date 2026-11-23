package seat_reservation.performance.domain.exception;

import seat_reservation.global.exception.BusinessException;
import seat_reservation.global.exception.ErrorCode;

public class PerformanceException extends BusinessException {

	public PerformanceException(ErrorCode error) {
		super(error);
	}

}
