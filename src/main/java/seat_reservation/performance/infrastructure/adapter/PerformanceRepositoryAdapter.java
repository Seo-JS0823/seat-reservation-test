package seat_reservation.performance.infrastructure.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.domain.exception.PerformanceError;
import seat_reservation.performance.domain.exception.PerformanceException;
import seat_reservation.performance.domain.model.Performance;
import seat_reservation.performance.domain.model.PerformanceSession;
import seat_reservation.performance.domain.repository.PerformanceRepository;
import seat_reservation.performance.infrastructure.persistence.JpaPerformanceRepository;
import seat_reservation.performance.infrastructure.persistence.JpaPerformanceSessionRepository;

@Repository
@RequiredArgsConstructor
public class PerformanceRepositoryAdapter implements PerformanceRepository {

	private final JpaPerformanceRepository performanceRepo;
	
	private final JpaPerformanceSessionRepository sessionRepo;

	@Override
	public Page<Performance> findByTitle(String title, Pageable pageable) {
		return performanceRepo.findByTitleContaining(title, pageable);
	}

	@Override
	public Page<Performance> findAll(Pageable pageable) {
		return performanceRepo.findAll(pageable);
	}

	@Override
	public Optional<PerformanceSession> findBySession(Long sessionId) {
		return sessionRepo.findById(sessionId);
	}

	@Override
	public List<PerformanceSession> findSessionsByPerformanceId(Long performanceId) {
		Performance performance = performanceRepo.findById(performanceId)
				.orElseThrow(() -> new PerformanceException(PerformanceError.PERFORMANCE_SESSION_NOT_FOUND));
		
		return sessionRepo.findByPerformance(performance);
	}
}
