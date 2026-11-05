-- Execute após 20261105000100_create_accounts.sql.
begin;
create table caas_private.clients (
    account_id uuid primary key references caas_private.accounts(id) on delete cascade,
    rg varchar(30) not null check (length(btrim(rg)) > 0),
    cpf varchar(11) not null unique check (cpf ~ '^[0-9]{11}$'),
    address varchar(300) not null check (length(btrim(address)) > 0),
    profession varchar(120) not null check (length(btrim(profession)) > 0)
);

create table caas_private.client_employments (
    client_id uuid not null references caas_private.clients(account_id) on delete cascade,
    position smallint not null check (position between 1 and 3),
    employer varchar(160) not null check (length(btrim(employer)) > 0),
    income numeric(12,2) not null check (income >= 0),
    primary key (client_id, position)
);

revoke all on table caas_private.clients, caas_private.client_employments from public, anon, authenticated, service_role;
-- Se caas_app já foi provisionado, atualiza as permissões do backend.
do $$ begin
    if exists (select 1 from pg_roles where rolname = 'caas_app') then
        grant usage on schema caas_private to caas_app;
        grant select, insert, update, delete on caas_private.accounts, caas_private.clients,
            caas_private.client_employments to caas_app;
    end if;
end $$;
commit;
