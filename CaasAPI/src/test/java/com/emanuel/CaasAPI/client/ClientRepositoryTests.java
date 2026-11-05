package com.emanuel.CaasAPI.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import com.emanuel.CaasAPI.database.SupabaseConnection;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ClientRepositoryTests {
    @Test
    void duplicateCpfRollsBackPreviouslyInsertedAccount() throws Exception {
        var database = mock(SupabaseConnection.class);
        var connection = mock(Connection.class);
        var insertAccount = mock(PreparedStatement.class);
        var insertClient = mock(PreparedStatement.class);
        when(database.open()).thenReturn(connection);
        when(connection.prepareStatement(contains("INSERT INTO caas_private.accounts"))).thenReturn(insertAccount);
        when(connection.prepareStatement(contains("INSERT INTO caas_private.clients"))).thenReturn(insertClient);
        when(insertClient.executeUpdate()).thenThrow(new SQLException("Sensitive database details", "23505"));
        var details = new Client.Details("Cliente", "123", "52998224725", "Rua A", "Analista", List.of());

        var error = catchThrowableOfType(ResponseStatusException.class,
                () -> new ClientRepository(database).create("cliente", "hash", details));

        assertThat(error.getStatusCode().value()).isEqualTo(409);
        assertThat(error.getReason()).isEqualTo("Usuário ou CPF já cadastrado.");
        verify(insertAccount).executeUpdate();
        verify(connection).setAutoCommit(false);
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    void databaseFailureDoesNotExposeCredentialsOrSql() throws Exception {
        var database = mock(SupabaseConnection.class);
        when(database.open()).thenThrow(new SQLException("password=secret host=internal", "08001"));
        var error = catchThrowableOfType(ResponseStatusException.class,
                () -> new ClientRepository(database).find(UUID.randomUUID()));
        assertThat(error.getStatusCode().value()).isEqualTo(503);
        assertThat(error.getReason()).doesNotContain("secret", "internal");
    }
}
