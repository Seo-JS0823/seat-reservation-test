package seat_reservation.global.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException e) {
		return createResponse(e.getError());
	}
	
	private ResponseEntity<ProblemDetail> createResponse(ErrorCode error) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				error.getStatus(),
				error.getMessage()
		);
		
		problem.setTitle(error.getCode());
		problem.setProperty("code", error.getCode());
		
		return ResponseEntity
				.status(error.getStatus())
				.body(problem);
	}
	
}