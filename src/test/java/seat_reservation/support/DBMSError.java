package seat_reservation.support;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

/*
 * 데이터베이스에서 던지는 에러코드 변환 인터페이스
 */
public interface DBMSError {
	public String sqlState();
	
	public String description();
	
	default String label() {
		return description() + "(" + sqlState() + ")";
	}
	
	default boolean isCauseOf(Throwable error) {
		return DBMSError.sqlStateOf(error)
				.map(sqlState()::equals)
				.orElse(false);
	}
	
	static Optional<DBMSError> find(String sqlState, DBMSError[] errors) {
		return Arrays.stream(errors)
				.filter(error -> error.sqlState().equals(sqlState))
				.findFirst();
	}
	
	/**
	 * 예외 체인에서 처음 만나는 SQLState를 반환한다.
	 */
	static Optional<String> sqlStateOf(Throwable error) {
		for (Throwable t = error; t != null; t = t.getCause()) {
			if (t instanceof SQLException sql && sql.getSQLState() != null) {
				return Optional.of(sql.getSQLState());
			}
		}
		return Optional.empty();
	}
}
