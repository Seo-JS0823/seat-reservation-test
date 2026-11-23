package seat_reservation.performance.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "session_seats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SessionSeat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long seatId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private PerformanceSession session;
	
	@Column(name = "seat_number", nullable = false)
	private Integer seatNumber;
	
	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private SeatGrade grade;
	
	@Column(nullable = false)
	private Long price;
	
	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private SeatStatus status;
	
	// 현재 유효한 점유 식별자 없으면 비어 있음
	@Column
	private UUID currentHoldId;
	
	// 현재 확정된 예약 식별자 없으면 비어 있음
	@Column
	private UUID currentReservationId;
}
