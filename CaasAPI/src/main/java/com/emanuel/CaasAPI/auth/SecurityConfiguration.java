package com.emanuel.CaasAPI.auth;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import com.emanuel.CaasAPI.database.SupabaseConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfiguration {

	@Bean
	UserDetailsService accounts(SupabaseConnection database) {
		return new SupabaseAccounts(database);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12) {
			@Override
			protected boolean matchesNonNull(String rawPassword, String encodedPassword) {
				return rawPassword.getBytes(StandardCharsets.UTF_8).length <= 72
						&& super.matchesNonNull(rawPassword, encodedPassword);
			}
		};
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) {
		return http
				.authorizeHttpRequests(auth -> auth
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/clients").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/organization", "/api/banks").hasRole("AGENTE")
                        .requestMatchers(HttpMethod.POST, "/api/vehicles", "/api/credits",
                                "/api/orders/*/reviews", "/api/orders/*/contract", "/api/orders/*/bank").hasRole("AGENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/vehicles/*").hasRole("AGENTE")
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/*/cancel").hasRole("CLIENTE")
                        .anyRequest().hasAnyRole("CLIENTE", "AGENTE"))
				.requestCache(cache -> cache.disable())
				.exceptionHandling(errors -> errors.authenticationEntryPoint(
						(request, response, exception) -> response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
				.formLogin(login -> login
						.loginPage("/login")
						.loginProcessingUrl("/api/auth/login")
						.successHandler((request, response, authentication) ->
								response.setStatus(HttpServletResponse.SC_NO_CONTENT))
						.failureHandler((request, response, exception) -> response.setStatus(
								exception instanceof AuthenticationServiceException
										? HttpServletResponse.SC_SERVICE_UNAVAILABLE : HttpServletResponse.SC_UNAUTHORIZED)))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.deleteCookies("CAAS_SESSION")
						.logoutSuccessHandler((request, response, authentication) ->
								response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
				.build();
	}

}
