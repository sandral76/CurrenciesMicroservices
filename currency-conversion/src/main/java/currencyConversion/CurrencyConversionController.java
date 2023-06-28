package currencyConversion;

import java.math.BigDecimal;
import java.util.HashMap;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import feign.FeignException;
import feign.Response;

@RestController
public class CurrencyConversionController {

	@Autowired
	private CurrencyExchangeProxy proxy;

	@Autowired
	private UserServiceProxy proxyUser;

	@Autowired
	private BankAccountProxy proxyBankAccount;

	private WebClient webClient;

	public CurrencyConversionController(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.build();
	}
	/*
	 * @GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
	 * public CurrencyConversion getConversion (@PathVariable String
	 * from, @PathVariable String to, @PathVariable double quantity) {
	 * 
	 * HashMap<String,String> uriVariables = new HashMap<String,String>();
	 * uriVariables.put("from", from); uriVariables.put("to", to);
	 * 
	 * ResponseEntity<CurrencyConversion> response = new RestTemplate().
	 * getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
	 * CurrencyConversion.class, uriVariables);
	 * 
	 * CurrencyConversion cc = response.getBody();
	 * 
	 * return new CurrencyConversion(from,to,cc.getConversionMultiple(),
	 * cc.getEnvironment(), quantity,
	 * cc.getConversionMultiple().multiply(BigDecimal.valueOf(quantity))); }
	 */

	// localhost:8100/currency-conversion/from/EUR/to/RSD/quantity/100
	@GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
	public Mono<CurrencyConversion> getConversion(@PathVariable String from, @PathVariable String to,
			@PathVariable double quantity) {
		String url = "http://localhost:8000/currency-exchange/from/{from}/to/{to}";
		return webClient.get().uri(url, from, to).accept(MediaType.APPLICATION_JSON).retrieve()
				.bodyToMono(CurrencyConversion.class).flatMap(cc -> {
					BigDecimal totalAmount = cc.getConversionMultiple().multiply(BigDecimal.valueOf(quantity));
					return Mono.just(new CurrencyConversion(from, to, cc.getConversionMultiple(), cc.getEnvironment(),
							quantity, totalAmount));
				})
				.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Requested currency exchange could not be found!")))
				.onErrorResume(error -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"An error occurred while fetching the currency exchange", error)));
	}

	// localhost:8100/currency-conversion?from=EUR&to=RSD&quantity=100
	/*
	 * @GetMapping("/currency-conversion") public ResponseEntity<?>
	 * getConversionParams(@RequestParam String from, @RequestParam String
	 * to, @RequestParam double quantity) {
	 * 
	 * HashMap<String,String> uriVariable = new HashMap<String, String>();
	 * uriVariable.put("from", from); uriVariable.put("to", to);
	 * 
	 * try { ResponseEntity<CurrencyConversion> response = new RestTemplate().
	 * getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
	 * CurrencyConversion.class, uriVariable); CurrencyConversion responseBody =
	 * response.getBody(); return ResponseEntity.status(HttpStatus.OK).body(new
	 * CurrencyConversion(from,to,responseBody.getConversionMultiple(),responseBody.
	 * getEnvironment(), quantity,
	 * responseBody.getConversionMultiple().multiply(BigDecimal.valueOf(quantity))))
	 * ; } catch(HttpClientErrorException e) { return
	 * ResponseEntity.status(e.getStatusCode()).body(e.getMessage()); } }
	 */

	// localhost:8100/currency-conversion-feign?from=EUR&to=RSD&quantity=50
	@GetMapping("/currency-conversion")
	public ResponseEntity<?> getConversionFeign(@RequestParam String from, @RequestParam String to,
			@RequestParam double quantity, @RequestHeader("Authorization") String authorizationHeader) {
		try {
			String currentUser = proxyUser.getCurrentUserRole(authorizationHeader);
			if (currentUser.equals("USER")) {
				String currentUserEmail = proxyUser.getCurrentUserEmail(authorizationHeader);
				Double accountCurrencyAmount = proxyBankAccount.getUserCurrencyAmount(currentUserEmail, from);
				if (accountCurrencyAmount >= quantity) {
					ResponseEntity<CurrencyConversion> response = proxy.getExchange(from, to);
					CurrencyConversion responseBody = response.getBody();
					ResponseEntity<?> updatedAccountCurrencies = proxyBankAccount.updateAccountCurrency(currentUserEmail,from, to,quantity,responseBody.getConversionMultiple().multiply(BigDecimal.valueOf(quantity)).doubleValue());
					String message ="Conversion was successfull! "+quantity+from+" is exchanged for "+to;
					//return ResponseEntity.ok(updatedAccountCurrencies.getBody());
					return ResponseEntity.ok().body(new Object() {
					    public Object getBody() { return updatedAccountCurrencies.getBody(); }
					    public String getMessage() { return message; }
					});
				} else {
					String errorMessage = "User doesn't have enoguh amount on his bank account for exchanging.";
					return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
				}
			} else {
				String errorMessage = "User is not allow to perform exchanging since he is not 'USER'.";
				return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
			}

		} catch (FeignException e) {
			return ResponseEntity.status(e.status()).body(e.getMessage());
		}
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<String> handleMissingParams(MissingServletRequestParameterException ex) {
		String parameter = ex.getParameterName();
		// return ResponseEntity.status(ex.getStatusCode()).body(ex.getMessage());
		return ResponseEntity.status(ex.getStatusCode())
				.body("Value [" + ex.getParameterType() + "] of parameter [" + parameter + "] has been ommited");
	}

}
