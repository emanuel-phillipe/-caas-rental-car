package com.emanuel.CaasAPI.rental;

import com.emanuel.CaasAPI.auth.AccountPrincipal;
import com.emanuel.CaasAPI.client.Client;
import com.emanuel.CaasAPI.database.SupabaseConnection;
import java.sql.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import static com.emanuel.CaasAPI.rental.Rental.*;
import static com.emanuel.CaasAPI.rental.RentalRules.*;

@Repository
public class RentalRepository {
    private final SupabaseConnection database;
    public RentalRepository(SupabaseConnection database) { this.database = database; }
    private static final String VEHICLES = """
        select v.*, coalesce(v.owner_client_id,v.owner_organization_id) owner_id,
        coalesce(a.display_name,g.name) owner_name, coalesce(g.kind,'CLIENTE') owner_kind
        from caas_private.vehicles v
        left join caas_private.accounts a on a.id=v.owner_client_id
        left join caas_private.organizations g on g.id=v.owner_organization_id
        """;
    private static final String ORDERS = """
        select o.*, a.display_name client_name, v.brand || ' ' || v.model vehicle_name, v.plate
        from caas_private.rental_orders o join caas_private.accounts a on a.id=o.client_id
        join caas_private.vehicles v on v.id=o.vehicle_id
        """;
    private static final String CREDITS = """
        select c.*, g.name bank_name, exists(select 1 from caas_private.rental_contracts r where r.credit_id=c.id) linked
        from caas_private.credit_contracts c join caas_private.organizations g on g.id=c.bank_id
        """;
    public static boolean isAgent(AccountPrincipal actor) {
        return actor.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_AGENTE"));
    }
    private Organization organization(Connection c, AccountPrincipal actor) throws SQLException {
        if (!isAgent(actor)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação exclusiva de representantes.");
        return one(c, "select g.* from caas_private.organizations g join caas_private.representatives r on r.organization_id=g.id where r.account_id=?",
                r -> new Organization(uuid(r,"id"), r.getString("name"),r.getString("kind")), actor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"Conta sem organização vinculada. Solicite o vínculo ao operador."));
    }
    public Organization organization(AccountPrincipal actor) { return tx(c -> organization(c,actor)); }
    public List<Vehicle> vehicles(AccountPrincipal actor) {
        return tx(c -> isAgent(actor)
            ? query(c,VEHICLES+" where v.managing_organization_id=? order by v.brand,v.model",this::vehicle,organization(c,actor).id())
            : query(c,VEHICLES+" where v.active order by v.brand,v.model",this::vehicle));
    }
    public Vehicle saveVehicle(AccountPrincipal actor, UUID id, VehicleInput input) {
        String registration=text(input.registration(),"Matrícula",40), brand=text(input.brand(),"Marca",80), model=text(input.model(),"Modelo",100);
        String plate=text(input.plate(),"Placa",10).toUpperCase(Locale.ROOT);
        if (input.year()<1900 || input.year()>2200) throw invalid("Ano inválido.");
        return tx(c -> {
            Organization org=organization(c,actor);
            UUID ownerOrg=input.ownerClientId()==null?org.id():null;
            if (input.ownerClientId()!=null && one(c,"select account_id from caas_private.clients where account_id=?",r -> r.getObject(1),input.ownerClientId()).isEmpty())
                throw invalid("Proprietário cliente não encontrado.");
            UUID vehicleId=id==null?UUID.randomUUID():id;
            if(id==null) execute(c,"insert into caas_private.vehicles (id,registration,year,brand,model,plate,owner_client_id,owner_organization_id,managing_organization_id,active) values (?,?,?,?,?,?,?,?,?,?)",
                vehicleId,registration,input.year(),brand,model,plate,input.ownerClientId(),ownerOrg,org.id(),input.active());
            else {
                Vehicle old=vehicle(c,id,true);
                if(!old.organizationId().equals(org.id())) throw forbidden();
                execute(c,"update caas_private.vehicles set registration=?,year=?,brand=?,model=?,plate=?,owner_client_id=?,owner_organization_id=?,active=? where id=?",
                    registration,input.year(),brand,model,plate,input.ownerClientId(),ownerOrg,input.active(),id);
            }
            return vehicle(c,vehicleId,false);
        });
    }
    public List<Order> orders(AccountPrincipal actor) {
        return tx(c -> {
            if(isAgent(actor)) return query(c,ORDERS+" where exists(select 1 from caas_private.order_assignments x where x.order_id=o.id and x.organization_id=?) order by o.created_at desc",this::order,organization(c,actor).id());
            return query(c,ORDERS+" where o.client_id=? order by o.created_at desc",this::order,actor.getId());
        });
    }
    private Order authorized(Connection c,AccountPrincipal actor,UUID id,boolean lock) throws SQLException {
        Order order=one(c,ORDERS+" where o.id=?"+(lock?" for update of o":""),this::order,id).orElseThrow(this::missing);
        if(isAgent(actor)) {
            UUID org=organization(c,actor).id();
            if(one(c,"select order_id from caas_private.order_assignments where order_id=? and organization_id=?",r -> r.getObject(1),id,org).isEmpty()) throw forbidden();
        } else if(!order.clientId().equals(actor.getId())) throw forbidden();
        return order;
    }
    public Detail detail(AccountPrincipal actor,UUID id) {
        return tx(c -> {
            Order order=authorized(c,actor,id,false);
            Client client=one(c,"select a.id,a.username,a.display_name,c.* from caas_private.clients c join caas_private.accounts a on a.id=c.account_id where a.id=?",
                r -> new Client(uuid(r,"id"),r.getString("username"),r.getString("display_name"),r.getString("rg"),r.getString("cpf"),r.getString("address"),r.getString("profession"),
                    query(c,"select employer,income from caas_private.client_employments where client_id=? order by position",j -> new Client.Employment(j.getString(1),j.getBigDecimal(2)),order.clientId())),order.clientId()).orElseThrow(this::missing);
            var reviews=query(c,"select f.*,a.display_name representative from caas_private.financial_reviews f join caas_private.accounts a on a.id=f.representative_id where order_id=? order by revision desc",
                r -> new Review(uuid(r,"id"),r.getInt("revision"),r.getString("representative"),r.getString("result"),r.getString("notes"),r.getTimestamp("created_at").toInstant()),id);
            return new Detail(order,vehicle(c,order.vehicleId(),false),client,reviews,
                one(c,"select * from caas_private.rental_contracts where order_id=?",this::contract,id).orElse(null),
                query(c,CREDITS+" where c.client_id=? and exists(select 1 from caas_private.order_assignments x where x.order_id=? and x.organization_id=c.bank_id) order by c.created_at desc",this::credit,order.clientId(),id),
                query(c,"select g.* from caas_private.organizations g join caas_private.order_assignments x on x.organization_id=g.id where x.order_id=? order by g.name",r -> new Organization(uuid(r,"id"),r.getString("name"),r.getString("kind")),id));
        });
    }
    public Order create(AccountPrincipal actor,OrderInput input) {
        if(isAgent(actor)) throw forbidden();
        period(input);
        return tx(c -> {
            Vehicle vehicle=vehicle(c,input.vehicleId(),true);
            if(!vehicle.active()) throw invalid("Veículo indisponível para novos pedidos.");
            available(c,vehicle.id(),input.startsOn(),input.endsOn(),null);
            UUID id=UUID.randomUUID();
            execute(c,"insert into caas_private.rental_orders (id,client_id,vehicle_id,starts_on,ends_on) values (?,?,?,?,?)",id,actor.getId(),vehicle.id(),input.startsOn(),input.endsOn());
            execute(c,"insert into caas_private.order_assignments (order_id,organization_id) values (?,?)",id,vehicle.organizationId());
            return authorized(c,actor,id,false);
        });
    }
    public Order update(AccountPrincipal actor,UUID id,OrderInput input) {
        period(input);
        return tx(c -> {
            Order old=authorized(c,actor,id,true); revision(old,input.revision()); editable(old);
            // Vehicle identity cannot change the organization responsible for an existing order.
            Vehicle original=vehicle(c,old.vehicleId(),false), selected=vehicle(c,input.vehicleId(),true);
            if(!selected.organizationId().equals(original.organizationId())) throw invalid("Escolha um veículo da mesma organização ou crie outro pedido.");
            if(!selected.active()) throw invalid("Veículo indisponível.");
            available(c,selected.id(),input.startsOn(),input.endsOn(),id);
            execute(c,"update caas_private.rental_orders set vehicle_id=?,starts_on=?,ends_on=?,revision=revision+1,status='EM_ANALISE' where id=?",selected.id(),input.startsOn(),input.endsOn(),id);
            return authorized(c,actor,id,false);
        });
    }
    public Order cancel(AccountPrincipal actor,UUID id,Revision input) {
        if(isAgent(actor)) throw forbidden();
        return tx(c -> {
            Order order=authorized(c,actor,id,true); revision(order,input.revision()); editable(order);
            execute(c,"update caas_private.rental_orders set status='CANCELADO' where id=?",id);
            return authorized(c,actor,id,false);
        });
    }
    public Order evaluate(AccountPrincipal actor,UUID id,ReviewInput input) {
        return tx(c -> {
            organization(c,actor);
            Order order=authorized(c,actor,id,true); review(order,input);
            execute(c,"insert into caas_private.financial_reviews (order_id,revision,representative_id,result,notes) values (?,?,?,?,?)",id,order.revision(),actor.getId(),input.result(),input.notes().strip());
            execute(c,"update caas_private.rental_orders set status=? where id=?",input.result().equals("FAVORAVEL")?"APROVADO":"REPROVADO",id);
            return authorized(c,actor,id,false);
        });
    }
    public Contract formalize(AccountPrincipal actor,UUID id,ContractInput input) {
        return tx(c -> {
            organization(c,actor);
            Order order=authorized(c,actor,id,true); revision(order,input.revision());
            if(!order.status().equals("APROVADO") || one(c,"select id from caas_private.financial_reviews where order_id=? and revision=? and result='FAVORAVEL'",r -> r.getObject(1),id,order.revision()).isEmpty())
                throw conflict("A formalização exige parecer favorável para a revisão atual.");
            Vehicle vehicle=vehicle(c,order.vehicleId(),true);
            if(!vehicle.active()) throw conflict("Veículo inativo. Atualize o pedido para continuar.");
            if(!("LOCACAO_"+vehicle.ownerKind()).equals(input.kind())) throw invalid("Tipo de contrato incompatível com o proprietário registrado.");
            available(c,vehicle.id(),order.startsOn(),order.endsOn(),id);
            if(input.creditId()!=null) {
                Credit credit=one(c,CREDITS+" where c.id=? for update of c",this::credit,input.creditId()).orElseThrow(() -> invalid("Crédito inexistente."));
                if(credit.linked() || !credit.clientId().equals(order.clientId())) throw conflict("Crédito já utilizado ou pertencente a outro cliente.");
                if(one(c,"select order_id from caas_private.order_assignments where order_id=? and organization_id=?",r -> r.getObject(1),id,credit.bankId()).isEmpty()) throw invalid("O banco do crédito não está atribuído ao pedido.");
            }
            UUID contractId=UUID.randomUUID();
            execute(c,"insert into caas_private.rental_contracts (id,order_id,representative_id,kind,owner_id,owner_name,owner_kind,credit_id) values (?,?,?,?,?,?,?,?)",
                contractId,id,actor.getId(),input.kind(),vehicle.ownerId(),vehicle.ownerName(),vehicle.ownerKind(),input.creditId());
            execute(c,"update caas_private.rental_orders set status='CONTRATADO' where id=?",id);
            return one(c,"select * from caas_private.rental_contracts where id=?",this::contract,contractId).orElseThrow();
        });
    }
    public Credit createCredit(AccountPrincipal actor,CreditInput input) {
        String reference=text(input.reference(),"Referência do crédito",100);
        if(input.orderId()==null || input.amount()==null || input.amount().signum()<=0 || input.amount().scale()>2 || input.amount().compareTo(new java.math.BigDecimal("9999999999.99"))>0) throw invalid("Informe o pedido e um valor positivo com até duas casas decimais.");
        return tx(c -> {
            Organization bank=organization(c,actor);
            if(!bank.kind().equals("BANCO")) throw forbidden();
            Order order=authorized(c,actor,input.orderId(),true); editable(order);
            UUID id=UUID.randomUUID();
            execute(c,"insert into caas_private.credit_contracts (id,reference,bank_id,client_id,amount) values (?,?,?,?,?)",id,reference,bank.id(),order.clientId(),input.amount());
            return one(c,CREDITS+" where c.id=?",this::credit,id).orElseThrow();
        });
    }
    public List<Organization> banks(AccountPrincipal actor) {
        return tx(c -> { organization(c,actor); return query(c,"select * from caas_private.organizations where kind='BANCO' order by name",r -> new Organization(uuid(r,"id"),r.getString("name"),r.getString("kind"))); });
    }
    public void assignBank(AccountPrincipal actor,UUID id,BankInput input) {
        tx(c -> {
            Organization org=organization(c,actor);
            Order order=authorized(c,actor,id,true); editable(order);
            if(!vehicle(c,order.vehicleId(),false).organizationId().equals(org.id())) throw forbidden();
            if(input.bankId()==null || one(c,"select id from caas_private.organizations where id=? and kind='BANCO'",r -> r.getObject(1),input.bankId()).isEmpty()) throw invalid("Selecione um banco válido.");
            execute(c,"insert into caas_private.order_assignments (order_id,organization_id) values (?,?) on conflict do nothing",id,input.bankId());
            return null;
        });
    }
    public List<Contract> contracts(AccountPrincipal actor) {
        return tx(c -> {
            if(isAgent(actor)) return query(c,"select r.* from caas_private.rental_contracts r where exists(select 1 from caas_private.order_assignments x where x.order_id=r.order_id and x.organization_id=?) order by r.created_at desc",this::contract,organization(c,actor).id());
            return query(c,"select r.* from caas_private.rental_contracts r join caas_private.rental_orders o on o.id=r.order_id where o.client_id=? order by r.created_at desc",this::contract,actor.getId());
        });
    }
    private void available(Connection c,UUID vehicle,java.time.LocalDate start,java.time.LocalDate end,UUID excluding) throws SQLException {
        if(!query(c,"select id from caas_private.rental_orders where vehicle_id=? and status='CONTRATADO' and starts_on<? and ends_on>? and (?::uuid is null or id<>?)",r -> r.getObject(1),vehicle,end,start,excluding,excluding).isEmpty())
            throw conflict("Veículo já contratado neste período. Escolha outras datas.");
    }
    private Vehicle vehicle(Connection c,UUID id,boolean lock) throws SQLException {
        return one(c,VEHICLES+" where v.id=?"+(lock?" for update of v":""),this::vehicle,id).orElseThrow(() -> invalid("Veículo não encontrado."));
    }
    private Vehicle vehicle(ResultSet r) throws SQLException { return new Vehicle(uuid(r,"id"),r.getString("registration"),r.getInt("year"),r.getString("brand"),r.getString("model"),r.getString("plate"),uuid(r,"owner_id"),r.getString("owner_name"),r.getString("owner_kind"),uuid(r,"managing_organization_id"),r.getBoolean("active")); }
    private Order order(ResultSet r) throws SQLException { return new Order(uuid(r,"id"),uuid(r,"client_id"),r.getString("client_name"),uuid(r,"vehicle_id"),r.getString("vehicle_name"),r.getString("plate"),r.getDate("starts_on").toLocalDate(),r.getDate("ends_on").toLocalDate(),r.getInt("revision"),r.getString("status"),r.getTimestamp("created_at").toInstant()); }
    private Contract contract(ResultSet r) throws SQLException { return new Contract(uuid(r,"id"),uuid(r,"order_id"),r.getString("kind"),uuid(r,"owner_id"),r.getString("owner_name"),r.getString("owner_kind"),uuid(r,"credit_id"),r.getTimestamp("created_at").toInstant()); }
    private Credit credit(ResultSet r) throws SQLException { return new Credit(uuid(r,"id"),r.getString("reference"),uuid(r,"bank_id"),r.getString("bank_name"),uuid(r,"client_id"),r.getBigDecimal("amount"),r.getBoolean("linked")); }
    private UUID uuid(ResultSet r,String field) throws SQLException { return r.getObject(field,UUID.class); }
    private ResponseStatusException forbidden() { return new ResponseStatusException(HttpStatus.FORBIDDEN,"Você não tem permissão para acessar este registro."); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Pedido não encontrado."); }
    private void execute(Connection c,String sql,Object... args) throws SQLException { try(var s=c.prepareStatement(sql)) { bind(s,args); s.executeUpdate(); } }
    private <T> List<T> query(Connection c,String sql,Mapper<T> mapper,Object... args) throws SQLException {
        try(var s=c.prepareStatement(sql)) { bind(s,args); try(var r=s.executeQuery()) { var items=new ArrayList<T>(); while(r.next()) items.add(mapper.read(r)); return items; } }
    }
    private <T> Optional<T> one(Connection c,String sql,Mapper<T> mapper,Object... args) throws SQLException { return query(c,sql,mapper,args).stream().findFirst(); }
    private void bind(PreparedStatement s,Object[] args) throws SQLException { for(int i=0;i<args.length;i++) s.setObject(i+1,args[i]); }
    private <T> T tx(Operation<T> operation) {
        try(var c=database.open()) {
            c.setAutoCommit(false);
            try { T value=operation.run(c); c.commit(); return value; }
            catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) {
            if("23505".equals(e.getSQLState())) throw conflict("Registro duplicado ou operação já concluída. Atualize a página.");
            if("23503".equals(e.getSQLState()) || "23514".equals(e.getSQLState())) throw invalid("Dados vinculados inválidos. Verifique o cadastro e tente novamente.");
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha no módulo de locações. SQLState={}",e.getSQLState());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Não foi possível acessar as locações. Confira a conexão e a migração do módulo.");
        }
    }
    @FunctionalInterface private interface Operation<T> { T run(Connection c) throws SQLException; }
    @FunctionalInterface private interface Mapper<T> { T read(ResultSet r) throws SQLException; }
}
