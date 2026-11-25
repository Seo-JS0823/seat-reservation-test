package seat_reservation.performance.application.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import lombok.RequiredArgsConstructor;
import seat_reservation.performance.application.properties.SeatProperties;

@Configuration(proxyBeanMethods = false)
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
