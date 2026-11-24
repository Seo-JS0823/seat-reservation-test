package seat_reservation.performance.presentation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.application.SessionSeatService;
import seat_reservation.performance.application.transfer.SessionSeatHold;
import seat_reservation.performance.application.transfer.SessionSeatInfo;
import seat_reservation.performance.presentation.request.SeatHoldRequests.SeatHoldRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/session-seats")
public class SessionSeatApi {

	private final SessionSeatService sessionSeatService;
	
	// 회차별 좌석 목록 조회
	@GetMapping("/{sessionId}")
	public List<SessionSeatInfo> getSessionSeatInfo(
			@PathVariable("sessionId") Long sessionId
	) {
		return sessionSeatService.getSessionSeatInfo(sessionId);
	}
	
	// 좌석 임시 점유
	@PostMapping("/{seatId}/holds")
	public SessionSeatHold seatHold(
			@PathVariable("seatId") Long seatId,
			@RequestBody SeatHoldRequest request
	) {
		return sessionSeatService.seatHold(seatId, request.userId());
	}
}
