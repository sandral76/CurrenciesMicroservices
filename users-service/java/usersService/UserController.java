package usersService;

import java.util.List;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ServerWebExchange;

import jakarta.servlet.http.HttpServletRequest;
import usersService.model.CustomUser;

@RestController

public class UserController {

	@Autowired
	private CustomUserRepository repo;

	@Autowired
	private BankAccountProxy proxy;

	@GetMapping("/users-service/users")
	public List<CustomUser> getAllUsers() {
		return repo.findAll();
	}

	/*
	 * @PostMapping("/users-service/users") public ResponseEntity<CustomUser>
	 * createUser(@RequestBody CustomUser user) { CustomUser createdUser =
	 * repo.save(user); return ResponseEntity.status(201).body(createdUser); }
	 */

	@PostMapping("/users-service/users")
	public ResponseEntity<?> createUser(@RequestBody CustomUser user,
			@RequestHeader("Authorization") String authorizationHeader) {
		String role = extractRoleFromAuthorizationHeader(authorizationHeader);
		if ("ADMIN".equals(role)) {
			if (user.getRole().equals("USER")) {
				if (repo.existsById(user.getId())) {
					String errorMessage = "User with ID " + user.getId() + " already exists.";
					return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
				} else {
					CustomUser createdUser = repo.save(user);
					return new ResponseEntity<CustomUser>(createdUser, HttpStatus.CREATED);
				}
			} else {
				String errorMessage = "Admin can't create user with role different from 'USER'";
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
			}

		} else if ("OWNER".equals(role)) {
			if (user.getRole().equals("USER") || user.getRole().equals("ADMIN")) {
				CustomUser createdUser = repo.save(user);
				return new ResponseEntity<CustomUser>(createdUser, HttpStatus.CREATED);
			} else {
				String errorMessage = "Admin can't create user with role different from 'USER'or 'ADMIN";
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
			}
		} else {
			if (user.getRole().equals("OWNER")) {
				if (repo.existsByRole("OWNER")) {
					return ResponseEntity.status(HttpStatus.CONFLICT).body("An user with role 'OWNER' already exists.");
				} else {
					if (repo.existsById(user.getId())) {
						String errorMessage = "User with ID " + user.getId() + " already exists.";
						return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
					} else {
						CustomUser createdUser = repo.save(user);
						return new ResponseEntity<CustomUser>(createdUser, HttpStatus.CREATED);
					}
				}
			} else {
				if (repo.existsById(user.getId())) {
					String errorMessage = "User with ID " + user.getId() + " already exists.";
					return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
				} else {
					CustomUser createdUser = repo.save(user);
					return new ResponseEntity<CustomUser>(createdUser, HttpStatus.CREATED);
				}
			}
		}
	}

	/*
	 * private boolean hasAdminRole(Principal principal) { if (principal instanceof
	 * Authentication) { Authentication authentication = (Authentication) principal;
	 * return authentication.getAuthorities().stream() .anyMatch(a ->
	 * a.getAuthority().equals("ADMIN")); } return false; }
	 */

	@PutMapping("/users-service/users/{id}")
	public ResponseEntity<?> updateUser(@PathVariable long id, @RequestBody CustomUser user,
			@RequestHeader("Authorization") String authorizationHeader) {
		String role = extractRoleFromAuthorizationHeader(authorizationHeader);
		if ("ADMIN".equals(role)) {
			if (user.getRole().equals("USER")) {
				if (repo.existsById(user.getId())) {
					repo.save(user);
					String errorMessage = "User with ID " + user.getId() + " updated.";
					return ResponseEntity.status(HttpStatus.OK).body(errorMessage);
				} else {
					String errorMessage = "User with ID " + user.getId() + " doesn't exists.";
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
				}
			} else {
				String errorMessage = "Admin can't update user with role different from 'USER'";
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
			}

		} else if ("OWNER".equals(role)) {
			if (repo.existsById(user.getId())) {
				repo.save(user);
				String errorMessage = "User with ID " + user.getId() + " updated.";
				return ResponseEntity.status(HttpStatus.OK).body(errorMessage);
			} else {
				String errorMessage = "User with ID " + user.getId() + " doesn't exists.";
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
			}
		} else {
			if (repo.existsById(user.getId())) {
				repo.save(user);
				String errorMessage = "User with ID " + user.getId() + " updated.";
				return ResponseEntity.status(HttpStatus.OK).body(errorMessage);
			} else {
				String errorMessage = "User with ID " + user.getId() + " doesn't exists.";
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
			}
		}
	}

