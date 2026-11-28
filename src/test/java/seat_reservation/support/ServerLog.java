package seat_reservation.support;

/**
 * DB 서버 로그를 읽는 인터페이스
 */
public interface ServerLog {

	/**
	 * 지금까지 쌓인 로그의 위치
	 */
	int position();
	
	/**
	 * position 이후에 쌓인 로그
	 */
	String since(int position);
	
	/**
	 * 로그에 해당 오류가 들어 있는지
	 */
	boolean hasReport(String log, DBMSError error);
}
