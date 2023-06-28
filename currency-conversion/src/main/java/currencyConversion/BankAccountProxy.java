package currencyConversion;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "bank-account")
public interface BankAccountProxy {
	
	@GetMapping("/bank-account/{email}/{currencyFrom}")
	Double getUserCurrencyAmount(@PathVariable String email,@PathVariable String currencyFrom);
	
	@PutMapping("/bank-account/account")
	ResponseEntity<?> updateAccountCurrency(@RequestParam String email, @RequestParam String from,
			@RequestParam String to, @RequestParam double quantity, @RequestParam double totalAmount);
	
}
