package com.emanuel.CaasAPI.auth;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:1/caas_test",
        "spring.datasource.username=test",
        "spring.datasource.password=test"
})
@AutoConfigureMockMvc
class AuthenticationTests {

	private static final String PASSWORD = "senha-teste";
	private static final String PASSWORD_HASH = new BCryptPasswordEncoder(4).encode(PASSWORD);

	@Autowired
	MockMvc mvc;

	@TestBean
	UserDetailsService accounts;

	static UserDetailsService accounts() {
		return username -> switch (username) {
			case "cliente" -> account(username, AccountPrincipal.Role.CLIENTE, true);
			case "agente" -> account(username, AccountPrincipal.Role.AGENTE, true);
			case "inativo" -> account(username, AccountPrincipal.Role.CLIENTE, false);
			default -> throw new UsernameNotFoundException("Conta não encontrada");
		};
	}

	@Test
	void rejectsUnauthenticatedAccessAndLoginWithoutCsrf() throws Exception {
		mvc.perform(get("/api/auth/session")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/auth/login")
				.param("username", "cliente").param("password", PASSWORD))
				.andExpect(status().isForbidden());
	}

	@Test
	void rejectsUnknownAccountsWrongPasswordsAndDisabledAccounts() throws Exception {
		CsrfRequest csrf = csrf(null);
		login(csrf, "desconhecido", PASSWORD).andExpect(status().isUnauthorized());
		login(csrf, "cliente", "senha-errada").andExpect(status().isUnauthorized());
		login(csrf, "inativo", PASSWORD).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/auth/session").session(csrf.session()))
				.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@EnumSource(AccountPrincipal.Role.class)
	void persistsEachProfileAndInvalidatesSessionOnLogout(AccountPrincipal.Role role) throws Exception {
		String username = role == AccountPrincipal.Role.CLIENTE ? "cliente" : "agente";
		CsrfRequest beforeLogin = csrf(null);
		String anonymousSessionId = beforeLogin.session().getId();

		MvcResult login = login(beforeLogin, username, PASSWORD).andExpect(status().isNoContent()).andReturn();
		MockHttpSession authenticated = (MockHttpSession) login.getRequest().getSession(false);
		assertThat(authenticated.getId()).isNotEqualTo(anonymousSessionId);
		mvc.perform(get("/api/auth/session").session(authenticated))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.role").value(role.name()))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		mvc.perform(post("/api/auth/logout").session(authenticated).header(beforeLogin.headerName(), beforeLogin.token()))
				.andExpect(status().isForbidden());
		CsrfRequest beforeLogout = csrf(authenticated);
		mvc.perform(post("/api/auth/logout").session(authenticated).header(beforeLogout.headerName(), beforeLogout.token()))
				.andExpect(status().isNoContent());
		assertThat(authenticated.isInvalid()).isTrue();
		mvc.perform(get("/api/auth/session")).andExpect(status().isUnauthorized());
	}

	private ResultActions login(CsrfRequest csrf, String username, String password)
			throws Exception {
		return mvc.perform(post("/api/auth/login").session(csrf.session()).with(request -> {
                    // Simula o cookie de sessão existente enviado pelo navegador.
                    request.setRequestedSessionId(csrf.session().getId());
                    request.setRequestedSessionIdValid(true);
                    return request;
                }).header(csrf.headerName(), csrf.token())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("username", username).param("password", password));
	}

	private CsrfRequest csrf(MockHttpSession session) throws Exception {
		var request = get("/api/auth/csrf");
		if (session != null) {
			request.session(session);
		}
		MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
		var json = JsonPath.parse(result.getResponse().getContentAsString());
		return new CsrfRequest((MockHttpSession) result.getRequest().getSession(false),
				json.read("$.headerName"), json.read("$.token"));
	}

	private static AccountPrincipal account(String username, AccountPrincipal.Role role, boolean enabled) {
		return new AccountPrincipal(UUID.randomUUID(), username, username, PASSWORD_HASH, role, enabled);
	}

	private record CsrfRequest(MockHttpSession session, String headerName, String token) {}
}
