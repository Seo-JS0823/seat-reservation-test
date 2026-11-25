package seat_reservation.performance.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import seat_reservation.performance.domain.model.PerformanceSession;
import seat_reservation.performance.domain.model.SeatHold;
import seat_reservation.performance.domain.model.SessionSeat;

public interface SeatRepository {
	
	// 특정 회차의 좌석 목록 조회
	List<SessionSeat> findSeatsBySession(PerformanceSession session);
	
	Optional<SessionSeat> getSessionSeat(Long seatId);
	
	SeatHold saveHold(SeatHold hold);
	
	Optional<SeatHold> findHoldingSeatByHoldId(UUID holdId, UUID userId);
}
