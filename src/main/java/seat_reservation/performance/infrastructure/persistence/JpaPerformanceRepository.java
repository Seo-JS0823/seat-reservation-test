package seat_reservation.performance.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import seat_reservation.performance.domain.model.Performance;

public interface JpaPerformanceRepository extends JpaRepository<Performance, Long> {

	Page<Performance> findByTitleContaining(String title, Pageable pageable);

}
