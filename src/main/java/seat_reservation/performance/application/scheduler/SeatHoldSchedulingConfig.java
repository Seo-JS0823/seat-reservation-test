package seat_reservation.performance.application.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.application.properties.SeatProperties;

@Configuration(proxyBeanMethods = false)

/*
 * 이 속성 값에 따라 이 설정 클래스를 빈으로 만들지 말지를 정한다.
 * 만들지 않으면 스케줄러 작업이 등록되지 않는다.
 * 
 * havingValue = true --> 값이 true일 때만 등록한다.
 * matchIfMissing = true --> 속성이 아예 없으면 등록한다.
 */
@ConditionalOnProperty(
		name = "seat.scheduling.enabled",
		havingValue = "true",
		matchIfMissing = true
)
@RequiredArgsConstructor
public class SeatHoldSchedulingConfig implements SchedulingConfigurer {

	private final SeatHoldExpirationScheduler seatHoldExpirationScheduler;
	
	private final SeatProperties seatProperties;
	
	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addFixedDelayTask(
				seatHoldExpirationScheduler::expireScheduling,
				seatProperties.expirationInterval()
		);
	}

}
