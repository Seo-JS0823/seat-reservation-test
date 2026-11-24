package seat_reservation.performance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import seat_reservation.performance.domain.model.SeatHold;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

}
