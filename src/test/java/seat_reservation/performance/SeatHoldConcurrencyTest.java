package seat_reservation.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
	private SeatProperties seatProperties;
	
	@Autowired
	private Clock clock;
	
	@Autowired
	private ApplicationContext context;
	
	private final UUID userA = UUID.randomUUID();
	private final UUID holdA = UUID.randomUUID();
	
	private long seatId;
	
	@BeforeEach
	void setUp() {
		// 해당 테스트만 사용하는 좌석 할당
		seatId = testData.createAvailableSeat();
		
		/*
		 * 첫 번째 흐름
		 * 
		 * userA -> ttl + 1분 전에 점유 = 1분 전에 만료된 좌석
		 */
		LocalDateTime heldAt = TestClock.NOW
				.truncatedTo(ChronoUnit.SECONDS)
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
		assertThat(hold.expiresAt()).isEqualTo(TestClock.NOW.minusMinutes(1));
		assertThat(hold.endedAt()).isNull();
	}
	
	
}
