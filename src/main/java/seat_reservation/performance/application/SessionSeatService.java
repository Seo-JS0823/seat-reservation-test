package seat_reservation.performance.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.application.transfer.SessionSeatInfo;
import seat_reservation.performance.domain.exception.PerformanceError;
import seat_reservation.performance.domain.exception.PerformanceException;
import seat_reservation.performance.domain.model.PerformanceSession;
import seat_reservation.performance.domain.model.SessionSeat;
import seat_reservation.performance.domain.repository.PerformanceRepository;
import seat_reservation.performance.domain.repository.SeatRepository;

@Service
@RequiredArgsConstructor
public class SessionSeatService {

	private final PerformanceRepository performanceRepo;
	
	private final SeatRepository seatRepo;
	
	@Transactional(readOnly = true)
	public List<SessionSeatInfo> getSessionSeatInfo(Long sessionId) {
		PerformanceSession session = performanceRepo.findBySession(sessionId)
				.orElseThrow(() -> new PerformanceException(PerformanceError.PERFORMANCE_SESSION_NOT_FOUND));
		
		List<SessionSeat> seats = seatRepo.findSeatsBySession(session);
		
		return seats.stream()
				.map(SessionSeatInfo::of)
				.toList();
	}
}
