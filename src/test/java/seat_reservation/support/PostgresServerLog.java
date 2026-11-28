package seat_reservation.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

public class PostgresServerLog implements ServerLog {

	private static final String LINE_PREFIX = "%m [%p] %e";
	
	private final PostgreSQLContainer container;
	
	public PostgresServerLog(PostgreSQLContainer container) {
		this.container = container;
	}
	
	public static String[] command() {
		return new String[] {
				"postgres",
				"-c", "fsync=off",
				
				// 서버가 받은 SQL을 전부 로그로 남기는 설정
				"-c", "log_statement=all",
				
				// 줄마다 밀리초 시각과 프로세스 번호를 붙인다
				"-c", "log_line_prefix=" + LINE_PREFIX
		};
	}
	
	@Override
	public int position() {
		return container.getLogs().length();
	}

	@Override
	public String since(int position) {
		return container.getLogs().substring(position);
	}

	@Override
	public boolean hasReport(String log, DBMSError error) {
		return log.contains("] " + error.sqlState() + " ");
	}

}
