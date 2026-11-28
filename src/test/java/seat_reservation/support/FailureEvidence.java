package seat_reservation.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 테스트 실행 결과에서 중요한 실패를 골라 예외 체인과 서버 로그를 파일로 남긴다.
 * 무엇을 기록할지는 생성할 때 정하고, 로그 구간은 start()가 돌려주는 Window가 기억한다.
 */
public class FailureEvidence {

	private static final Path DIR = Path.of("build", "failure-evidence");
	private static final int REPORT_WAIT_TRIES = 10;
	private static final long REPORT_WAIT_MILLIS = 100;
	
	private final ServerLog serverLog;
	private final DBMSError[] targets;
	
	public FailureEvidence(ServerLog serverLog, DBMSError... targets) {
		this.serverLog = serverLog;
		this.targets = targets.clone();
	}
	
	public Window start() {
		return new Window(serverLog.position());
	}
	
	public final class Window {
		
		private final int position;
		
		private Window(int position) {
			this.position = position;
		}
		
		/**
		 * 실패가 기록 대상일 때 파일로 남기고 그 경로를 돌려준다.
		 * 성공이거나 대상이 아니면 비어있다.
		 */
		public Optional<Path> save(String name, Throwable failure) throws IOException, InterruptedException {
			Optional<DBMSError> target = DBMSError.sqlStateOf(failure)
					.flatMap(state -> DBMSError.find(state, targets));
			
			if (target.isEmpty()) {
				return Optional.empty();
			}
			
			String content = """
				== A. 요약 ==
				이름: %s
				오류: %s
				
				== B. 예외 체인 (바깥 → 안쪽) ==
				%s
				== C. 이 구간의 서버 로그 ==
				%s
		""".formatted(
				name, target.get().label(),
				causeChain(failure),
				waitForReport(target.get())
				);
			
			Files.createDirectories(DIR);
			Path file = DIR.resolve(name + ".txt");
			Files.writeString(file, content);
			return Optional.of(file);
		}
		
		/**
		 * 도커 로그가 비동기로 모이기 때문에 오류가 보일 때까지 잠깐 다시 읽는다.
		 */
		private String waitForReport(DBMSError error) throws InterruptedException {
			String log = serverLog.since(position);
			
			for (int i = 0; i < REPORT_WAIT_TRIES && !serverLog.hasReport(log, error); i++) {
				Thread.sleep(REPORT_WAIT_MILLIS);
				log = serverLog.since(position);
			}
			
			return log;
		}
		
		private static String causeChain(Throwable error) {
			StringBuilder sb = new StringBuilder();
			int depth = 0;
			
			for (Throwable t = error; t != null; t = t.getCause()) {
				sb.append("[").append(depth++).append("]").append(t.getClass().getName()).append('\n')
					.append("    ").append(t.getMessage()).append('\n');
			}
			
			return sb.toString();
		}
	}
	
}
