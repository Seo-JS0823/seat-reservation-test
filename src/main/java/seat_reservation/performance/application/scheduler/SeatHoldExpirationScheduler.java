package seat_reservation.performance.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import seat_reservation.performance.application.properties.SeatProperties;
import seat_reservation.performance.application.service.SessionSeatService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldExpirationScheduler {

	private final SessionSeatService seatService;
	
	private final SeatProperties seatProperties;
	
	public void expireScheduling() {
		LocalDateTime now = LocalDateTime.now();
		
		// 1. 만료 처리할 점유 정보 조회
		List<UUID> expiredHolds = seatService.findExpiredHoldsIds(now, seatProperties.expirationBatchSize());
		
		log.info("[ExpireScheduling] 스케줄러 동작 확인: expiredHoldsSize={}", expiredHolds.size());
		if (expiredHolds.isEmpty()) {
			
			return;
		}
		
		// 2. 한 트랜잭션에서 하나의 좌석을 만료 처리
		int expiredCount = 0;
		
		for (UUID holdId : expiredHolds) {
			try {
				if (seatService.expireSeatHold(holdId, now)) {
					expiredCount++;
				}
			}
			catch (OptimisticLockingFailureException e) {
				log.debug("좌석 점유 만료 중 동시성 충돌이 발생했습니다. holdId={}", holdId);
			}
			catch (RuntimeException e) {
				log.error("좌석 점유 만료 처리에 실패했습니다. holdId={}", holdId, e);
			}
		}
		
		if (expiredCount > 0) {
			log.info("만료된 좌석 점유를 정리했습니다. expiredCount={}", expiredCount);
		}	
	}
}
