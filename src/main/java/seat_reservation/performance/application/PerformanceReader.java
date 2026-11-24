package seat_reservation.performance.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.application.transfer.PerformanceInfo;
import seat_reservation.performance.application.transfer.PerformanceSessionInfo;
import seat_reservation.performance.domain.exception.PerformanceError;
import seat_reservation.performance.domain.exception.PerformanceException;
import seat_reservation.performance.domain.model.Performance;
import seat_reservation.performance.domain.model.PerformanceSession;
import seat_reservation.performance.domain.repository.PerformanceRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceReader {

	private final PerformanceRepository performanceRepo;
	
	public Page<PerformanceInfo> searchPerformance(String title, Pageable pageable) {
		Page<Performance> performances;
		
		if (StringUtils.hasText(title)) {
			performances = performanceRepo.findByTitle(title, pageable);
		} else {
			performances = performanceRepo.findAll(pageable);
		}
		
		return performances
				.map(p -> new PerformanceInfo(p.getPerformanceId(), p.getTitle()));
	}
	
	public PerformanceSessionInfo getPerformanceSession(Long sessionId) {
		PerformanceSession session = performanceRepo.findBySession(sessionId)
				.orElseThrow(() -> new PerformanceException(PerformanceError.PERFORMANCE_SESSION_NOT_FOUND));
		
		return new PerformanceSessionInfo(
				session.getSessionId(),
				session.getPerformance().getPerformanceId(),
				session.getStartAt()
		);
	}
	
	public List<PerformanceSessionInfo> getPerformanceSessions(Long performanceId) {
		List<PerformanceSession> sessions = performanceRepo.findSessionsByPerformanceId(performanceId);
		
		return sessions.stream()
				.map(p -> new PerformanceSessionInfo(p.getSessionId(), p.getPerformance().getPerformanceId(), p.getStartAt()))
				.toList();
	}

	public PerformanceInfo getPerformance(Long performanceId) {
		Performance performance =  performanceRepo.findById(performanceId)
				.orElseThrow(() -> new PerformanceException(PerformanceError.PERFORMANCE_SESSION_NOT_FOUND));
		
		return new PerformanceInfo(
				performance.getPerformanceId(),
				performance.getTitle()
		);
	}
}
