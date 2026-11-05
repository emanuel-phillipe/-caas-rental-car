package com.emanuel.CaasAPI.rental;

import java.util.*;
import com.emanuel.CaasAPI.auth.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import static com.emanuel.CaasAPI.rental.Rental.*;

@RestController
@RequestMapping("/api")
public class RentalController {
    private final RentalRepository service;
    public RentalController(RentalRepository service) { this.service=service; }
    @GetMapping("/organization") @PreAuthorize("hasRole('AGENTE')")
    public Organization organization(@AuthenticationPrincipal AccountPrincipal actor) { return service.organization(actor); }
    @GetMapping("/vehicles")
    public List<Vehicle> vehicles(@AuthenticationPrincipal AccountPrincipal actor) { return service.vehicles(actor); }
    @PostMapping("/vehicles") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('AGENTE')")
    public Vehicle createVehicle(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody VehicleInput input) { return service.saveVehicle(actor,null,input); }
    @PutMapping("/vehicles/{id}") @PreAuthorize("hasRole('AGENTE')")
    public Vehicle updateVehicle(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody VehicleInput input) { return service.saveVehicle(actor,id,input); }
    @GetMapping("/orders")
    public List<Order> orders(@AuthenticationPrincipal AccountPrincipal actor) { return service.orders(actor); }
    @GetMapping("/orders/{id}")
    public Detail detail(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id) { return service.detail(actor,id); }
    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('CLIENTE')")
    public Order create(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody OrderInput input) { return service.create(actor,input); }
    @PutMapping("/orders/{id}")
    public Order update(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody OrderInput input) { return service.update(actor,id,input); }
    @PostMapping("/orders/{id}/cancel") @PreAuthorize("hasRole('CLIENTE')")
    public Order cancel(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody Revision input) { return service.cancel(actor,id,input); }
    @PostMapping("/orders/{id}/reviews") @PreAuthorize("hasRole('AGENTE')")
    public Order evaluate(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody ReviewInput input) { return service.evaluate(actor,id,input); }
    @PostMapping("/orders/{id}/contract") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('AGENTE')")
    public Contract formalize(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody ContractInput input) { return service.formalize(actor,id,input); }
    @PostMapping("/credits") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('AGENTE')")
    public Credit credit(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody CreditInput input) { return service.createCredit(actor,input); }
    @GetMapping("/banks") @PreAuthorize("hasRole('AGENTE')")
    public List<Organization> banks(@AuthenticationPrincipal AccountPrincipal actor) { return service.banks(actor); }
    @PostMapping("/orders/{id}/bank") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('AGENTE')")
    public void assignBank(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody BankInput input) { service.assignBank(actor,id,input); }
    @GetMapping("/contracts")
    public List<Contract> contracts(@AuthenticationPrincipal AccountPrincipal actor) { return service.contracts(actor); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> error(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"Solicitação inválida."))); }
}
