package seat_reservation.performance.presentation.request;

import java.util.UUID;

public class SeatHoldRequests {

	public record SeatHoldRequest(UUID userId) {
		
	}
	
	public record SeatCancelRequest(UUID userId) {
		
	}
}
