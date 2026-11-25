package seat_reservation.performance.presentation;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.application.service.PerformanceReader;
import seat_reservation.performance.application.transfer.PerformanceInfo;
import seat_reservation.performance.application.transfer.PerformanceSessionInfo;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performances")
public class PerformanceAPI {

	private final PerformanceReader performanceReader;
	
	// 공연 목록 조회
	@GetMapping
	public Page<PerformanceInfo> searchPerformance(
			@RequestParam(name = "title", required = false) String title,
			@PageableDefault(
					size = 10,
					sort = {"title", "performanceId"},
					direction = Direction.DESC
			) Pageable pageable
	) {
		return performanceReader.searchPerformance(title, pageable);
	}
	
	// 공연 상세 조회
	@GetMapping("/{performanceId}")
	public PerformanceInfo getPerformanceInfo(
			@PathVariable("performanceId") Long performanceId
	) {
		return performanceReader.getPerformance(performanceId); 
	}
	
	// 특정 공연의 회차 조회
	@GetMapping("/{performanceId}/sessions")
	public List<PerformanceSessionInfo> getPerformanceSessions(
			@PathVariable("performanceId") Long performanceId
	) {
		return performanceReader.getPerformanceSessions(performanceId);
	}
	
	// 회차 상세 조회
	@GetMapping("/sessions/{sessionId}")
	public PerformanceSessionInfo getDetailPerformanceSession(
			@PathVariable("sessionId") Long sessionId
	) {
		return performanceReader.getPerformanceSession(sessionId);
	}
}
