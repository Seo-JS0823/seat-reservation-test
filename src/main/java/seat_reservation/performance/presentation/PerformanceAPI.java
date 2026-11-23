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
import seat_reservation.performance.application.PerformanceInfo;
import seat_reservation.performance.application.PerformanceReader;
import seat_reservation.performance.application.PerformanceSessionInfo;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performance")
public class PerformanceAPI {

	private final PerformanceReader performanceReader;
	
	// 공연 목록 조회
	@GetMapping("/search")
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
	
	// 특정 공연의 회차 조회
	@GetMapping("/{id}")
	public List<PerformanceSessionInfo> getPerformanceSessions(
			@PathVariable("id") Long performanceId
	) {
		List<PerformanceSessionInfo> response = performanceReader.getPerformanceSessions(performanceId);
		
		return response;
	}
	
	// 회차 상세 조회
	@GetMapping("/{id}/sessions")
	public PerformanceSessionInfo getDetailPerformanceSession(
			@PathVariable("id") Long sessionId
	) {
		PerformanceSessionInfo response = performanceReader.getPerformanceSession(sessionId);
		
		return response;
	}
}
