package seat_reservation.performance.infrastructure.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.domain.model.PerformanceSession;
import seat_reservation.performance.domain.model.SessionSeat;
import seat_reservation.performance.domain.repository.SeatRepository;
import seat_reservation.performance.infrastructure.persistence.JpaSessionSeatRepository;

@Repository
@RequiredArgsConstructor
public class SeatRepositoryAdapter implements SeatRepository {

	private final JpaSessionSeatRepository sessionSeatRepo;

	@Override
	public List<SessionSeat> findSeatsBySession(PerformanceSession session) {
		return sessionSeatRepo.findBySession(session);
	}

	@Override
	public SessionSeat save(SessionSeat seat) {
		return sessionSeatRepo.save(seat);
	}
	
	
}
