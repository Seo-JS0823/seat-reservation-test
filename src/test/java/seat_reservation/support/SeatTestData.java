package seat_reservation.support;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.RequiredArgsConstructor;
import seat_reservation.config.TestClock;

@TestComponent
@RequiredArgsConstructor
public class SeatTestData {

	private final JdbcTemplate jdbc;
	
	public record SeatRow(String status, UUID currentHoldId, long version) {}
	
	public record HoldRow(String status, LocalDateTime expiresAt, LocalDateTime endedAt) {}
	
	// 공연, 회차, 좌석 (AVAILABLE)을 만들고 좌석 ID를 돌려준다.
	public long createAvailableSeat() {
		long id = newId();
		
		jdbc.update(
				"""
				INSERT INTO seat_reservation.performances (performance_id, title)
				VALUES (?, ?)
				""", id, "동시성 테스트 공연 " + id
		);
		
		jdbc.update(
				"""
				INSERT INTO seat_reservation.performance_sessions (session_id, performance_id, start_at)
				VALUES (?, ?, ?)
				""", id, id, TestClock.NOW.plusDays(30)
		);
		
		jdbc.update(
				"""
				INSERT INTO seat_reservation.session_seats (seat_id, session_id, seat_number, grade, price, status)
				VALUES (?, ?, 1, 'VIP', 10000, 'AVAILABLE')
				""", id, id
		);
		
		return id;
	}
	
	// 좌석의 살아 있는 점유 수 조회
	public long countLiveHolds(long seatId) {
		return jdbc.queryForObject(
				"""
				SELECT count(*)
				FROM seat_reservation.seat_holds
				WHERE seat_id = ?
					AND status IN ('ACTIVE', 'PAYMENT_PENDING')
				""", Long.class, seatId
		);
	}
	
	public void deleteSeat(long seatId) {
		jdbc.update(
				"""
				DELETE FROM seat_reservation.seat_holds WHERE seat_id = ?
				""", seatId
		);
		
		jdbc.update(
				"""
				DELETE FROM seat_reservation.session_seats WHERE seat_id = ?
				""", seatId
		);
		
		jdbc.update(
				"""
				DELETE FROM seat_reservation.performance_sessions WHERE session_id = ?
				""", seatId
		);
		
		jdbc.update(
				"""
				DELETE FROM seat_reservation.performances WHERE performance_id = ?
				""", seatId
		);
	}
	
	public SeatRow findSeat(long seatId) {
		return jdbc.queryForObject(
				"""
				SELECT status, current_hold_id, version
				FROM seat_reservation.session_seats
				WHERE seat_id = ?
				""",
				(rs, rowNum) -> new SeatRow(
						rs.getString("status"),
						rs.getObject("current_hold_id", UUID.class),
						rs.getLong("version")
				),
				seatId
		);
	}
	
	public HoldRow findHold(UUID holdId) {
		return jdbc.queryForObject(
				"""
				SELECT status, expires_at, ended_at
				FROM seat_reservation.seat_holds
				WHERE hold_id = ?
				""",
				(rs, rowNum) -> new HoldRow(
						rs.getString("status"),
						rs.getObject("expires_at", LocalDateTime.class),
						rs.getObject("ended_at", LocalDateTime.class)
				),
				holdId
		);
	}
	
	private static long newId() {
		return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
	}
}
