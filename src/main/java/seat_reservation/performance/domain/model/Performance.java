package seat_reservation.performance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "performances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Performance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long performanceId;
	
	@Column(nullable = false)
	private String title;
}
