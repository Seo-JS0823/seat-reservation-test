package seat_reservation.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import seat_reservation.performance.domain.model.HoldStatus;
import seat_reservation.performance.domain.model.SeatStatus;
import seat_reservation.support.SeatTestData;
import seat_reservation.support.SeatTestData.HoldRow;
import seat_reservation.support.SeatTestData.SeatRow;

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
	
	private final UUID userA = UUID.randomUUID();
	private final UUID holdA = UUID.randomUUID();
	
	private final UUID userB = UUID.randomUUID();
	
	private long seatId;
	
	private LocalDateTime assertEndedAt;
	
	private LocalDateTime assertExpiresAt;
	
	@BeforeEach
	void setUp() {
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
}
