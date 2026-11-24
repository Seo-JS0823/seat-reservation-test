package seat_reservation.performance.application.usecase;

import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.application.service.SessionSeatService;
import seat_reservation.performance.application.transfer.SessionSeatHold;
import seat_reservation.performance.domain.exception.SeatError;
import seat_reservation.performance.domain.exception.SeatException;

@Service
@RequiredArgsConstructor
public class SeatHoldUsecase {

	private final SessionSeatService seatService;
	
	public SessionSeatHold seatHold(Long seatId, UUID userId) {
		UUID holdId = UUID.randomUUID();
		
		try {
			return seatService.seatHold(seatId, userId, holdId);
		}
		// 낙관적 락 충돌 -> SeatException 변환
		catch (OptimisticLockingFailureException e) {
			throw new SeatException(SeatError.SEAT_HOLD_NOT_STATUS);
		}
	}
}
