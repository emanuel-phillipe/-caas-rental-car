create schema if not exists caas_private;

create table caas_private.accounts (
    id uuid primary key default gen_random_uuid(),
    username text not null unique check (username ~ '^[a-z0-9][a-z0-9._-]{0,63}$'),
    display_name text not null check (length(btrim(display_name)) > 0),
    password_hash text not null check (password_hash ~ '^\$2[aby]\$(0[4-9]|[12][0-9]|3[01])\$[./A-Za-z0-9]{53}$'),
    role text not null check (role in ('CLIENTE', 'AGENTE')),
    enabled boolean not null default true
);

revoke all on schema caas_private from public, anon, authenticated, service_role;
revoke all on table caas_private.accounts from public, anon, authenticated, service_role;
