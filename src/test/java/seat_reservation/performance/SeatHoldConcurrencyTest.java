package seat_reservation.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static seat_reservation.support.ConcurrentTaskSupport.failureOf;
import static seat_reservation.support.ConcurrentTaskSupport.label;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import seat_reservation.config.IntegrationTest;
import seat_reservation.config.TestClock;
import seat_reservation.performance.application.properties.SeatProperties;
import seat_reservation.performance.application.scheduler.SeatHoldSchedulingConfig;
import seat_reservation.performance.application.service.SessionSeatService;
import seat_reservation.performance.application.transfer.SessionSeatHold;
import seat_reservation.performance.application.usecase.SeatHoldUsecase;
import seat_reservation.performance.domain.exception.SeatError;
import seat_reservation.performance.domain.model.HoldStatus;
import seat_reservation.performance.domain.model.SeatStatus;
import seat_reservation.support.FailureEvidence;
import seat_reservation.support.PostgresError;
import seat_reservation.support.ResultCode;
import seat_reservation.support.SeatTestData;
import seat_reservation.support.SeatTestData.HoldRow;
import seat_reservation.support.SeatTestData.SeatRow;
import seat_reservation.support.ServerLog;

@IntegrationTest
public class SeatHoldConcurrencyTest {

	@Autowired
	private SeatTestData testData;
	
	@Autowired
	private SessionSeatService seatService;
	
	@Autowired
	private SeatHoldUsecase seatHoldUsecase;
	
	@Autowired
	private SeatProperties seatProperties;
	
	@Autowired
	private Clock clock;
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private ServerLog serverLog;
	
	private FailureEvidence evidence;
	
	private final UUID userA = UUID.randomUUID();
	private final UUID holdA = UUID.randomUUID();
	
	private final UUID userB = UUID.randomUUID();
	
	private long seatId;
	
	private LocalDateTime assertEndedAt;
	
	private LocalDateTime assertExpiresAt;
	
	@BeforeEach
	void setUp() {
		// 이 테스트에서 기록할 실패
		evidence = new FailureEvidence(
				serverLog,
				PostgresError.DEADLOCK
		);
		
		// 해당 테스트만 사용하는 좌석 할당
		seatId = testData.createAvailableSeat();
		
		assertEndedAt = TestClock.NOW.minusMinutes(1);
		assertExpiresAt = TestClock.NOW.plus(seatProperties.ttl());
		
		/*
		 * 첫 번째 흐름
		 * 
		 * userA -> ttl + 1분 전에 점유 = 1분 전에 만료된 좌석
		 */
		LocalDateTime heldAt = TestClock.NOW
				.minus(seatProperties.ttl())
				.minusMinutes(1);
		
		seatService.seatHold(seatId, userA, holdA, heldAt);
	}
	
	@AfterEach
	void tearDown() {
		testData.deleteSeat(seatId);
	}
	
	@Test
	@DisplayName("출발 상태: 스케줄러 꺼짐, A의 점유는 1분 전에 만료된 채 ACTIVE")
	void startingState() {
		// 진입점(usecase, scheduler)이 받을 고정 시계 검사
		assertThat(LocalDateTime.now(clock)).isEqualTo(TestClock.NOW);
		
		// 백그라운드 스케줄러 등록되지 않았는지 검사
		assertThat(context.getBeanNamesForType(SeatHoldSchedulingConfig.class)).isEmpty();
		
		// 좌석 검사
		SeatRow seat = testData.findSeat(seatId);
		assertThat(seat.status()).isEqualTo("HELD");
		assertThat(seat.currentHoldId()).isEqualTo(holdA);
		assertThat(seat.version()).isEqualTo(1L);
		
		// 점유 검사
		HoldRow hold = testData.findHold(holdA);
		assertThat(hold.status()).isEqualTo("ACTIVE");
		assertThat(hold.expiresAt()).isEqualTo(assertEndedAt);
		assertThat(hold.endedAt()).isNull();
	}
	
	@Test
	@DisplayName("[혼자] 재점유: A는 EXPIRED, B가 새로 점유하고 좌석은 B를 가리킨다")
	void rehold_alone() {
		SessionSeatHold result = seatHoldUsecase.seatHold(seatId, userB);
		UUID holdB = result.holdId();
		
		SeatRow seat = testData.findSeat(seatId);
		assertThat(seat.status()).isEqualTo(SeatStatus.HELD.name());
		assertThat(seat.currentHoldId()).isEqualTo(holdB);
		assertThat(seat.version()).isEqualTo(2L);
		
		HoldRow a = testData.findHold(holdA);
		assertThat(a.status()).isEqualTo(HoldStatus.EXPIRED.name());
		assertThat(a.endedAt()).isEqualTo(assertEndedAt);
		
		HoldRow b = testData.findHold(holdB);
		assertThat(b.status()).isEqualTo(HoldStatus.ACTIVE.name());
		assertThat(b.expiresAt()).isEqualTo(assertExpiresAt);
		assertThat(b.endedAt()).isNull();
		
		assertThat(testData.countLiveHolds(seatId)).isOne();
	}
	
