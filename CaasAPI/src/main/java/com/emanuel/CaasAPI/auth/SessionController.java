package com.emanuel.CaasAPI.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class SessionController {

	@GetMapping("/csrf")
	CsrfResponse csrf(CsrfToken csrf) {
		return new CsrfResponse(csrf.getHeaderName(), csrf.getToken());
	}

	@GetMapping("/session")
	AccountPrincipal.Profile session(@AuthenticationPrincipal AccountPrincipal account) {
		return account.profile();
	}

	record CsrfResponse(String headerName, String token) {}

}
