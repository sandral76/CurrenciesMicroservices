package currencyConversion;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "users-service")
public interface UserServiceProxy {

	@GetMapping("/users-service/current-user-role")
	String getCurrentUserRole(@RequestHeader("Authorization") String authorizationHeader);
	
	@GetMapping("/users-service/current-user-email")
	String getCurrentUserEmail(@RequestHeader("Authorization") String authorizationHeader);
}
