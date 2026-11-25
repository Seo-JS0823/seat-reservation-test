package seat_reservation.performance.domain.model;

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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import seat_reservation.global.exception.BusinessException;
import seat_reservation.global.exception.CommonError;
import seat_reservation.performance.domain.exception.SeatError;
import seat_reservation.performance.domain.exception.SeatException;

@Entity
@Table(name = "session_seats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SessionSeat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long seatId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private PerformanceSession session;
	
	@Column(name = "seat_number", nullable = false)
	private Integer seatNumber;
	
	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private SeatGrade grade;
	
	@Column(nullable = false)
	private Long price;
	
	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private SeatStatus status;
	
	// 현재 유효한 점유 식별자 없으면 비어 있음
	@Column
	private UUID currentHoldId;
	
	// 현재 확정된 예약 식별자 없으면 비어 있음
	@Column
	private UUID currentReservationId;
	
	@Version
	private Long version;
	
	public boolean isAvailable() {
		return this.status.isAvailable();
	}
	
	public boolean isHeld() {
		return this.status.isHeld();
	}
	
	// AVAILABLE 상태에서의 점유 처리
	public boolean availableOnlySeatHold(UUID holdId) {
		if (holdId == null) {
			throw new BusinessException(CommonError.BAD_REQUEST);
		}
		
		if (isAvailable()) {
			this.status = SeatStatus.HELD;
			this.currentHoldId = holdId;
			
			return true;
		}
		
		return false;
	}
	
	// 만료된 기존 점유를 새로운 점유 식별자로 교체
	public void replaceExpiredHold(UUID expiredHoldId, UUID newHoldId) {
		if (expiredHoldId == null || newHoldId == null) {
			throw new BusinessException(CommonError.BAD_REQUEST);
		}
		
		if (!isHeld() || !expiredHoldId.equals(this.currentHoldId)) {
			throw new SeatException(SeatError.SEAT_HOLD_INVALID_STATE);
		}
		
		this.currentHoldId = newHoldId;
	}
	
	public void release(UUID holdId) {
		if (holdId == null || this.currentHoldId == null) {
			throw new SeatException(SeatError.SEAT_HOLD_INVALID_STATE);
		}
		
		if (!holdId.equals(this.currentHoldId) || this.status != SeatStatus.HELD) {
			throw new SeatException(SeatError.SEAT_HOLD_INVALID_STATE);
		}
		
		this.status = SeatStatus.AVAILABLE;
		this.currentHoldId = null;
	}
	
}
