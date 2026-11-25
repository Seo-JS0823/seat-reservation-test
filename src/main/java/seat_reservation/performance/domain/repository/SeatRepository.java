package seat_reservation.performance.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import seat_reservation.performance.domain.model.PerformanceSession;
import seat_reservation.performance.domain.model.SeatHold;
import seat_reservation.performance.domain.model.SessionSeat;

public interface SeatRepository {
	
	void flush();
	
	// 특정 회차의 좌석 목록 조회
	List<SessionSeat> findSeatsBySession(PerformanceSession session);
	
	Optional<SessionSeat> getSessionSeat(Long seatId);
	
	SeatHold saveHold(SeatHold hold);
	
	// 자신의 현재 점유중인 점유 정보 조회
	Optional<SeatHold> findMeHoldingSeat(UUID holdId, UUID userId);
	
	// 좌석의 현재 점유중인 점유 정보 조회
	Optional<SeatHold> findHoldingSeat(SessionSeat seat);
}
