<br/>

# CircuitBreaker (서킷브레이커)
CircuitBreaker 패턴은 오류가 많아지면 서버가 죽거나 과부하가 발생하는 것을 방지하는 역할을 한다. <br/>
즉, 서버가 완전히 죽지 않도록 장애가 난 서비스에 대한 요청을 조절하면서 전체 시스템을 보호하는 역할을 한다. <br/>
Resilience4j CircuitBreaker의 동작 방식은 설정한 실패율을 초과하면 열리고(Open), 일정 시간이 지나면 다시 닫힌다(Closed). <br/>
<br/><br/>

## 왜 CircuitBreaker가 필요한가?
✔ 연속적인 실패가 발생하면 서버가 정상 요청도 처리하지 못할 수 있음. <br/>
✔ 외부 API나 데이터베이스가 느려지거나 응답하지 않으면 서버가 무한 대기 상태가 될 수 있음. <br/>
✔ 장애가 발생한 서비스에 계속 요청을 보내면 전체 시스템이 영향을 받을 위험이 있음. <br/>
<br/><br/>

## CircuitBreaker의 주요 상태
- Closed (닫힘)
  - 정상적으로 요청을 처리하는 상태.
  - 실패율이 임계값 이하일 때 유지됨.
- Open (열림)
  - 설정한 실패율을 초과하면 CircuitBreaker가 Open 상태로 전환됨.
  - 이 상태에서는 모든 요청을 즉시 실패 처리하고, 실제 작업을 실행하지 않음.
    - **_더 이상 해당 서비스에 요청을 보내지 않음 → 시스템 과부하 방지_** 
    - 오류가 많아지는 걸 방지하고 서비스의 안정성을 유지하는 역할.
- Half-Open (반열림)
  - 일정 시간 (waitDurationInOpenState)이 지나면 일부 요청을 허용하여 서비스가 정상적으로 복구되었는지 테스트.
  - 성공적인 요청이 많으면 다시 Closed 상태로 전환, 실패율이 높으면 다시 Open 상태 유지.

#### [예시]
~~~
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)  // 실패율 50% 이상이면 Open 상태로 변경
    .waitDurationInOpenState(Duration.ofSeconds(10)) // Open 상태 유지 시간 (10초 후 Half-Open)
    .slidingWindowSize(5)  // 실패율 계산을 위한 윈도우 크기
    .build();
~~~
✔ 실패율 50% 초과 → CircuitBreaker Open → 모든 요청 차단 <br/>
✔ 10초 후 Half-Open → 일부 요청 허용하여 정상 작동 확인 <br/>
✔ 성공하면 Closed 상태로 복귀, 실패하면 다시 Open 상태 <br/>
<br/>
즉, CircuitBreaker가 Open 상태일 때 요청을 받지 않다가 일정 시간이 지나면 Half-Open으로 전환되고, <br/>
최종적으로 정상 응답이 많으면 다시 Closed 상태로 돌아간다. <br/>
<br/><br/>

## CircuitBreaker 구현 

#### [build.gradle]
~~~
dependencies {
	.....
	implementation 'org.springframework.boot:spring-boot-starter-aop'
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
	.....
~~~

#### [application.yml]
~~~
# actuator (Circuit Breaker 모니터링 추가.)
management:
  endpoints:
    web:
      exposure:
        include: health,info,circuitbreakers

# circuitbreaker (resilience4j)
resilience4j:
  circuitbreaker:
    instances:
      myServiceCircuitBreaker:
        registerHealthIndicator: true    # Health Indicator 등록.
        failureRateThreshold: 20         # 실패율이 20%일 경우 Circuit Breaker가 열린다.
        slowCallDurationThreshold: 1000  # 1000ms(1초) 이상 소요 시 실패로 간주.
        waitDurationInOpenState: 10000   # Circuit Breaker가 열린 상태에서 10초 후에 상태를 재검토.
        slidingWindowSize: 4             # Sliding window 크기 4보다 많은 요청에서 실패율을 계산하도록 한다.
        permittedNumberOfCallsInHalfOpenState: 1  # Half-open 상태에서 허용하는 최대 요청 수.
        minimumNumberOfCalls: 2          # 최소 호출 횟수 2번 이상. (이보다 적으면 실패율 계산 안 함.)
~~~

#### [AppConfig.java]
~~~
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(20000);
        factory.setReadTimeout(20000);
        return new RestTemplate(factory);
    }

}
~~~

#### [MyService.java]
~~~
@Service
@Slf4j
public class MyService {

    private final RestTemplate restTemplate;

    public MyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // @CircuitBreaker를 사용하여 실패 시 회로 차단
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
~~~

<br/><br/><br/><br/>
