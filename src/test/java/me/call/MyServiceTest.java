package me.call;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
@Slf4j
public class MyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Autowired
    private MyService myService;

    private CircuitBreaker circuitBreaker;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    public void setup() {
        // Circuit Breaker 초기화.
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("myServiceCircuitBreaker");
    }

    @Test
    public void testCircuitBreakerStateAfterFailure() {
        // 여러 번 호출하여 실패 발생시킴.
        for (int i = 0; i < 10; i++) {
            try {
                String response = myService.callExternalService();
                log.debug("# response: " + response);
            } catch (Exception e) {
                // 예외를 무시하고 계속 실패.
            }
        }

        // 상태가 OPEN으로 바뀌었는지 확인. (Open 상태로 변경되려면 몇 초 정도 시간이 걸릴 수 있음.)
        log.debug("-------------------------------------------------");
        log.debug("# getFailureRateThreshold: " + circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold());
        log.debug("# getSlowCallDurationThreshold: " + circuitBreaker.getCircuitBreakerConfig().getSlowCallDurationThreshold());
        log.debug("# getSlidingWindowSize: " + circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize());
        log.debug("# getMinimumNumberOfCalls: " + circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls());
        log.debug("-------------------------------------------------");
        log.debug("# getState: " + circuitBreaker.getState());
        log.debug("# getFailureRate: " + circuitBreaker.getMetrics().getFailureRate());
        log.debug("# getSlowCallRate: " + circuitBreaker.getMetrics().getSlowCallRate());
        log.debug("# getNumberOfNotPermittedCalls: " + circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()); // 허용 안된 호출 수.
        log.debug("# getNumberOfSlowCalls: " + circuitBreaker.getMetrics().getNumberOfSlowCalls());
        log.debug("# getNumberOfFailedCalls: " + circuitBreaker.getMetrics().getNumberOfFailedCalls());
        log.debug("# getNumberOfSuccessfulCalls: " + circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        log.debug("-------------------------------------------------");

        // Assertion 체크.
        assertTrue(circuitBreaker.getState() == CircuitBreaker.State.OPEN);
    }

    @Disabled("Mock 객체 사용 시 사용.")
    @Test
    @DisplayName("성공 테스트 (CLOSED)")
    public void testCallExternalService_success() {
        // 성공적인 외부 API 호출.
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("Success");

        String response = myService.callExternalService();

        // 응답이 정상적으로 돌아오는지 확인.
        assertEquals("Success", response);
        assertTrue(circuitBreaker.getState() == CircuitBreaker.State.CLOSED);  // CircuitBreaker 상태가 CLOSED이어야 함.
    }

}