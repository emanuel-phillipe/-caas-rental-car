package com.emanuel.CaasAPI.rental;

import com.emanuel.CaasAPI.auth.AccountPrincipal;
import com.emanuel.CaasAPI.database.SupabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RentalRepositoryTests {
    final UUID id=UUID.randomUUID(), clientId=UUID.randomUUID(), vehicleId=UUID.randomUUID(), orgId=UUID.randomUUID();
    final SupabaseConnection database=mock(SupabaseConnection.class);
    final Connection connection=mock(Connection.class);
    final RentalRepository repository=new RentalRepository(database);
    RentalRepositoryTests() throws Exception { when(database.open()).thenReturn(connection); }
    AccountPrincipal actor(UUID accountId,boolean agent) {
        var actor=mock(AccountPrincipal.class);
        when(actor.getId()).thenReturn(accountId);
        doReturn(List.of(new SimpleGrantedAuthority(agent?"ROLE_AGENTE":"ROLE_CLIENTE"))).when(actor).getAuthorities();
        return actor;
    }
    PreparedStatement rows(String sql,ResultSet rows) throws Exception {
        var statement=mock(PreparedStatement.class);
        when(connection.prepareStatement(contains(sql))).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        return statement;
    }
    ResultSet order(String status) throws Exception {
        var r=mock(ResultSet.class); when(r.next()).thenReturn(true,false);
        when(r.getObject("id",UUID.class)).thenReturn(id); when(r.getObject("client_id",UUID.class)).thenReturn(clientId); when(r.getObject("vehicle_id",UUID.class)).thenReturn(vehicleId);
        when(r.getInt("revision")).thenReturn(2); when(r.getString("status")).thenReturn(status);
        when(r.getDate(anyString())).thenReturn(java.sql.Date.valueOf("2026-12-01")); when(r.getTimestamp("created_at")).thenReturn(Timestamp.valueOf("2026-11-05 12:00:00"));
        rows("from caas_private.rental_orders o",r);
        return r;
    }
    void organization() throws Exception {
        var r=mock(ResultSet.class); when(r.next()).thenReturn(true,false);
        when(r.next()).thenReturn(true,false,true,false,true,false);
        when(r.getObject("id",UUID.class)).thenReturn(orgId); when(r.getString("kind")).thenReturn("EMPRESA");
        rows("join caas_private.representatives",r);
    }
    @Test void rejectsAnotherClientsOrderBeforeReadingPrivateDetails() throws Exception {
        order("EM_ANALISE");
        var error=catchThrowableOfType(ResponseStatusException.class,() -> repository.detail(actor(UUID.randomUUID(),false),id));
        assertThat(error.getStatusCode().value()).isEqualTo(403);
        verify(connection).rollback(); verify(connection,never()).commit();
        verify(connection,never()).prepareStatement(contains("client_employments"));
    }
    @Test void rejectsAgentOutsideAssignment() throws Exception {
        organization(); order("EM_ANALISE");
        var empty=mock(ResultSet.class); when(empty.next()).thenReturn(false); rows("select order_id from caas_private.order_assignments",empty);
        var error=catchThrowableOfType(ResponseStatusException.class,() -> repository.detail(actor(UUID.randomUUID(),true),id));
        assertThat(error.getStatusCode().value()).isEqualTo(403);
        verify(connection).rollback(); verify(connection,never()).prepareStatement(contains("client_employments"));
    }
    @Test void staleRevisionRollsBackWithoutChangingOrder() throws Exception {
        order("APROVADO");
        var input=new Rental.OrderInput(vehicleId,LocalDate.of(2026,12,1),LocalDate.of(2026,12,5),1);
        var error=catchThrowableOfType(ResponseStatusException.class,() -> repository.update(actor(clientId,false),id,input));
        assertThat(error.getStatusCode().value()).isEqualTo(409);
        verify(connection).prepareStatement(contains("for update of o")); verify(connection).rollback();
        verify(connection,never()).prepareStatement(startsWith("update"));
    }
    @Test void terminalCancellationDoesNotMutate() throws Exception {
        order("CONTRATADO");
        var error=catchThrowableOfType(ResponseStatusException.class,() -> repository.cancel(actor(clientId,false),id,new Rental.Revision(2)));
        assertThat(error.getStatusCode().value()).isEqualTo(409);
        verify(connection).rollback(); verify(connection,never()).prepareStatement(startsWith("update"));
    }
    void assigned() throws Exception {
        var r=mock(ResultSet.class); when(r.next()).thenReturn(true,false,true,false); when(r.getObject(1)).thenReturn(id);
        rows("select order_id from caas_private.order_assignments",r);
    }
    @Test void formalizationWithoutCurrentFavorableReviewIsRejected() throws Exception {
        organization(); assigned(); order("APROVADO");
        var r=mock(ResultSet.class); when(r.next()).thenReturn(false); rows("select id from caas_private.financial_reviews",r);
        var error=catchThrowableOfType(ResponseStatusException.class,() -> repository.formalize(actor(UUID.randomUUID(),true),id,new Rental.ContractInput(2,"LOCACAO_EMPRESA",null)));
        assertThat(error.getStatusCode().value()).isEqualTo(409);
        verify(connection).rollback(); verify(connection,never()).prepareStatement(startsWith("insert"));
    }
    @Test void reviewAndStatusAreAtomicWhenUpdateFails() throws Exception {
        organization(); assigned(); order("EM_ANALISE");
        var insert=mock(PreparedStatement.class); var update=mock(PreparedStatement.class);
        when(connection.prepareStatement(startsWith("insert into caas_private.financial_reviews"))).thenReturn(insert);
        when(connection.prepareStatement(startsWith("update caas_private.rental_orders"))).thenReturn(update);
        when(update.executeUpdate()).thenThrow(new SQLException("internal", "08006"));
        var error=catchThrowableOfType(ResponseStatusException.class,() -> repository.evaluate(actor(UUID.randomUUID(),true),id,new Rental.ReviewInput(2,"FAVORAVEL","Renda validada")));
        assertThat(error.getStatusCode().value()).isEqualTo(503);
        verify(insert).executeUpdate(); verify(update).setObject(1,"APROVADO");
        verify(connection).rollback(); verify(connection,never()).commit();
    }
    @Test void cancelPreservesOrderAndCommitsStatus() throws Exception {
        var r=order("EM_ANALISE"); when(r.next()).thenReturn(true,false,true,false);
        var update=mock(PreparedStatement.class);
        when(connection.prepareStatement(startsWith("update caas_private.rental_orders"))).thenReturn(update);
        repository.cancel(actor(clientId,false),id,new Rental.Revision(2));
        verify(connection).prepareStatement(contains("set status='CANCELADO'"));
        verify(update).executeUpdate(); verify(connection).commit(); verify(connection,never()).rollback();
        verify(connection,never()).prepareStatement(startsWith("delete"));
    }
    @Test void invalidCreditAmountsAreRejectedBeforeDatabaseAccess() {
        for(String amount:List.of("-1","0","1.001","1E+100","10000000000")) {
            var input=new Rental.CreditInput(id,"CRED-001",new java.math.BigDecimal(amount));
            var error=catchThrowableOfType(ResponseStatusException.class,() -> repository.createCredit(actor(UUID.randomUUID(),true),input));
            assertThat(error.getStatusCode().value()).isEqualTo(400);
        }
        verifyNoInteractions(database);
    }
    @Test void sqlFailuresAreSanitizedAndConnectionClosed() throws Exception {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("password=secret","08006"));
        var error=catchThrowableOfType(ResponseStatusException.class,() -> repository.orders(actor(clientId,false)));
        assertThat(error.getStatusCode().value()).isEqualTo(503); assertThat(error.getReason()).doesNotContain("secret");
        verify(connection).rollback(); verify(connection).close();
    }
}
