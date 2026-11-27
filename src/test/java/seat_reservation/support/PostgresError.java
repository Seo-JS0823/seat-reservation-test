package seat_reservation.support;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PostgresError implements DBMSError {

	DEADLOCK("40P01", "데드락"),
	SERIALIZATION_FAILURE("40001", "직렬화 실패"),
	UNIQUE_VIOLATION("23505", "유일성 제약조건 위반"),
	FOREIGN_KEY_VIOLATION("23503", "외래키 제약조건 위반"),
	NOT_NULL_VIOLATION("23502", "NOT NULL 위반"),
	CHECK_VIOLATION("23514", "CHECK 제약조건 위반"),
	LOCK_NOT_AVAILABLE("55P03", "락 획득 실패"),
	
	;
	
	private final String sqlState;
	
	private final String description;

	@Override
	public String sqlState() {
		return sqlState;
	}

	@Override
	public String description() {
		return description;
	}
}
