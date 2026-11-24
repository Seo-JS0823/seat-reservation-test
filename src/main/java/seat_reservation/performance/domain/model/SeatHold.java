package seat_reservation.performance.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import seat_reservation.global.exception.CommonError;
import seat_reservation.performance.domain.exception.SeatException;

@Entity
@Table(name = "seat_holds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SeatHold {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
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
	
	public static SeatHold hold(SessionSeat seat, UUID userId, Duration ttl) {
		SeatHold hold = new SeatHold();
		
		hold.validateTtl(ttl);
		
		LocalDateTime now = LocalDateTime.now();
		
		hold.seat = seat;
		hold.userId = userId;
		hold.heldAt = now;
		hold.expiresAt = now.plus(ttl);
		hold.status = HoldStatus.ACTIVE;
		
		return hold;
	}
	
	private void validateTtl(Duration ttl) {
		if (ttl == null || ttl.isNegative() || ttl.isZero()) {
			throw new SeatException(CommonError.INTERNAL_SERVER_ERROR);
		}
	}
}
