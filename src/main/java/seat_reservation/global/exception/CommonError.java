package seat_reservation.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonError implements ErrorCode {

	
	;
	private final HttpStatus status;
	
	private final String message;

	@Override
	public String getCode() {
		return this.name();
	}
}
