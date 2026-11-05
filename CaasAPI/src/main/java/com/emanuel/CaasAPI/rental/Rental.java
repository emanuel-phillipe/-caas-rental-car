package com.emanuel.CaasAPI.rental;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import com.emanuel.CaasAPI.client.Client;

public final class Rental {
    private Rental() {}
    public record Organization(UUID id, String name, String kind) {}
    public record Vehicle(UUID id, String registration, int year, String brand, String model, String plate,
            UUID ownerId, String ownerName, String ownerKind, UUID organizationId, boolean active) {}
    public record VehicleInput(String registration, int year, String brand, String model, String plate,
            UUID ownerClientId, boolean active) {}
    public record Order(UUID id, UUID clientId, String clientName, UUID vehicleId, String vehicleName,
            String plate, LocalDate startsOn, LocalDate endsOn, int revision, String status, Instant createdAt) {}
    public record OrderInput(UUID vehicleId, LocalDate startsOn, LocalDate endsOn, Integer revision) {}
    public record Revision(Integer revision) {}
    public record BankInput(UUID bankId) {}
    public record ReviewInput(Integer revision, String result, String notes) {}
    public record Review(UUID id, int revision, String representative, String result, String notes, Instant createdAt) {}
    public record ContractInput(Integer revision, String kind, UUID creditId) {}
    public record Contract(UUID id, UUID orderId, String kind, UUID ownerId, String ownerName, String ownerKind,
            UUID creditId, Instant createdAt) {}
    public record Credit(UUID id, String reference, UUID bankId, String bankName, UUID clientId,
            BigDecimal amount, boolean linked) {}
    public record CreditInput(UUID orderId, String reference, BigDecimal amount) {}
    public record Detail(Order order, Vehicle vehicle, Client client, List<Review> reviews, Contract contract, List<Credit> credits, List<Organization> organizations) {}
}
