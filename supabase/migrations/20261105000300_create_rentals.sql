begin;
create table caas_private.organizations (
 id uuid primary key default gen_random_uuid(), name varchar(160) not null,
 kind text not null check (kind in ('EMPRESA','BANCO'))
);
create table caas_private.representatives (
 account_id uuid primary key references caas_private.accounts(id),
 organization_id uuid not null references caas_private.organizations(id)
);
create table caas_private.vehicles (
 id uuid primary key default gen_random_uuid(), registration varchar(40) not null unique,
 year integer not null check (year between 1900 and 2200), brand varchar(80) not null,
 model varchar(100) not null, plate varchar(10) not null unique,
 owner_client_id uuid references caas_private.clients(account_id),
 owner_organization_id uuid references caas_private.organizations(id),
 managing_organization_id uuid not null references caas_private.organizations(id),
 active boolean not null default true,
 check (num_nonnulls(owner_client_id, owner_organization_id) = 1)
);
create table caas_private.rental_orders (
 id uuid primary key default gen_random_uuid(), client_id uuid not null references caas_private.clients(account_id) on delete restrict,
 vehicle_id uuid not null references caas_private.vehicles(id), starts_on date not null, ends_on date not null,
 revision integer not null default 1 check (revision > 0),
 status text not null default 'EM_ANALISE' check (status in ('EM_ANALISE','APROVADO','REPROVADO','CANCELADO','CONTRATADO')),
 created_at timestamptz not null default now(), check (ends_on > starts_on)
);
create index rental_orders_client_idx on caas_private.rental_orders(client_id);
create table caas_private.order_assignments (
 order_id uuid not null references caas_private.rental_orders(id),
 organization_id uuid not null references caas_private.organizations(id), primary key(order_id, organization_id)
);
create index order_assignments_org_idx on caas_private.order_assignments(organization_id);
create table caas_private.financial_reviews (
 id uuid primary key default gen_random_uuid(), order_id uuid not null references caas_private.rental_orders(id),
 revision integer not null, representative_id uuid not null references caas_private.representatives(account_id),
 result text not null check (result in ('FAVORAVEL','DESFAVORAVEL')),
 notes varchar(2000) not null, created_at timestamptz not null default now(), unique(order_id, revision)
);
create table caas_private.credit_contracts (
 id uuid primary key default gen_random_uuid(), reference varchar(100) not null,
 bank_id uuid not null references caas_private.organizations(id), client_id uuid not null references caas_private.clients(account_id),
 amount numeric(12,2) not null check(amount > 0), created_at timestamptz not null default now(), unique(bank_id, reference)
);
create table caas_private.rental_contracts (
 id uuid primary key default gen_random_uuid(), order_id uuid not null unique references caas_private.rental_orders(id),
 representative_id uuid not null references caas_private.representatives(account_id),
 kind text not null check(kind in ('LOCACAO_CLIENTE','LOCACAO_EMPRESA','LOCACAO_BANCO')),
 owner_id uuid not null, owner_name varchar(160) not null, owner_kind text not null,
 credit_id uuid unique references caas_private.credit_contracts(id), created_at timestamptz not null default now()
);
revoke all on all tables in schema caas_private from public, anon, authenticated, service_role;
do $$ begin
 if exists (select 1 from pg_roles where rolname = 'caas_app') then
  grant select, insert, update, delete on all tables in schema caas_private to caas_app;
 end if;
end $$;
commit;
