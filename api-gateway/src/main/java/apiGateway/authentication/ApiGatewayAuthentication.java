package apiGateway.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;

import authentication.dtos.CustomUserDto;

@Configuration
@EnableWebFluxSecurity
public class ApiGatewayAuthentication  {
	
	private static final String ROLE = "X-User-Role";
	/*@Bean
	public MapReactiveUserDetailsService userDetailsService(BCryptPasswordEncoder encoder) {
		List<UserDetails> users = new ArrayList<>();
		users.add(User.withUsername("user")
				.password(encoder.encode("password1"))
				.roles("USER")
				.build());
		
		users.add(User.withUsername("admin")
				.password(encoder.encode("password2"))
				.roles("ADMIN")
				.build());
		
		return new MapReactiveUserDetailsService(users);
	}*/
	
	@Bean
	public MapReactiveUserDetailsService userDetailsService(BCryptPasswordEncoder encoder) {
		List<UserDetails> users = new ArrayList<>();
		List<CustomUserDto> usersFromDatabase;
        

		ResponseEntity<CustomUserDto[]> response = 
		new RestTemplate().getForEntity("http://localhost:8770/users-service/users", CustomUserDto[].class);
		
		usersFromDatabase = Arrays.asList(response.getBody());
		
		for(CustomUserDto cud: usersFromDatabase) {
			String role = cud.getRole();
			users.add(User.withUsername(cud.getEmail())
					.password(encoder.encode(cud.getPassword()))
					.roles(role)
					.build());
			List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(cud.getRole());
			/*HttpHeaders headers = new HttpHeaders();
			headers.set("Role", role); */
		}
		
		
		return new MapReactiveUserDetailsService(users);
	}
	
	@Bean
	public BCryptPasswordEncoder getEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityWebFilterChain filterChain(ServerHttpSecurity http ) throws Exception {
	    http.csrf().disable()
	        .authorizeExchange()
	        .pathMatchers(HttpMethod.POST).hasRole("ADMIN")
	        .pathMatchers("/currency-exchange/**").permitAll()
	        .pathMatchers("/users-service/**").permitAll()
	        .pathMatchers("/currency-conversion").hasAnyRole("ADMIN", "USER")
	        //.pathMatchers(HttpMethod.POST,"/bank-account/**").hasRole("ADMIN")
	        //.pathMatchers(HttpMethod.PUT,"/bank-account/**").hasRole("ADMIN")
	        .and()
	        .httpBasic();

	    return http.build();
	}
	
	/*@Bean
	public WebClient.Builder webClientBuilder() {
	    return WebClient.builder()
	            .filter((request, next) -> {
	                // Add the user's role to the Authorization header
	                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	                if (authentication != null && authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
	                    String role = authentication.getAuthorities().iterator().next().getAuthority();
	                    request = ClientRequest.from(request)
	                            .headers(headers -> headers.set(ROLE, role))
	                            .build();
	                }
	                return next.exchange(request);
	            });
	}*/
	



	

}

