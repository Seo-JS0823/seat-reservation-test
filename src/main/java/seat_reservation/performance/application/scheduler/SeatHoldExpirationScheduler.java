package seat_reservation.performance.application.scheduler;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import seat_reservation.performance.application.logger.LoggerMessages;
import seat_reservation.performance.application.properties.SeatProperties;
import seat_reservation.performance.application.service.SessionSeatService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldExpirationScheduler {

	private final SessionSeatService seatService;
	
	private final SeatProperties seatProperties;
	
	private final Clock clock;
	
	public void expireScheduling() {
		LocalDateTime now = LocalDateTime.now(clock);
		
		// 1. 만료 처리할 점유 정보 조회
		List<UUID> expiredHolds = seatService.findExpiredHoldsIds(now, seatProperties.expirationBatchSize());
		
		if (!expiredHolds.isEmpty()) {
			log.info(
					"[ExpireScheduling]: expiredHoldsSize={}",
					expiredHolds.size()
			);
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
				log.debug(
						"[ExpireScheduling]: type={} holdId={}",
						LoggerMessages.OPTIMISTIC_LOCK,
						holdId
				);
			}
			catch (RuntimeException e) {
				log.error(
						"[ExpireScheduling]: type={} holdId={}",
						LoggerMessages.UNIQUE,
						holdId,
						e
				);
			}
		}
		
		if (expiredCount > 0) {
			log.info(
					"[ExpireScheduling]: 만료된 좌석 점유를 정리했습니다. expiredCount={}",
					expiredCount
			);
		}	
	}
}
