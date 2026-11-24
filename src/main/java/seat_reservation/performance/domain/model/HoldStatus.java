package seat_reservation.performance.domain.model;

public enum HoldStatus {

	// 점유시간이 남아 있고 결제를 시작할 수 있는 상태
	ACTIVE,
	
	// 연결된 결제 결과가 확정되기를 기다리는 상태
	PAYMENT_PENDING,
	
	// 결제 성공으로 예약 전환이 완료된 상태
	COMPLETED,
	
	// 점유시간이 지나 종료된 상태
	EXPIRED,
	
	// 사용자가 직접 점유를 취소한 상태
	RELEASED
}
