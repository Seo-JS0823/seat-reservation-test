package seat_reservation.performance.application.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

	private static final String LIVE_HOLD_INDEX = "uk_seat_holds_live_seat";
	
	private final SessionSeatService seatService;
	
	public SessionSeatHold seatHold(Long seatId, UUID userId) {
		UUID holdId = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();
		
		try {
			return seatService.seatHold(seatId, userId, holdId, now);
		}
		// 낙관적 락 충돌 -> SeatException 변환
		catch (OptimisticLockingFailureException e) {
			throw new SeatException(SeatError.SEAT_HOLD_NOT_STATUS, e);
		}
		// 부분 유니크 제약 조건 충돌 -> SeatException 변환
		catch (DataIntegrityViolationException e) {
			if (isLiveHoldConflict(e)) {
				throw new SeatException(SeatError.SEAT_HOLD_NOT_STATUS, e);
			}
			
			throw e;
		}
	}

	public void cancelSeatHold(Long seatId, UUID userId, UUID holdId) {
		LocalDateTime now = LocalDateTime.now();
		
		try {
			seatService.seatCancel(seatId, userId, holdId, now);
		} catch (OptimisticLockingFailureException e) {
			throw new SeatException(SeatError.SEAT_HOLD_INVALID_STATE);
		}
	}
	
	private boolean isLiveHoldConflict(DataIntegrityViolationException e) {
		Throwable cause = e;
		
		while (cause != null) {
			// 현재 단계의 예외 확인
			if (cause instanceof ConstraintViolationException cve) {
				return LIVE_HOLD_INDEX.equals(cve.getConstraintName());
			}
			
			// 다음 단계의 예외로 이동
			cause = cause.getCause();
		}
		
		return false;
	}
}
