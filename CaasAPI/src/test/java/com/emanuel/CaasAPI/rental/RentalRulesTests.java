package com.emanuel.CaasAPI.rental;

import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class RentalRulesTests {
    private Rental.Order order(String status) { return new Rental.Order(UUID.randomUUID(),UUID.randomUUID(),"Cliente",UUID.randomUUID(),"Carro","ABC1D23",LocalDate.of(2026,12,1),LocalDate.of(2026,12,3),2,status,Instant.now()); }
    @Test void rejectsMissingVehicleAndInvalidPeriods() {
        var start=LocalDate.of(2026,12,1);
        for(var input: new Rental.OrderInput[]{new Rental.OrderInput(null,start,start.plusDays(1),null), new Rental.OrderInput(UUID.randomUUID(),start,start,null), new Rental.OrderInput(UUID.randomUUID(),start,start.minusDays(1),null), new Rental.OrderInput(UUID.randomUUID(),null,start,null)})
            assertThatThrownBy(() -> RentalRules.period(input)).isInstanceOf(ResponseStatusException.class);
        assertThatCode(() -> RentalRules.period(new Rental.OrderInput(UUID.randomUUID(),start,start.plusDays(1),null))).doesNotThrowAnyException();
    }
    @Test void staleOrMissingRevisionIsRejected() {
        for(Integer revision: new Integer[]{null,1,3}) {
            var error=catchThrowableOfType(ResponseStatusException.class,() -> RentalRules.revision(order("EM_ANALISE"),revision));
            assertThat(error.getStatusCode().value()).isEqualTo(409);
        }
    }
    @ParameterizedTest @ValueSource(strings={"CANCELADO","CONTRATADO"})
    void terminalOrdersCannotChange(String status) { assertThatThrownBy(() -> RentalRules.editable(order(status))).isInstanceOf(ResponseStatusException.class); }
    @ParameterizedTest @ValueSource(strings={"EM_ANALISE","APROVADO","REPROVADO"})
    void activeOrdersCanBeRevised(String status) { assertThatCode(() -> RentalRules.editable(order(status))).doesNotThrowAnyException(); }
    @ParameterizedTest @ValueSource(strings={"APROVADO","REPROVADO","CANCELADO","CONTRATADO"})
    void onlyPendingOrdersCanBeEvaluated(String status) { assertThatThrownBy(() -> RentalRules.review(order(status),new Rental.ReviewInput(2,"FAVORAVEL","Analisado"))).isInstanceOf(ResponseStatusException.class); }
    @Test void validatesReviewResultNotesAndRevision() {
        for(var input:new Rental.ReviewInput[]{new Rental.ReviewInput(1,"FAVORAVEL","Ok"),new Rental.ReviewInput(2,"INVALIDO","Ok"),new Rental.ReviewInput(2,null,"Ok"),new Rental.ReviewInput(2,"FAVORAVEL"," "),new Rental.ReviewInput(2,"FAVORAVEL","x".repeat(2001))})
            assertThatThrownBy(() -> RentalRules.review(order("EM_ANALISE"),input)).isInstanceOf(ResponseStatusException.class);
        for(String result:new String[]{"FAVORAVEL","DESFAVORAVEL"}) assertThatCode(() -> RentalRules.review(order("EM_ANALISE"),new Rental.ReviewInput(2,result,"Analisado"))).doesNotThrowAnyException();
    }
}
