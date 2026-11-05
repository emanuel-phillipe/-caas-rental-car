package com.emanuel.CaasAPI.auth;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.emanuel.CaasAPI.client.Client;
import com.emanuel.CaasAPI.client.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockHttpSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:1/caas_test",
        "spring.datasource.username=test",
        "spring.datasource.password=test"
})
@AutoConfigureMockMvc
class ClientCrudTests {
    private static final UUID ID = UUID.randomUUID();
    private static final String DETAILS = """
            {"name":" Emanuel ","rg":"1234567","cpf":"529.982.247-25","address":"Rua A, 10",
             "profession":"Analista","employments":[{"employer":"Empresa A","income":2500.50}]}
            """;

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean ClientRepository repository;

    private AccountPrincipal client() {
        return new AccountPrincipal(ID, "emanuel", "Emanuel", "unused", AccountPrincipal.Role.CLIENTE, true);
    }
    private Client stored() {
        return new Client(ID, "emanuel", "Emanuel", "1234567", "52998224725", "Rua A, 10", "Analista",
                List.of(new Client.Employment("Empresa A", new BigDecimal("2500.50"))));
    }
    private String registration(String details) {
        return "{\"username\":\" Emanuel \",\"password\":\"senha-segura\",\"details\":" + details + "}";
    }

    @Test
    void registersPublicClientAndHashesPassword() throws Exception {
        when(repository.create(anyString(), anyString(), any())).thenReturn(stored());
        mvc.perform(post("/api/clients").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registration(DETAILS)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/clients/me"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        var hash = ArgumentCaptor.forClass(String.class);
        var details = ArgumentCaptor.forClass(Client.Details.class);
        verify(repository).create(eq("emanuel"), hash.capture(), details.capture());
        assertThat(encoder.matches("senha-segura", hash.getValue())).isTrue();
        assertThat(details.getValue().name()).isEqualTo("Emanuel");
        assertThat(details.getValue().cpf()).isEqualTo("52998224725");
    }

    @Test
    void requiresSessionClientRoleAndCsrf() throws Exception {
        mvc.perform(get("/api/clients/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(registration(DETAILS)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/clients/me").with(user(client())).contentType(MediaType.APPLICATION_JSON).content(DETAILS))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/clients/me").with(user(client()))).andExpect(status().isForbidden());
        for (var request : List.of(get("/api/clients/me"), put("/api/clients/me").contentType(MediaType.APPLICATION_JSON).content(DETAILS), delete("/api/clients/me"))) {
            mvc.perform(request.with(user("agent").roles("AGENTE")).with(csrf())).andExpect(status().isForbidden());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void readsAndUpdatesOnlyAuthenticatedAccount() throws Exception {
        when(repository.find(ID)).thenReturn(stored());
        when(repository.update(eq(ID), any())).thenReturn(stored());
        mvc.perform(get("/api/clients/me").with(user(client())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ID.toString()));
        // An injected identifier cannot select another client's data.
        String payload = DETAILS.replace("{\"name\"", "{\"id\":\"" + UUID.randomUUID() + "\",\"name\"");
        mvc.perform(put("/api/clients/me").with(user(client())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
        verify(repository).find(ID);
        verify(repository).update(eq(ID), any());
    }

    @Test
    void deletesAccountAndInvalidatesSession() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(delete("/api/clients/me").session(session).with(user(client())).with(csrf()))
                .andExpect(status().isNoContent());
        verify(repository).delete(ID);
        assertThat(session.isInvalid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11111111111", "52998224724", "123", "abcdefghijk"})
    void rejectsInvalidCpf(String cpf) throws Exception {
        mvc.perform(post("/api/clients").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(registration(DETAILS.replace("529.982.247-25", cpf))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Informe um CPF válido."));
        verifyNoInteractions(repository);
    }

    @Test
    void validatesRequiredFieldsPasswordAndEmploymentLimits() throws Exception {
        String job = "{\"employer\":\"Empresa\",\"income\":100}";
        List<String> invalidDetails = List.of(
                DETAILS.replace(" Emanuel ", " "), DETAILS.replace("1234567", ""),
                DETAILS.replace("Rua A, 10", ""), DETAILS.replace("Analista", ""),
                DETAILS.replace("2500.50", "-1"), DETAILS.replace("2500.50", "1.001"),
                DETAILS.replace("Empresa A", " "),
                DETAILS.replace("[{\"employer\":\"Empresa A\",\"income\":2500.50}]", "[" + String.join(",", job, job, job, job) + "]"));
        for (String invalid : invalidDetails) {
            mvc.perform(put("/api/clients/me").with(user(client())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        for (String password : List.of("short", "a".repeat(73), "á".repeat(37))) {
            mvc.perform(post("/api/clients").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(registration(DETAILS).replace("senha-segura", password))).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsZeroOrThreeJobs() throws Exception {
        when(repository.update(eq(ID), any())).thenReturn(stored());
        String job = "{\"employer\":\"Empresa A\",\"income\":2500.50}";
        for (String jobs : List.of("[]", "[" + String.join(",", job, job, job) + "]")) {
            mvc.perform(put("/api/clients/me").with(user(client())).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(DETAILS.replace("[" + job + "]", jobs))).andExpect(status().isOk());
        }
    }

    @Test
    void reportsConflictNotFoundAndUnavailableWithoutDatabaseDetails() throws Exception {
        when(repository.create(anyString(), anyString(), any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Usuário ou CPF já cadastrado."));
        mvc.perform(post("/api/clients").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registration(DETAILS)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Usuário ou CPF já cadastrado."));
        when(repository.find(ID)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro de cliente não encontrado."));
        mvc.perform(get("/api/clients/me").with(user(client()))).andExpect(status().isNotFound());
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível acessar os dados. Tente novamente.")).when(repository).delete(ID);
        var session = new MockHttpSession();
        mvc.perform(delete("/api/clients/me").session(session).with(user(client())).with(csrf())).andExpect(status().isServiceUnavailable());
        assertThat(session.isInvalid()).isFalse();
    }
}
