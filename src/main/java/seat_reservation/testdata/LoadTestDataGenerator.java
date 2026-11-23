package seat_reservation.testdata;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class LoadTestDataGenerator {

	private static final String CURRENT_DATABASE_SQL = """
			SELECT current_database()
	""";
	
	private static final String DATA_EXISTS_SQL = """
			SELECT
					EXISTS (
							SELECT 1
							FROM seat_reservation.performances
					)
					OR EXISTS (
							SELECT 1
							FROM seat_reservation.performance_sessions
					)
	""";
	
	private static final String TRUNCATE_SQL = """
			TRUNCATE TABLE
					seat_reservation.performance_sessions,
					seat_reservation.performances
	""";
	
	private static final String INSERT_PERFORMANCES_SQL = """
			INSERT INTO seat_reservation.performances (
					performance_id,
					title
			)
			SELECT
					number,
					'부하 테스트 공연' || LPAD(number::TEXT, 6, '0')
			FROM generate_series(
					1,
					CAST(? AS BIGINT)
			) AS number
	""";
	
	private static final String INSERT_SESSIONS_SQL = """
			WITH expanded_sessions AS (
					SELECT
							performance.performance_id,
							session_series.session_number
					FROM seat_reservation.performances AS performance
					CROSS JOIN LATERAL generate_series(
							1,
							CAST(
									CAST(? AS INTEGER)
									+ MOD(
											performance.performance_id - 1,
											CAST(? AS BIGINT)
									) AS INTEGER
							)
					) AS session_series(session_number)
			),
			numbered_sessions AS (
					SELECT
							ROW_NUMBER() OVER (
									ORDER BY performance_id, session_number
							) AS session_id,
							performance_id
					FROM expanded_sessions
			)
			INSERT INTO seat_reservation.performance_sessions (
					session_id,
					performance_id,
					start_at
			)
			SELECT
					session_id,
					performance_id,
					CAST(? AS TIMESTAMP)
							+ CAST(
									MOD(
											session_id - 1,
											CAST(? AS BIGINT)
									) AS INTEGER
							) * INTERVAL '1 day'
							+ CAST(
									MOD(
											(session_id - 1) / CAST(? AS BIGINT),
											CAST(7 AS BIGINT)
									) AS INTEGER
							) * INTERVAL '2 hours'
							+ INTERVAL '10 hours'
			FROM numbered_sessions
	""";
	
	private static final String PERFORMANCE_COUNT_SQL = """
			SELECT COUNT(*)
			FROM seat_reservation.performances
	""";
	
	private static final String SESSION_COUNT_SQL = """
			SELECT COUNT(*)
			FROM seat_reservation.performance_sessions
  """;
	
	private final JdbcTemplate jdbcTemplate;
	
	@Transactional
	public void generate(LoadTestDataProperties properties) {
		// 1. 현재 DB 확인
		verifyDatabase(properties.allowedDatabase());
		
		// 2. 기존 데이터 초기화
		prepareTables(properties.reset());
		
		// 3. 공연 생성
		long dayCount = ChronoUnit.DAYS.between(
				properties.startDate(),
				properties.endDate()
		) + 1;
		
		int sessionRange = properties.maxSessionsPerPerformance() - properties.minSessionsPerPerformance() + 1;
		
		int insertedPerformances = jdbcTemplate.update(
				INSERT_PERFORMANCES_SQL,
				properties.performanceCount()
		);
		
		// 4. 회차 생성
		int insertedSessions = jdbcTemplate.update(
				INSERT_SESSIONS_SQL,
				properties.minSessionsPerPerformance(),
				sessionRange,
				Timestamp.valueOf(properties.startDate().atStartOfDay()),
				dayCount,
				dayCount
		);
		
		// 5. 결과 확인 및 로그 출력
		verifyResult(properties);
		
		log.info(
				"부하 테스트 데이터 생성 완료: performances={}, sessions={}",
				insertedPerformances,
				insertedSessions
		);
		
	}
	
	private void verifyDatabase(String allowedDatabase) {
		String currentDatabase = jdbcTemplate.queryForObject(
				CURRENT_DATABASE_SQL,
				String.class
		);
		
		if (!Objects.equals(currentDatabase, allowedDatabase)) {
			throw new IllegalStateException(
					"테스트 데이터 생성을 거부했습니다. 허용된 DB=$s, 현재 DB=%s".formatted(allowedDatabase, currentDatabase)
			);
		}
	}
	
	private void prepareTables(boolean reset) {
		if (reset) {
			jdbcTemplate.execute(TRUNCATE_SQL);
			log.info("[LoadTestDateGenerator] 기존 부하 테스트 데이터를 초기화했습니다.");
			return;
		}
		
		Boolean dataExists = jdbcTemplate.queryForObject(
				DATA_EXISTS_SQL,
				Boolean.class
		);
		
		if (Boolean.TRUE.equals(dataExists)) {
			throw new IllegalStateException(
					"[LoadTestDataGenerator] 기존 데이터가 존재합니다. 다시 생성하려면 test-data.reset을 true로 설정하세요."
			);
		}
	}
	
	private void verifyResult(LoadTestDataProperties properties) {
		long actualPerformanceCount = jdbcTemplate.queryForObject(
				PERFORMANCE_COUNT_SQL,
				Long.class
		);
		
		long actualSessionCount = jdbcTemplate.queryForObject(
				SESSION_COUNT_SQL,
				Long.class
		);
		
		long expectedSessionCount = calculateExpectedSessionCount(properties);
		
		if (actualPerformanceCount != properties.performanceCount()) {
			throw new IllegalStateException(
					"공연 생성 건수가 일치하지 않습니다. expected=%d, actual=%d".formatted(properties.performanceCount(), actualPerformanceCount)
			);
		}
		
		if (actualSessionCount != expectedSessionCount) {
			throw new IllegalStateException(
					"회차 생성 건수가 일치하지 않습니다. expected=%d, actual=%d".formatted(expectedSessionCount, actualSessionCount)
			);
		}
	}

	private long calculateExpectedSessionCount(LoadTestDataProperties properties) {
		int sessionRagne = properties.maxSessionsPerPerformance() - properties.minSessionsPerPerformance() + 1;
		
		long fullCycleCount = properties.performanceCount() / sessionRagne;
		int remainingPerformanceCount = (int) (properties.performanceCount() % sessionRagne);
		
		long cycleSessionCount = ((long) properties.minSessionsPerPerformance() + properties.maxSessionsPerPerformance()) * sessionRagne / 2;
		
		long expectedSessionCount = fullCycleCount * cycleSessionCount;
		
		for (int offset = 0; offset < remainingPerformanceCount; offset++) {
			expectedSessionCount += properties.minSessionsPerPerformance() + offset;
		}
		
		return expectedSessionCount;
	}
}