	/*
	 * @DeleteMapping("/users-service/users/{id}") public ResponseEntity<?>
	 * deleteUser(@PathVariable long id) { if (repo.existsById(id)) {
	 * repo.deleteById(id); String errorMessage = "User with ID " + id +
	 * " deleted."; return ResponseEntity.status(HttpStatus.OK).body(errorMessage);
	 * } else { String errorMessage = "User with ID " + id + " doesn't exists.";
	 * return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
	 * 
	 * }
	 * 
	 * }
	 */
	/*
	 * @DeleteMapping("/users-service/users/{id}") public ResponseEntity<?>
	 * deleteUser(@PathVariable long id, @RequestHeader("Role") String role) { if
	 * ("OWNER".equals(role)) { // Brisanje korisnika if (repo.existsById(id)) {
	 * repo.deleteById(id); String errorMessage = "User with ID " + id +
	 * " deleted."; return ResponseEntity.status(HttpStatus.OK).body(errorMessage);
	 * } else { String errorMessage = "User with ID " + id + " doesn't exist.";
	 * return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage); } }
	 * else { String errorMessage = "Only the owner can delete a user."; return
	 * ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage); } }
	 */
	@DeleteMapping("users-service/users/{id}")
	public ResponseEntity<?> deleteUser(@PathVariable long id,
			@RequestHeader("Authorization") String authorizationHeader) {
		String role = extractRoleFromAuthorizationHeader(authorizationHeader);
		CustomUser user = repo.getById(id);
		// System.out.println("Role: " + role);
		if ("OWNER".equals(role)) {
			if (repo.existsById(id)) {
				if (user.getRole().equals("USER")) {
					proxy.deleteUsersAccount(user.getEmail());
					repo.deleteById(id);
					String successMessage = "User with ID " + id + " deleted.";
					return ResponseEntity.ok(successMessage);
				} else {
					repo.deleteById(id);
					String successMessage = "User with ID " + id + " deleted.";
					return ResponseEntity.ok(successMessage);
				}
			} else {
				String errorMessage = "User with ID " + id + " doesn't exist.";
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
			}
		} else {
			String errorMessage = "Only the user with role 'OWNER' can delete all users.";
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
		}
	}

	@GetMapping("/users-service/user/{email}")
	public Boolean getUserByEmail(@PathVariable String email) {
		CustomUser user = repo.findByEmail(email);
		if (user == null) {
			return false;
		} else
			return true;
	}

	@GetMapping("/users-service/user/role/{email}")
	public String getUsersRoleByEmail(@PathVariable String email) {
		CustomUser user = repo.findByEmail(email);
		if (user == null) {
			return null;
		} else
			return user.getRole();
	}
	
	@GetMapping("/users-service/current-user-role")
	public String getCurrentUserRole(@RequestHeader("Authorization") String authorizationHeader){
		String role = extractRoleFromAuthorizationHeader(authorizationHeader);
		return role;
	}
	@GetMapping("/users-service/current-user-email")
	public String getCurrentUserEmail(@RequestHeader("Authorization") String authorizationHeader){
		String email = extractEmailFromAuthorizationHeader(authorizationHeader);
		return email;
	}

	public String extractRoleFromAuthorizationHeader(String authorizationHeader) {
		String encodedCredentials = authorizationHeader.replaceFirst("Basic ", "");
		byte[] decodedBytes = Base64.decode(encodedCredentials.getBytes());
		String decodedCredentials = new String(decodedBytes);
		String[] credentials = decodedCredentials.split(":");
		String role = credentials[0]; // prvo se unosi email kao username korisnika
		CustomUser user = repo.findByEmail(role);
		return user.getRole();
	}
	public String extractEmailFromAuthorizationHeader(String authorizationHeader) {
		String encodedCredentials = authorizationHeader.replaceFirst("Basic ", "");
		byte[] decodedBytes = Base64.decode(encodedCredentials.getBytes());
		String decodedCredentials = new String(decodedBytes);
		String[] credentials = decodedCredentials.split(":");
		String role = credentials[0]; // prvo se unosi email kao username korisnika
		return role;
	}
	
}
