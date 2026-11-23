package seat_reservation.performance.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import seat_reservation.performance.domain.model.Performance;
import seat_reservation.performance.domain.model.PerformanceSession;

public interface JpaPerformanceSessionRepository extends JpaRepository<PerformanceSession, Long> {

	List<PerformanceSession> findByPerformance(Performance performance);

}
