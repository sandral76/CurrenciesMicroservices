package bankAccount;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "users-service")
public interface UserServiceProxy {

	@GetMapping("/users-service/user/{email}")
	public Boolean getUser(@PathVariable String email);
	
	@GetMapping("/users-service/user/role/{email}")
	public String getUsersRole(@PathVariable String email);
}
