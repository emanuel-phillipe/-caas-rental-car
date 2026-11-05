# Implementação do fluxo de locações

## Escopo

HU01–HU10 possuem endpoints e telas: cadastro e autenticação, catálogo/frota, criação/consulta/alteração/cancelamento de pedidos, consulta por organização, avaliação por revisão, formalização e crédito opcional. A página inicial apresenta contadores calculados dos pedidos acessíveis à conta.

Telas: `/`, `/cliente`, `/veiculos`, `/pedidos`, `/pedidos/novo`, `/pedidos/:id`, `/contratos`. Todas reutilizam o template existente (Inter, laranja, fundo claro, cartões e controles Tailwind). Há estados de carregamento, vazio, erro e confirmação para operações de encerramento.

## Ativação

1. Aplique `supabase/migrations/20261105000300_create_rentals.sql` após as duas migrations existentes. O backend não executa SQL de migração automaticamente.
2. Cadastre as organizações e vincule as contas AGENTE existentes como representantes. O provisionamento permanece administrativo, sem cadastro público de agentes. Exemplo para adaptar no SQL Editor:

```sql
-- Substitua pelos nomes e usuários reais. As contas AGENTE já devem existir.
insert into caas_private.organizations (name, kind)
values ('Locadora Exemplo', 'EMPRESA'), ('Banco Exemplo', 'BANCO');

insert into caas_private.representatives (account_id, organization_id)
select a.id, o.id from caas_private.accounts a
cross join caas_private.organizations o
where a.username = 'agente.demo' and a.role = 'AGENTE'
  and o.name = 'Locadora Exemplo';

-- Repita para a conta representante do banco, selecionando Banco Exemplo.
```

3. Entre como agente e cadastre a frota em **Veículos**. O proprietário padrão é a organização. O cadastro também aceita o identificador de um cliente proprietário previamente cadastrado. Organizações podem ser empresas ou bancos.
4. Entre como cliente, complete o cadastro e envie um pedido.
5. Entre como agente da organização gestora para avaliar o pedido. Opcionalmente, use **Atribuir banco** para conceder acesso a um banco cadastrado. Seu representante passa a ver o pedido e pode registrar crédito já concedido.
6. Com parecer favorável da revisão atual, formalize o contrato com ou sem crédito.

Se o papel `caas_app` for criado depois da migration, conceda `usage` no schema e `select, insert, update, delete on all tables in schema caas_private`. O schema deve continuar fora da Data API do Supabase.

## Decisões para as premissas da modelagem

- Cada representante pertence a uma organização. As contas e esse vínculo são provisionados pelo operador.
- Cada veículo tem uma organização gestora. Novos pedidos são atribuídos automaticamente a ela. A gestora pode adicionar bancos ao pedido pela interface; bancos atribuídos não podem delegar o acesso a outras organizações.
- Alterações de veículo em um pedido ficam limitadas à mesma organização gestora. Para mudar de gestora, o cliente cria outro pedido.
- Clientes veem somente seus pedidos e contratos; agentes veem somente os atribuídos à sua organização. Dados pessoais e rendimentos são lidos apenas após validar acesso ao pedido.
- Uma avaliação conclusiva por revisão é suficiente, mesmo havendo vários agentes. A revisão deve ser enviada nas operações de alteração, cancelamento, avaliação e formalização. Revisão desatualizada retorna 409.
- Alterar um pedido incrementa sua revisão e retorna a `EM_ANALISE`. Pareceres antigos permanecem no histórico. Cancelamento e contratação encerram o pedido.
- A disponibilidade é verificada ao criar/alterar e novamente na formalização. Só contratos formalizados reservam o veículo. Intervalos são `[retirada, devolução)`: a devolução de um aluguel pode coincidir com a retirada de outro. A formalização serializa operações por veículo para impedir contratos sobrepostos.
- Tipos implementados: `LOCACAO_CLIENTE`, `LOCACAO_EMPRESA`, `LOCACAO_BANCO`, conforme o proprietário registrado no momento da formalização. Não há transferência de propriedade. O contrato salva ID, nome e tipo do proprietário como fotografia histórica.
- Crédito é um registro de concessão confirmada pelo representante do banco, com referência e valor. Não há integração externa para conceder crédito ou movimentar dinheiro. Somente banco atribuído registra crédito; o cliente do crédito deve coincidir com o do pedido; cada crédito pode aparecer em um único aluguel.
- A exclusão de cliente com pedidos é bloqueada pelo banco; veículos são inativados, preservando referências históricas.

