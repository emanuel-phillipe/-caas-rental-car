package com.emanuel.CaasAPI.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record Client(UUID id, String username, String name, String rg, String cpf,
        String address, String profession, List<Employment> employments) {
    public record Employment(String employer, BigDecimal income) {}
    public record Details(String name, String rg, String cpf, String address,
            String profession, List<Employment> employments) {}
    public record Registration(String username, String password, Details details) {}
}
