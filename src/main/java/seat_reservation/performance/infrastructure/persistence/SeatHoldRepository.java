package seat_reservation.performance.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import seat_reservation.performance.domain.model.HoldStatus;
import seat_reservation.performance.domain.model.SeatHold;
import seat_reservation.performance.domain.model.SessionSeat;

public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {

	@Query(
			"""
			SELECT sh
			FROM SeatHold sh
			JOIN FETCH sh.seat
			WHERE sh.holdId = :holdId
				AND sh.userId = :userId
				AND sh.status = :status
			"""
	)
	Optional<SeatHold> findByIdAndUserIdAndStatus(
			@Param("holdId") UUID holdId,
			@Param("userId") UUID userId,
			@Param("status") HoldStatus status
	);

	Optional<SeatHold> findBySeatAndStatus(SessionSeat seat, HoldStatus active);

}
