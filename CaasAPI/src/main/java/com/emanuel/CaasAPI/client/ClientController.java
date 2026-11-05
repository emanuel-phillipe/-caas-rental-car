package com.emanuel.CaasAPI.client;

import com.emanuel.CaasAPI.auth.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final ClientService service;
    public ClientController(ClientService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Client> create(@RequestBody Client.Registration registration) {
        return ResponseEntity.created(URI.create("/api/clients/me")).body(service.create(registration));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public Client find(@AuthenticationPrincipal AccountPrincipal principal) { return service.find(principal.getId()); }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public Client update(@AuthenticationPrincipal AccountPrincipal principal, @RequestBody Client.Details details) {
        return service.update(principal.getId(), details);
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Void> delete(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        service.delete(((AccountPrincipal) authentication.getPrincipal()).getId());
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Error> error(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new Error(exception.getReason()));
    }
    public record Error(String message) {}
}
