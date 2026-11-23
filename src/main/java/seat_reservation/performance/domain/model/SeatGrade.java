package seat_reservation.performance.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatGrade {
	VIP("VIP석"),
	
	ROYAL("R석"),
	
	SUPERIOR("S석"),
	
	A_GRADE("A석"),
	
	;
	private final String gradeDisplayName;
}
