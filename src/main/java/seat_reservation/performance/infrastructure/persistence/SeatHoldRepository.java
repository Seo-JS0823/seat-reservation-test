package seat_reservation.performance.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
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

	@Query(
			"""
			SELECT sh.id
			FROM SeatHold sh
			WHERE sh.status = :status
				AND sh.endedAt IS NULL
				AND sh.expiresAt <= :now
			ORDER BY sh.expiresAt, sh.holdId
			"""
	)
	List<UUID> findExpiredHoldsIds(
			@Param("status") HoldStatus status,
			@Param("now") LocalDateTime now,
			Limit limit
	);

	@Query(
			"""
			SELECT sh
			FROM SeatHold sh
			JOIN FETCH sh.seat
			WHERE sh.holdId = :holdId
				AND sh.status = :status
			"""
	)
	Optional<SeatHold> findByIdAndStatusWithSeat(
			@Param("holdId") UUID holdId,
			@Param("status") HoldStatus status
	);

}
