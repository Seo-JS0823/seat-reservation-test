package seat_reservation.performance.infrastructure.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.domain.model.HoldStatus;
import seat_reservation.performance.domain.model.PerformanceSession;
import seat_reservation.performance.domain.model.SeatHold;
import seat_reservation.performance.domain.model.SessionSeat;
import seat_reservation.performance.domain.repository.SeatRepository;
import seat_reservation.performance.infrastructure.persistence.JpaSessionSeatRepository;
import seat_reservation.performance.infrastructure.persistence.SeatHoldRepository;

@Repository
@RequiredArgsConstructor
public class SeatRepositoryAdapter implements SeatRepository {

	private final JpaSessionSeatRepository sessionSeatRepo;
	
	private final SeatHoldRepository holdRepo;

	@Override
	public void flush() {
		holdRepo.flush();
	}
	
	@Override
	public List<SessionSeat> findSeatsBySession(PerformanceSession session) {
		return sessionSeatRepo.findBySession(session);
	}

	@Override
	public Optional<SessionSeat> getSessionSeat(Long seatId) {
		return sessionSeatRepo.findById(seatId);
	}

	@Override
	public SeatHold saveHold(SeatHold hold) {
		return holdRepo.save(hold);
	}

	@Override
	public Optional<SeatHold> findMeHoldingSeat(UUID holdId, UUID userId) {
		// 현재 점유중인 정보를 조회하므로 status 조건 추가
		return holdRepo.findByIdAndUserIdAndStatus(holdId, userId, HoldStatus.ACTIVE);
	}

	@Override
	public Optional<SeatHold> findHoldingSeat(SessionSeat seat) {
		return holdRepo.findBySeatAndStatus(seat, HoldStatus.ACTIVE);
	}
	
}