Essas decisões concretizam as premissas abertas em `docs/README.md`; não incluem pagamentos, assinatura eletrônica, entrega/devolução operacional nem transferência patrimonial, que não foram especificados nas histórias.

## Contrato HTTP

Todas as rotas abaixo exigem sessão. Escritas exigem CSRF como no módulo de clientes.

| Método | Rota | Função |
| --- | --- | --- |
| GET | `/api/organization` | Organização do representante |
| GET | `/api/banks` | Bancos disponíveis para atribuição (agente) |
| GET | `/api/vehicles` | Catálogo ativo (cliente) ou frota própria (agente) |
| POST / PUT | `/api/vehicles` / `/api/vehicles/{id}` | Cadastrar/editar veículo da organização |
| GET / POST | `/api/orders` | Listar pedidos acessíveis / criar pedido de cliente |
| GET / PUT | `/api/orders/{id}` | Detalhar / alterar pedido autorizado |
| POST | `/api/orders/{id}/cancel` | Cancelar pedido próprio |
| POST | `/api/orders/{id}/reviews` | Registrar parecer por revisão |
| POST | `/api/orders/{id}/bank` | Gestora atribui banco ao pedido |
| POST | `/api/orders/{id}/contract` | Formalizar contrato com crédito opcional |
| POST | `/api/credits` | Banco registra crédito concedido ao cliente do pedido |
| GET | `/api/contracts` | Listar contratos acessíveis |

Exemplos de corpos:

```json
{"vehicleId":"UUID","startsOn":"2026-12-01","endsOn":"2026-12-05"}
```

Alteração usa os mesmos campos mais `revision`. Cancelamento recebe `{"revision":1}`; parecer recebe `{"revision":1,"result":"FAVORAVEL","notes":"Justificativa"}`; formalização recebe `{"revision":1,"kind":"LOCACAO_EMPRESA","creditId":null}`; atribuição recebe `{"bankId":"UUID"}`; crédito recebe `{"orderId":"UUID","reference":"CRED-001","amount":1500.00}`.

Erros: 400 para dados inválidos, 401 sem sessão, 403 sem permissão, 404 pedido inexistente, 409 conflito de estado/revisão/disponibilidade/duplicidade e 503 para falha no banco. Detalhes internos do JDBC não são enviados ao navegador.

## Validação

```bash
cd CaasAPI
./mvnw test
```

```bash
cd caas-web-app
pnpm exec tsc --noEmit
pnpm build
```

Os testes automatizados usam persistência simulada: verificam regras de revisão e estado, autorização/CSRF, isolamento por cliente/organização, rollback e sanitização de erros. Não substituem um ensaio com PostgreSQL real.

Roteiro de aceitação após aplicar a migration:

1. Cadastre cliente, empresa, banco, representantes e dois veículos.
2. Crie um pedido como cliente. Verifique que outro cliente e agente não atribuído não o acessam por ID.
3. Aprove como agente, altere como cliente e confira a nova revisão em análise e o parecer anterior no histórico.
4. Tente avaliar com a revisão anterior: deve retornar 409.
5. Atribua banco, registre crédito como representante do banco e aprove a revisão atual.
6. Formalize com esse crédito; confira contrato, proprietário histórico e estado contratado. Repetir formalização deve falhar.
7. Tente reutilizar o crédito em outro pedido e formalizar períodos sobrepostos do mesmo veículo: ambos devem falhar.
8. Cancele outro pedido, confira sua permanência no histórico e a recusa de novas alterações.
9. Tente excluir cliente com histórico: deve retornar 409.
