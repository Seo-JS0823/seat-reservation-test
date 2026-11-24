package seat_reservation.performance.infrastructure.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
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
	
}
