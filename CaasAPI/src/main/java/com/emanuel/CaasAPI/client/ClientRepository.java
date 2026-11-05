package com.emanuel.CaasAPI.client;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import com.emanuel.CaasAPI.database.SupabaseConnection;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class ClientRepository {
    private static final Logger log = LoggerFactory.getLogger(ClientRepository.class);
    private final SupabaseConnection database;
    public ClientRepository(SupabaseConnection database) { this.database = database; }

    public Client create(String username, String passwordHash, Client.Details details) {
        return transaction(connection -> {
            UUID id = UUID.randomUUID();
            try (var statement = connection.prepareStatement("""
                    INSERT INTO caas_private.accounts (id, username, display_name, password_hash, role)
                    VALUES (?, ?, ?, ?, 'CLIENTE')
                    """)) {
                statement.setObject(1, id);
                statement.setString(2, username);
                statement.setString(3, details.name());
                statement.setString(4, passwordHash);
                statement.executeUpdate();
            }
            save(connection, id, details, true);
            return read(connection, id, false);
        });
    }

    public Client find(UUID id) { return transaction(connection -> read(connection, id, false)); }

    public Client update(UUID id, Client.Details details) {
        return transaction(connection -> {
            var client = read(connection, id, true);
            save(connection, client.id(), details, false);
            try (var statement = connection.prepareStatement("UPDATE caas_private.accounts SET display_name = ? WHERE id = ?")) {
                statement.setString(1, details.name());
                statement.setObject(2, client.id());
                statement.executeUpdate();
            }
            return read(connection, id, false);
        });
    }

    public void delete(UUID id) {
        transaction(connection -> {
            var client = read(connection, id, true);
            try (var statement = connection.prepareStatement("DELETE FROM caas_private.accounts WHERE id = ?")) {
                statement.setObject(1, client.id());
                statement.executeUpdate();
            }
            return null;
        });
    }

    private Client read(Connection connection, UUID accountId, boolean lock) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT a.id, a.username, a.display_name, c.rg, c.cpf, c.address, c.profession
                FROM caas_private.accounts a JOIN caas_private.clients c ON c.account_id = a.id
                WHERE a.id = ? AND a.role = 'CLIENTE' AND a.enabled = true
                """ + (lock ? " FOR UPDATE OF a, c" : ""))) {
            statement.setObject(1, accountId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro de cliente não encontrado.");
                UUID id = result.getObject("id", UUID.class);
                var jobs = new ArrayList<Client.Employment>();
                try (var query = connection.prepareStatement("SELECT employer, income FROM caas_private.client_employments WHERE client_id = ? ORDER BY position")) {
                    query.setObject(1, id);
                    try (var rows = query.executeQuery()) {
                        while (rows.next()) jobs.add(new Client.Employment(rows.getString(1), rows.getBigDecimal(2)));
                    }
                }
                return new Client(id, result.getString("username"), result.getString("display_name"),
                        result.getString("rg"), result.getString("cpf"), result.getString("address"),
                        result.getString("profession"), jobs);
            }
        }
    }

    private void save(Connection connection, UUID id, Client.Details details, boolean create) throws SQLException {
        String sql = create
                ? "INSERT INTO caas_private.clients (rg, cpf, address, profession, account_id) VALUES (?, ?, ?, ?, ?)"
                : "UPDATE caas_private.clients SET rg = ?, cpf = ?, address = ?, profession = ? WHERE account_id = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, details.rg());
            statement.setString(2, details.cpf());
            statement.setString(3, details.address());
            statement.setString(4, details.profession());
            statement.setObject(5, id);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("DELETE FROM caas_private.client_employments WHERE client_id = ?")) {
            statement.setObject(1, id);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("INSERT INTO caas_private.client_employments (client_id, position, employer, income) VALUES (?, ?, ?, ?)")) {
            int position = 0;
            for (var job : details.employments()) {
                statement.setObject(1, id);
                statement.setInt(2, ++position);
                statement.setString(3, job.employer());
                statement.setBigDecimal(4, job.income());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private <T> T transaction(Operation<T> operation) {
        try (var connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                T result = operation.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário ou CPF já cadastrado.");
            if ("23503".equals(exception.getSQLState()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O cliente possui registros vinculados e não pode ser excluído.");
            // Não registrar a mensagem/stack trace JDBC: podem conter credenciais ou dados pessoais.
            String state = exception.getSQLState();
            String hint = switch (state == null ? "" : state) {
                case "42P01", "3F000" -> "Tabela/schema ausente: aplique as migrations de contas e clientes.";
                case "42501" -> "Permissão insuficiente: confira os GRANTs do usuário do backend.";
                case "28P01", "28000" -> "Autenticação do banco recusada: confira SUPABASE_DB_USER e SUPABASE_DB_PASSWORD.";
                case "08001", "08006" -> "Falha de conexão: confira SUPABASE_DB_URL (ou SUPABASE_JDBC_URL), rede e parâmetros SSL.";
                default -> "Confira a conexão e o schema do Supabase.";
            };
            log.error("Falha no banco durante CRUD de cliente. SQLState={}; causa={}; {}", state,
                    exception.getCause() == null ? exception.getClass().getSimpleName()
                            : exception.getCause().getClass().getSimpleName(), hint);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível acessar os dados. Tente novamente.");
        }
    }

    @FunctionalInterface
    private interface Operation<T> { T run(Connection connection) throws SQLException; }
}
