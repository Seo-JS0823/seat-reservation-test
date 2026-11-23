package seat_reservation.performance.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import seat_reservation.performance.domain.model.PerformanceSession;
import seat_reservation.performance.domain.model.SessionSeat;

public interface JpaSessionSeatRepository extends JpaRepository<SessionSeat, Long> {

	List<SessionSeat> findBySession(PerformanceSession session);

}
