package seat_reservation.performance.domain.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seat_reservation.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum PerformanceError implements ErrorCode {

	PERFORMANCE_SESSION_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"요청하신 회차 정보를 찾을 수 없습니다."
	),
	
	;
	private final HttpStatus status;
	
	private final String message;

	@Override
	public String getCode() {
		return this.name();
	}
}
