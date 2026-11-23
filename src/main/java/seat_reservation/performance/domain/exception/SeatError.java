package seat_reservation.performance.domain.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seat_reservation.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum SeatError implements ErrorCode {

	;
	private final HttpStatus status;
	
	private final String message;

	@Override
	public String getCode() {
		return this.name();
	}
}
