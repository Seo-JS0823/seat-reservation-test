package seat_reservation.performance.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import seat_reservation.global.exception.CommonError;
import seat_reservation.performance.domain.exception.SeatError;
import seat_reservation.performance.domain.exception.SeatException;

@Entity
@Table(name = "seat_holds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SeatHold {
	
	@Id
	private UUID holdId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seat_id", nullable = false, updatable = false)
	private SessionSeat seat;
	
	@Column(nullable = false, updatable = false)
	private UUID userId;
	
	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private HoldStatus status;
	
	@Column(nullable = false, updatable = false)
	private LocalDateTime heldAt;
	
	@Column(nullable = false, updatable = false)
	private LocalDateTime expiresAt;
	
	@Column
	private LocalDateTime endedAt;
	
	/*
	 * note: SessionSeat.currentHoldId = SeatHold.holdId
	 * note: ttl -> SeatProperties.ttl()
	 */
	public static SeatHold hold(UUID holdId, SessionSeat seat, UUID userId, LocalDateTime now, Duration ttl) {
		SeatHold hold = new SeatHold();
		
		hold.validateTtl(ttl);
		
		hold.holdId = holdId;
		hold.seat = seat;
		hold.userId = userId;
		hold.heldAt = now;
		hold.expiresAt = now.plus(ttl);
		hold.status = HoldStatus.ACTIVE;
		
		return hold;
	}
	
	public boolean isExpired(LocalDateTime now) {
		return !now.isBefore(this.expiresAt);
	}
	
	// 좌석 점유 취소
	public void cancel(LocalDateTime now) {
		if (now == null) {
			throw new SeatException(CommonError.INTERNAL_SERVER_ERROR);
		}
		
		if (this.status != HoldStatus.ACTIVE) {
			throw new SeatException(SeatError.SEAT_HOLD_INVALID_STATE);
		}
		
		// 사용자 요청 시점과 서버 수신 시점 사이에 만료 시간이 지날 수 있으므로
		// 만료 시간이 지난 경우 EXPIRED
		// 만료 시간이 지나지 않은 경우 RELEASED
		if (isExpired(now)) {
			expire(now);
			return;
		}
		
		this.status = HoldStatus.RELEASED;
		this.endedAt = now;
	}
	
	public void expire(LocalDateTime now) {
		if (now == null) {
			throw new SeatException(CommonError.INTERNAL_SERVER_ERROR);
		}
		
		if (this.status != HoldStatus.ACTIVE || !isExpired(now)) {
			throw new SeatException(SeatError.SEAT_HOLD_INVALID_STATE);
		}
		
		this.status = HoldStatus.EXPIRED;
		this.endedAt = this.expiresAt;
	}
	
	private void validateTtl(Duration ttl) {
		if (ttl == null || ttl.isNegative() || ttl.isZero()) {
			throw new SeatException(CommonError.INTERNAL_SERVER_ERROR);
		}
	}
}
