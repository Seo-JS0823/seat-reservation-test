package seat_reservation.performance.domain.model;

public enum SeatStatus {

	// 다른 사용자가 점유할 수 있는 상태
	AVAILABLE,
	
	// 특정 사용자가 설정된 시간동안 임시 점유한 상태
	HELD,
	
	// 결제를 처리 중이거나 결제 결과를 아직 확정할 수 없어 다른 사용자에게 제공할 수 없는 상태
	PAYMENT_PENDING,
	
	// 결제가 성공하여 예약이 확정된 상태
	RESERVED
	
	;
	
	public boolean isAvailable() {
		return this == SeatStatus.AVAILABLE;
	}
	
	public boolean isHeld() {
		return this == SeatStatus.HELD;
	}
	
	public boolean isPaymentPending() {
		return this == SeatStatus.PAYMENT_PENDING;
	}
	
	public boolean isReserved() {
		return this == SeatStatus.RESERVED;
	}
}
