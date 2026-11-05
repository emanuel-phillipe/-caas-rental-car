package com.emanuel.CaasAPI.auth;

import java.util.UUID;
import java.util.List;
import com.emanuel.CaasAPI.rental.Rental;
import com.emanuel.CaasAPI.rental.RentalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:postgresql://localhost:1/caas_test","spring.datasource.username=test","spring.datasource.password=test"})
@AutoConfigureMockMvc
class RentalAccessTests {
    @Autowired MockMvc mvc;
    @MockitoBean RentalRepository repository;
    final UUID id=UUID.randomUUID();
    AccountPrincipal actor(AccountPrincipal.Role role) { return new AccountPrincipal(id,"test","Test","unused",role,true); }
    @Test void businessReadsRequireSession() throws Exception {
        for(String path:List.of("/orders","/orders/"+id,"/vehicles","/contracts","/banks","/organization")) mvc.perform(get("/api"+path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }
    @Test void clientCannotPerformAgentOperations() throws Exception {
        for(String path:List.of("/vehicles","/credits","/orders/"+id+"/reviews","/orders/"+id+"/contract","/orders/"+id+"/bank"))
            mvc.perform(post("/api"+path).with(user(actor(AccountPrincipal.Role.CLIENTE))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }
    @Test void agentCannotCreateOrCancelClientOrders() throws Exception {
        for(String path:List.of("/orders","/orders/"+id+"/cancel")) mvc.perform(post("/api"+path).with(user(actor(AccountPrincipal.Role.AGENTE))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }
    @Test void writesRequireCsrf() throws Exception {
        mvc.perform(post("/api/orders").with(user(actor(AccountPrincipal.Role.CLIENTE))).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/orders/"+id).with(user(actor(AccountPrincipal.Role.AGENTE))).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }
    @Test void usesSessionIdentityAndReturnsConflictMessages() throws Exception {
        var actor=actor(AccountPrincipal.Role.CLIENTE);
        when(repository.orders(any())).thenReturn(List.of());
        mvc.perform(get("/api/orders").with(user(actor))).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(repository).orders(actor);
        when(repository.update(eq(actor),eq(id),any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,"Revisão desatualizada."));
        mvc.perform(put("/api/orders/"+id).with(user(actor)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"revision\":1}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Revisão desatualizada."));
    }
    @Test void rejectsMalformedIdentifiersAndDates() throws Exception {
        mvc.perform(get("/api/orders/not-a-uuid").with(user(actor(AccountPrincipal.Role.CLIENTE)))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").with(user(actor(AccountPrincipal.Role.CLIENTE))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"startsOn\":\"not-a-date\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }
}
