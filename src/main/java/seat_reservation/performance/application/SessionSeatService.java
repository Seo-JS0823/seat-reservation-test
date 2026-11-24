package seat_reservation.performance.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.application.properties.SeatProperties;
import seat_reservation.performance.application.transfer.SessionSeatHold;
import seat_reservation.performance.application.transfer.SessionSeatInfo;
import seat_reservation.performance.domain.exception.PerformanceError;
import seat_reservation.performance.domain.exception.PerformanceException;
import seat_reservation.performance.domain.exception.SeatError;
import seat_reservation.performance.domain.exception.SeatException;
import seat_reservation.performance.domain.model.PerformanceSession;
import seat_reservation.performance.domain.model.SeatHold;
import seat_reservation.performance.domain.model.SessionSeat;
import seat_reservation.performance.domain.repository.PerformanceRepository;
import seat_reservation.performance.domain.repository.SeatRepository;

@Service
@RequiredArgsConstructor
public class SessionSeatService {

	private final SeatProperties seatProperties;
	
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
	
	@Transactional
	public SessionSeatHold seatHold(Long seatId, UUID userId) {
		// 1. 점유 가능한 좌석 조회
		SessionSeat seat = seatRepo.getSessionSeat(seatId)
				.orElseThrow(() -> new SeatException(SeatError.SESSION_SEAT_NOT_FOUND));
		
		// 3. 임시 점유키 발급
		UUID holdId = UUID.randomUUID();
		
		// 2. 점유한 좌석 상태 확인과 동시에 좌석 점유
		boolean usable = seat.availableOnlySeatHold(holdId);
		
		if (!usable) {
			throw new SeatException(SeatError.SEAT_HOLD_NOT_STATUS);
		}
		
		// 4. 좌석 점유 정보 저장
		SeatHold hold = SeatHold.hold(seat, userId, seatProperties.ttl());
		
		seatRepo.saveHold(hold);
		
		// . 임시 점유키와 함께 반환
		return new SessionSeatHold(
				seat.getSeatId(),
				seat.getSession().getSessionId(),
				seat.getSeatNumber(),
				seat.getGrade(),
				seat.getPrice(),
				seat.getStatus(),
				seat.getCurrentHoldId(),
				hold.getExpiresAt(),
				hold.getHoldId()
		);
	}
}
