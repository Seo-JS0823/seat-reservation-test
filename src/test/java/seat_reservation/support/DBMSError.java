package seat_reservation.support;

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
	
	static Optional<DBMSError> find(String sqlState, DBMSError[] errors) {
		return Arrays.stream(errors)
				.filter(error -> error.sqlState().equals(sqlState))
				.findFirst();
	}
}
