package me.call;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class MyService {

    private final RestTemplate restTemplate;

    public MyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // @CircuitBreaker를 사용하여 실패 시 회로 차단.
    @CircuitBreaker(name = "myServiceCircuitBreaker", fallbackMethod = "fallback")
    public String callExternalService() {
        // throw new RuntimeException("External service failed."); // 실패 (OPEN)

        // 외부 API 호출 예시
        String url = "https://some-external-api.com/data"; // 실패 (OPEN)
        // String url = "https://jsonplaceholder.typicode.com/posts"; // 성공 (CLOSED)
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 실패한 호출 수 만큼 반환됨.
     */
    private String fallback(Exception e) {
        return "Fallback response: " + e.getMessage();
    }

    /**
     * 실패 이후 허용되지 않은 호출 수 만큼 반환됨.
     */
    private String fallback(CallNotPermittedException e) {
        return "CircuitBreaker is OPEN: " + e.getMessage();
    }

}
