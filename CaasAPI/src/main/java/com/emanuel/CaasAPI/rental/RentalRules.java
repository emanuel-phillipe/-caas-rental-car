package com.emanuel.CaasAPI.rental;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class RentalRules {
    static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    static String text(String value, String label, int max) {
        if (value == null || value.isBlank() || value.strip().length() > max) throw invalid(label + " é obrigatório (até " + max + " caracteres).");
        return value.strip();
    }
    static void period(Rental.OrderInput input) {
        if (input.vehicleId() == null || input.startsOn() == null || input.endsOn() == null || !input.endsOn().isAfter(input.startsOn()))
            throw invalid("Informe um veículo e um período com fim posterior ao início.");
    }
    static void revision(Rental.Order order, Integer revision) {
        if (revision == null || revision != order.revision()) throw conflict("O pedido foi atualizado. Recarregue os dados antes de continuar.");
    }
    static void editable(Rental.Order order) {
        if (order.status().equals("CANCELADO") || order.status().equals("CONTRATADO")) throw conflict("Este pedido foi encerrado e não pode ser alterado.");
    }
    static void review(Rental.Order order, Rental.ReviewInput input) {
        revision(order, input.revision());
        if (!order.status().equals("EM_ANALISE")) throw conflict("Somente pedidos em análise podem receber um parecer.");
        if (!"FAVORAVEL".equals(input.result()) && !"DESFAVORAVEL".equals(input.result())) throw invalid("Resultado do parecer inválido.");
        text(input.notes(), "Justificativa", 2000);
    }
}
