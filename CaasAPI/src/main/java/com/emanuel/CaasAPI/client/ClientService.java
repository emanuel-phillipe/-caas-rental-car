package com.emanuel.CaasAPI.client;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClientService {
    private final ClientRepository repository;
    private final PasswordEncoder encoder;

    public ClientService(ClientRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public Client create(Client.Registration registration) {
        String username = text(registration.username(), "Usuário", 64).toLowerCase(Locale.ROOT);
        if (!username.matches("[a-z0-9][a-z0-9._-]{0,63}"))
            throw invalid("Usuário deve conter apenas letras, números, ponto, hífen ou sublinhado.");
        String password = registration.password();
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw invalid("A senha deve ter pelo menos 8 caracteres e no máximo 72 bytes.");
        Client.Details details = validate(registration.details());
        return repository.create(username, encoder.encode(password), details);
    }

    public Client find(UUID id) { return repository.find(id); }
    public Client update(UUID id, Client.Details details) {
        return repository.update(id, validate(details));
    }
    public void delete(UUID id) { repository.delete(id); }

    private Client.Details validate(Client.Details details) {
        if (details == null) throw invalid("Informe os dados do cliente.");
        String cpf = text(details.cpf(), "CPF", 14).replaceAll("[.\\-\\s]", "");
        if (!validCpf(cpf)) throw invalid("Informe um CPF válido.");
        List<Client.Employment> jobs = details.employments() == null ? List.of() : details.employments();
        if (jobs.size() > 3) throw invalid("Informe no máximo três vínculos empregatícios.");
        var normalized = jobs.stream().map(job -> {
            if (job == null || job.income() == null || job.income().signum() < 0
                    || job.income().compareTo(new BigDecimal("9999999999.99")) > 0
                    || job.income().stripTrailingZeros().scale() > 2)
                throw invalid("Informe um rendimento válido, não negativo e com até duas casas decimais.");
            return new Client.Employment(text(job.employer(), "Empregador", 160), job.income());
        }).toList();
        return new Client.Details(text(details.name(), "Nome", 160), text(details.rg(), "RG", 30), cpf,
                text(details.address(), "Endereço", 300), text(details.profession(), "Profissão", 120), normalized);
    }

    private String text(String value, String field, int max) {
        if (value == null || value.isBlank() || value.strip().length() > max)
            throw invalid(field + " é obrigatório e deve ter até " + max + " caracteres.");
        return value.strip();
    }

    private boolean validCpf(String cpf) {
        if (!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1) return false;
        for (int length = 9; length <= 10; length++) {
            int sum = 0;
            for (int i = 0; i < length; i++) sum += (cpf.charAt(i) - '0') * (length + 1 - i);
            int digit = (sum * 10) % 11;
            if (digit == 10) digit = 0;
            if (digit != cpf.charAt(length) - '0') return false;
        }
        return true;
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
