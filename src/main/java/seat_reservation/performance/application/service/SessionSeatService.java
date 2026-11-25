package seat_reservation.performance.application.service;

import java.time.LocalDateTime;
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
import seat_reservation.performance.domain.model.SeatStatus;
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
	public SessionSeatHold seatHold(Long seatId, UUID userId, UUID holdId, LocalDateTime now) {
		// 1. 점유 가능한 좌석 조회
		SessionSeat seat = seatRepo.getSessionSeat(seatId)
				.orElseThrow(() -> new SeatException(SeatError.SESSION_SEAT_NOT_FOUND));
		
		// 2. 점유 가능한 좌석의 상태 확인 및 분기
		boolean usable = false;
		
		switch (seat.getStatus()) {
			case AVAILABLE -> usable = seat.availableOnlySeatHold(holdId);
			
			case HELD -> {
				SeatHold holding = seatRepo.findHoldingSeat(seat)
						.orElseThrow(() -> new SeatException(SeatError.SEAT_HOLD_NOT_STATUS));
				
				if (holding.isExpired(now)) {
					holding.expire(now);
					
					seatRepo.flush();
					
					seat.replaceExpiredHold(holding.getHoldId(), holdId);
					
					usable = true;
				}
			}
			
			default -> throw new SeatException(SeatError.SEAT_HOLD_NOT_STATUS);
		}
		
		if (!usable)
			throw new SeatException(SeatError.SEAT_HOLD_NOT_STATUS);
		
		// 3. 좌석 점유 정보 저장
		SeatHold hold = SeatHold.hold(holdId, seat, userId, now, seatProperties.ttl());
		
		seatRepo.saveHold(hold);
		
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

	@Transactional
	public void seatCancel(Long seatId, UUID userId, UUID holdId, LocalDateTime now) {
		// 1. 좌석 점유 정보 조회 + SessionSeat Join
		SeatHold currentHold = seatRepo.findMeHoldingSeat(holdId, userId)
				.orElseThrow(() -> new SeatException(SeatError.SEAT_HOLDING_NOT_FOUND));
		
		// 2. 조회한 점유 정보가 요청 경로의 좌석 및 현재 점유 정보와 일치하는지 확인
		SessionSeat seat = currentHold.getSeat();
		
		boolean usable =
				
				// 2-1. holdId가 좌석의 currentHoldId와 같은가?
				currentHold.getHoldId().equals(seat.getCurrentHoldId())
				
				// 2-2. 요청으로 들어온 좌석 식별자가 점유 정보와 연결된 좌석의 seatId와 같은가?
				&& seatId.equals(seat.getSeatId())
				
				// 2-3. 좌석의 상태가 HELD 인가?
				&& seat.getStatus() == SeatStatus.HELD;
		
		if (!usable) {
			throw new SeatException(SeatError.SEAT_HOLD_INVALID_STATE);
		}
		
		// 3. 좌석 점유를 취소
		currentHold.cancel(now);
		seat.release(holdId);
	}
}
