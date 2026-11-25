package seat_reservation.performance.domain.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seat_reservation.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum SeatError implements ErrorCode {

	SESSION_SEAT_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"해당 좌석이 존재하지 않습니다."
	),
	
	SEAT_HOLD_NOT_STATUS(
			HttpStatus.CONFLICT,
			"좌석을 임시 점유 가능한 상태가 아닙니다."
	),
	
	SEAT_HOLDING_NOT_FOUND(
			HttpStatus.CONFLICT,
			"유효한 좌석 점유 내역을 찾을 수 없습니다."
	),
	
	SEAT_HOLD_INVALID_STATE(
			HttpStatus.CONFLICT,
			"이미 처리되었거나 유효하지 않은 좌석 점유 상태입니다."
	),
	
	;
	private final HttpStatus status;
	
	private final String message;

	@Override
	public String getCode() {
		return this.name();
	}
}
