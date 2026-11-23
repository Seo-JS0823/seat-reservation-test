package seat_reservation.performance.presentation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.application.SessionSeatService;
import seat_reservation.performance.application.transfer.SessionSeatInfo;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seats")
public class SessionSeatApi {

	private final SessionSeatService sessionSeatService;
	
	@GetMapping("/{id}")
	public List<SessionSeatInfo> getSessionSeatInfo(
			@PathVariable("id") Long sessionId
	) {
		return sessionSeatService.getSessionSeatInfo(sessionId);
	}
}
