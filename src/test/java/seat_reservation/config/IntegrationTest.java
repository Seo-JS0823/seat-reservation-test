package seat_reservation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import seat_reservation.support.SeatTestData;

/*
 * 테스트 클래스마다 @SpringBootTest 속성과 @Import 속성을 따로 쓰면
 * 하나만 빼먹어도 잘못된 DB에 연결되거나 스케줄러가 켜진 채로 돌 수 있다.
 * 
 * 공통 애노테이션 설정으로 묶어서 실수를 방지한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = "seat.scheduling.enabled=false")
@Import({
	SeatReservationTestcontainersConfig.class,
	TestClock.class,
	SeatTestData.class,
})
public @interface IntegrationTest {

}