	@Test
	@DisplayName("[혼자] 취소: 이미 만료된 점유라 RELEASED가 아니라 EXPIRED로 끝나고 좌석은 빈다")
	void cancel_alone() {
		seatHoldUsecase.cancelSeatHold(seatId, userA, holdA);
		
		SeatRow seat = testData.findSeat(seatId);
		assertThat(seat.status()).isEqualTo(SeatStatus.AVAILABLE.name());
		assertThat(seat.currentHoldId()).isNull();
		assertThat(seat.version()).isEqualTo(2L);
		
		HoldRow a = testData.findHold(holdA);
		assertThat(a.status()).isEqualTo(HoldStatus.EXPIRED.name());
		assertThat(a.endedAt()).isEqualTo(assertEndedAt);
		
		assertThat(testData.countLiveHolds(seatId)).isZero();
	}
	
	@Test
	@DisplayName("[혼자] 만료 처리: A는 EXPIRED, 좌석은 빈다")
	void expire_alone() {
		boolean expired = seatService.expireSeatHold(holdA, LocalDateTime.now(clock));
		
		assertThat(expired).isTrue();
		
		SeatRow seat = testData.findSeat(seatId);
		assertThat(seat.status()).isEqualTo(SeatStatus.AVAILABLE.name());
		assertThat(seat.currentHoldId()).isNull();
		assertThat(seat.version()).isEqualTo(2L);
		
		HoldRow a = testData.findHold(holdA);
		assertThat(a.status()).isEqualTo(HoldStatus.EXPIRED.name());
		assertThat(a.endedAt()).isEqualTo(assertEndedAt);
		
		assertThat(testData.countLiveHolds(seatId)).isZero();
	}
	
	@RepeatedTest(30)
	@DisplayName("[동시성 테스트] 재점유 vs 취소: 누가 이기든 정합성은 지켜지고, 진 쪽은 SeatException을 받는다")
	void rehold_vs_cancel(RepetitionInfo repetition) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		FailureEvidence.Window window = evidence.start();
		
		Throwable reholdError;
		Throwable cancelError;
		UUID holdB = null;
		
		try {
			// 1. 두 요청을 출발 선에 세운다.
			Future<SessionSeatHold> rehold = pool.submit(() -> {
				start.await();
				return seatHoldUsecase.seatHold(seatId, userB);
			});
			
			Future<?> cancel = pool.submit(() -> {
				start.await();
				seatHoldUsecase.cancelSeatHold(seatId, userA, holdA);
				return null;
			});
			
			// 2. 동시에 출발시키고 둘 다 끝날 때까지 기다린다.
			start.countDown();
			
			reholdError = failureOf(rehold);
			cancelError = failureOf(cancel);
			
			if (reholdError == null) {
				holdB = rehold.get().holdId();
			}
		}
		
		finally {
			pool.shutdown();
		}
		
		String success = ResultCode.SUCCESS.code();
		String reholdResult = label(reholdError);
		String cancelResult = label(cancelError);
		SeatRow seat = testData.findSeat(seatId);
		
		System.out.printf(
				"[%2d회] 재점유=%s, 취소=%s, version=%d%n",
				repetition.getCurrentRepetition(),
				reholdResult,
				cancelResult,
				seat.version()
		);
		
		// 기록 대상 실패만 파일로 남긴다.
		String name = "rehold-vs-cancel-%02d".formatted(repetition.getCurrentRepetition());
		
		window.save(name + "-rehold", reholdError)
				.ifPresent(file -> System.out.println("중요한 실패 기록 저장: " + file.toAbsolutePath()));
		
		window.save(name + "-cancel", cancelError)
				.ifPresent(file -> System.out.println("중요한 실패 기록 저장: " + file.toAbsolutePath()));
		
		// 3. 둘 다 실패할 수 없다.
		assertThat(List.of(reholdResult, cancelResult)).contains(success);
		
		// 4. 데이터 정합성은 둘 중 누가 경합에서 이기던 항상 성립해야 한다.
		HoldRow a = testData.findHold(holdA);
		assertThat(a.status()).isEqualTo(HoldStatus.EXPIRED.name());
		assertThat(a.endedAt()).isEqualTo(assertEndedAt);
		
		int successCount = (reholdError == null ? 1 : 0) + (cancelError == null ? 1 : 0);
		assertThat(seat.version()).isEqualTo(1 + successCount);
		
		if (reholdError == null) {
			assertThat(seat.status()).isEqualTo(SeatStatus.HELD.name());
			assertThat(seat.currentHoldId()).isEqualTo(holdB);
			
			HoldRow b = testData.findHold(holdB);
			assertThat(b.status()).isEqualTo(HoldStatus.ACTIVE.name());
			assertThat(b.expiresAt()).isEqualTo(assertExpiresAt);
			
			assertThat(testData.countLiveHolds(seatId)).isOne();
		}
		
		else {
			assertThat(seat.status()).isEqualTo(SeatStatus.AVAILABLE.name());
			assertThat(seat.currentHoldId()).isNull();
			
			assertThat(testData.countLiveHolds(seatId)).isZero();
		}
		
		// 5. 예외 검증
		assertThat(reholdResult).isIn(
				success,
				SeatError.SEAT_HOLD_NOT_STATUS.getCode()
		);
		assertThat(cancelResult).isIn(
				success,
				SeatError.SEAT_HOLDING_NOT_FOUND.getCode(),
				SeatError.SEAT_HOLD_INVALID_STATE.getCode()
		);
	}
}
