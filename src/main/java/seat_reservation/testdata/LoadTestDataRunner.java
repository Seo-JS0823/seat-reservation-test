package seat_reservation.testdata;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class LoadTestDataRunner implements ApplicationRunner {

	private final LoadTestDataProperties properties;
	
	private final LoadTestDataGenerator generator;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (!properties.enabled()) {
			log.info(
					"부하 테스트 데이터 생성기가 비활성화되어 있습니다. 실행하려면 test-data.enabled를 true로 설정하세요."
			);
			return;
		}
		
		long startedAt = System.currentTimeMillis();
		
		generator.generate(properties);
		
		long elapsedMillis = System.currentTimeMillis() - startedAt;
		
		log.info(
				"부하 테스트 데이터 생성 작업이 종료되었습니다. elapsed={}ms",
				elapsedMillis
		);
	}
}
