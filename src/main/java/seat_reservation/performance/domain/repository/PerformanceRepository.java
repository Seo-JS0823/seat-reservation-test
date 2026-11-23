package seat_reservation.performance.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import seat_reservation.performance.domain.model.Performance;
import seat_reservation.performance.domain.model.PerformanceSession;

public interface PerformanceRepository {

	Page<Performance> findByTitle(String title, Pageable pageable);
	
	Page<Performance> findAll(Pageable pageable);
	
	Optional<PerformanceSession> findBySession(Long sessionId);

	List<PerformanceSession> findSessionsByPerformanceId(Long performanceId);
}
