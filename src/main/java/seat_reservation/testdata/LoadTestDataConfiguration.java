package seat_reservation.testdata;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("loadtest")
@EnableConfigurationProperties(LoadTestDataProperties.class)
public class LoadTestDataConfiguration {

}
