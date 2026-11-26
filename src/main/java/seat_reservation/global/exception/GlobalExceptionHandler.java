package seat_reservation.global.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	// 비즈니스 예외
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ProblemDetail> handleBusinessException(
			BusinessException e,
			HttpServletRequest request
	) {
		ErrorCode error = e.getError();
		
		if (error.getStatus().is5xxServerError()) {
			log.error(
					"[GlobalExceptionHandler]: method={} uri={} code={}",
					request.getMethod(),
					request.getRequestURI(),
					error.getCode(),
					e
			);
		}
		
		else if (log.isDebugEnabled()) {
			log.debug(
					"[GlobalExceptionHandler]: method={} uri={} code={} cause={}",
					request.getMethod(),
					request.getRequestURI(),
					error.getCode(),
					causeName(e)
			);
		}
		
		return createResponse(error);
	}
	
	// 처리하지 못한 모든 예외
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleException(Exception e, HttpServletRequest request) {
		log.error(
				"[UNHANDLED]: method={} uri={}",
				request.getMethod(),
				request.getRequestURI(),
				e
		);
		
		return createResponse(CommonError.INTERNAL_SERVER_ERROR);
	}
	
	// Spring MVC 표준 예외
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception e,
			Object body,
			HttpHeaders headers,
			HttpStatusCode statusCode,
			WebRequest request
	) {
		log.warn(
				"[GlobalExceptionHandler] uri={}, status={}, reason={}",
				((ServletWebRequest) request).getRequest().getRequestURI(),
				statusCode.value(),
				e.getMessage()
		);
		
		return super.handleExceptionInternal(e, body, headers, statusCode, request);
	}
	
	private String causeName(Throwable e) {
		return e.getCause() == null
				? "-"
				: e.getCause().getClass().getSimpleName();
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