package currencyExchange;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@RestController
public class CurrencyExchangeController {

	@Autowired
	private CurrencyExchangeRepository repo;

	@Autowired
	private Environment environment;

	private WebClient webClient;
	
	/*public CurrencyExchangeController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }*/

	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	@RateLimiter(name = "default")
	public ResponseEntity<?> getExchange(@PathVariable String from, @PathVariable String to) { // return
		//new CurrencyExchange(10000, from, to, BigDecimal.valueOf(117), "");
		String port = environment.getProperty("local.server.port");
		CurrencyExchange kurs = repo.findByFromAndToIgnoreCase(from, to);

		if (kurs != null) {
			kurs.setEnvironment(port);
			return ResponseEntity.ok(kurs);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Requested currency exchange could not be found!");
		}

	}

	
	/*@GetMapping("/currency-exchange/from/{from}/to/{to}")
	@RateLimiter(name = "default")
	public Mono<Object> getExchange(@PathVariable String from, @PathVariable String to) {
	    String port = environment.getProperty("local.server.port");
	    CurrencyExchange kurs = repo.findByFromAndToIgnoreCase(from, to);

	    if (kurs != null) {
	        kurs.setEnvironment(port);
	        return Mono.just(kurs.convertToJson());
	    } else {
	        String url = "http://localhost:8000/currency-exchange/from/" + from + "/to/" + to;
	        return webClient.get()
	                .uri(url)
	                .accept(MediaType.APPLICATION_JSON)
	                .retrieve()
	                .bodyToMono(String.class)
	                .flatMap(responseBody -> {
	                    if (responseBody != null) {
	                        return Mono.just((Object) responseBody);
	                    } else {
	                        return Mono.just("Requested currency exchange could not be found!");
	                    }
	                })
	                .onErrorReturn("An error occurred while fetching the currency exchange");
	    }
	}*/

	@ExceptionHandler(RequestNotPermitted.class)
	public ResponseEntity<String> rateLimiterExceptionHandler(RequestNotPermitted ex) {
		return ResponseEntity.status(503)
				.body("Currency exchange service can only serve up to 2 requests every 30 seconds");
	}

	// localhost:8000/currency-exchange/from/EUR/to/RSD
}
