package seat_reservation.performance.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "performance_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PerformanceSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sessionId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "performance_id", nullable = false)
	private Performance performance;
	
	@Column(nullable = false)
	private LocalDateTime startAt;
}
