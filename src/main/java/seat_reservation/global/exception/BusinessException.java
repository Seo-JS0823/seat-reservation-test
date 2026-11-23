package seat_reservation.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode error;
	
	public BusinessException(ErrorCode error) {
		super(error.getMessage());
		
		this.error = error;
	}
}
