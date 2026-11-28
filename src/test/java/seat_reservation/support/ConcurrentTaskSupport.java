package seat_reservation.support;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import seat_reservation.global.exception.BusinessException;

public class ConcurrentTaskSupport {

	private static final long TIMEOUT_SECONDS = 10L;
	
	private ConcurrentTaskSupport() {}
	
	/**
	 * 작업이 끝날 때까지 최대 {@value #TIMEOUT_SECONDS}초 기다리면서 성공이면 null
	 * 실패면 던진 예외를 돌려준다.
	 */
	public static final Throwable failureOf(Future<?> task) throws InterruptedException, TimeoutException {
		try {
			task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			return null;
		} catch (ExecutionException e) {
			return e.getCause();
		}
	}
	
	/**
	 * 실행 결과를 문자열로 표현한다.
	 */
	public static final String label(Throwable error) {
		if (error == null) {
			return ResultCode.SUCCESS.code();
		}
		
		if (error instanceof BusinessException bx) {
			return bx.getError().getCode();
		}
		
		String sqlError = classifySqlException(error);
		
		if (sqlError != null) {
			return sqlError;
		}
		
		return error.getClass().getSimpleName();
	}
	
	private static final String classifySqlException(Throwable error) {
		return DBMSError.sqlStateOf(error)
				.map(state -> DBMSError.find(state, PostgresError.values())
						.map(DBMSError::label)
						.orElse("SQL 예외(" + state + ")"))
				.orElse(null);
	}
}
