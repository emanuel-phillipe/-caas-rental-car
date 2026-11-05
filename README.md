# Caas Car Rental

Portal de locações com frontend React/Vite e API Spring Boot. Clientes e agentes usam a mesma tela de login. O backend consulta o perfil da conta no PostgreSQL do Supabase, verifica a senha com BCrypt e mantém a sessão por cookie.

## 1. Preparar o Supabase

1. Execute, nesta ordem, a [migração de contas](supabase/migrations/20261105000100_create_accounts.sql), a [migração de clientes](supabase/migrations/20261105000200_create_clients.sql) e a [migração de locações](supabase/migrations/20261105000300_create_rentals.sql) no projeto Supabase. Use o fluxo de migrations da CLI ou aplique o SQL pelo SQL Editor.
2. Em uma sessão administrativa do `psql`, crie uma credencial para o backend e defina a senha com o prompt do `psql`.

   ```sql
   create role caas_app login;
   grant usage on schema caas_private to caas_app;
   grant select, insert, update, delete on table caas_private.accounts,
       caas_private.clients, caas_private.client_employments to caas_app;
   \password caas_app
   ```

3. No painel **Connect**, selecione **Session pooler**, porta **5432**, e copie a URL JDBC. Configure as variáveis no mesmo terminal (ou configuração de execução da IDE) que inicia o backend:

   ```bash
   export SUPABASE_DB_URL='jdbc:postgresql://HOST-DO-POOLER:5432/postgres?sslmode=require'
   export SUPABASE_DB_USER='caas_app.REFERENCIA-DO-PROJETO'
   read -rsp 'Senha da credencial do banco: ' SUPABASE_DB_PASSWORD; echo
   export SUPABASE_DB_PASSWORD
   ```

   Use o host exato do painel. Para a credencial padrão, o usuário é `postgres.REFERENCIA-DO-PROJETO`; para o papel criado acima, use `caas_app.REFERENCIA-DO-PROJETO`. Também é aceita a URL completa do guia, com `user` e `password` na query; nesse caso, percent-encode caracteres reservados da senha. Prefira a senha na variável separada para dispensar essa codificação.

   `SUPABASE_JDBC_URL` continua aceito como alternativa; `SUPABASE_DB_URL` tem prioridade. A aplicação exige uma URL configurada, sem fallback silencioso para localhost. O Spring Boot gerencia um pool Hikari de até cinco conexões, compartilhado pelo login e pelo CRUD.

   SSL usa `require`, conforme o quickstart: criptografa a conexão sem exigir um arquivo de certificado local. Para também validar certificado e hostname, configure `sslmode=verify-full&sslrootcert=/caminho/absoluto/supabase.cer` na URL e forneça um certificado existente e válido. Parâmetros explícitos da URL prevalecem sobre as propriedades do driver. Não use `sslmode=disable` ou `prefer`.

   Conexão direta exige IPv6 ou o adicional IPv4 do Supabase; use o usuário sem o sufixo do projeto. Para este backend persistente, prefira o Session pooler a Transaction pooler (6543).

   O projeto utiliza JDBC com SQL explícito, não JPA/Hibernate. Por isso não precisa de `ddl-auto` nem de entidades JPA: as migrations criam as tabelas em `caas_private`, e as consultas usam esse schema explicitamente. Mantenha-o fora dos schemas expostos pela Data API.

   Referência: [guia oficial Spring Boot + Supabase](https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot).

## 2. Criar as contas iniciais

Clientes criam a conta pela tela `/cadastro`. O operador provisiona contas de agente no banco. O exemplo abaixo também permite criar uma conta de cliente apenas para testar o login; contas legadas sem registro em `clients` precisam ter seus dados completos provisionados antes de usar `/cliente`. Gere um hash para cada conta com `htpasswd` (pacote `apache2-utils` em Debian/Ubuntu). O comando pede a senha de forma interativa. Use senhas de até 72 bytes em UTF-8.

```bash
htpasswd -nBC 12 cliente.demo
htpasswd -nBC 12 agente.demo
```

No SQL Editor, insira a parte que vem após os dois-pontos de cada saída.

```sql
insert into caas_private.accounts (username, display_name, password_hash, role)
values
    ('cliente.demo', 'Cliente de teste', '<hash BCrypt do cliente>', 'CLIENTE'),
    ('agente.demo', 'Agente de teste', '<hash BCrypt do agente>', 'AGENTE');
```

A conta é única por nome de usuário. O login normaliza maiúsculas e espaços externos. Definir `enabled = false` impede novos logins para a conta. [Documentação do htpasswd](https://httpd.apache.org/docs/2.4/en/programs/htpasswd.html)

## 3. Executar

Em terminais separados, na raiz do projeto:

```bash
cd CaasAPI
./mvnw spring-boot:run
```

```bash
cd caas-web-app
pnpm install --frozen-lockfile
pnpm dev
```

Abra [http://localhost:3000](http://localhost:3000). O Vite encaminha `/api` para o Spring Boot na porta 8080. Em produção, sirva o build do Vite e a API pelo mesmo domínio HTTPS. Configure fallback para `index.html` nas rotas do frontend e `SESSION_COOKIE_SECURE=true` no backend.

## Endpoints de autenticação

| Método | Rota | Comportamento |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Obtém o token e o nome do cabeçalho CSRF. |
| POST | `/api/auth/login` | Recebe `username` e `password` como formulário e inicia a sessão. |
| GET | `/api/auth/session` | Retorna os dados e o perfil da conta autenticada. |
| POST | `/api/auth/logout` | Encerra a sessão. |

Login e logout exigem o cabeçalho CSRF. O cookie de sessão é `HttpOnly` e a sessão expira após 30 minutos de inatividade. As demais rotas da API exigem uma sessão com perfil de cliente ou agente.

Os testes de autenticação podem ser executados com `./mvnw test` em `CaasAPI`. Eles usam contas simuladas para verificar as regras do Spring Security. No frontend, `pnpm build` valida a compilação de produção.


## CRUD de cliente (MVC)

O fluxo segue HU01 e as regras de acesso da modelagem: cada cliente consulta, altera e exclui **seu próprio cadastro**. Não há listagem global de dados pessoais nem gerenciamento de clientes por agentes sem atribuição de acesso.

- **Model:** `Client`, `ClientService` (validação e regras) e `ClientRepository` (persistência JDBC no PostgreSQL do Supabase).
- **Controller:** `ClientController`, endpoints REST Spring MVC.
- **View:** React + Vite, com `/cadastro` para criar a conta e `/cliente` para consultar, editar e excluir. O login e a página inicial contêm links para essas telas.

| Método | Rota | Acesso e resultado |
| --- | --- | --- |
| POST | `/api/clients` | Cadastro público com CSRF; cria conta CLIENTE e dados pessoais; 201. |
| GET | `/api/clients/me` | Cliente autenticado consulta o próprio cadastro; 200. |
| PUT | `/api/clients/me` | Cliente autenticado substitui seus dados e vínculos; 200. |
| DELETE | `/api/clients/me` | Exclui conta, cadastro e vínculos; encerra a sessão atual; 204. |

O POST recebe:

```json
{
  "username": "cliente.demo",
  "password": "uma-senha-forte",
  "details": {
    "name": "Cliente de demonstração",
    "rg": "1234567",
    "cpf": "52998224725",
    "address": "Rua Exemplo, 100",
    "profession": "Analista",
    "employments": [{ "employer": "Empresa Exemplo", "income": 2500.50 }]
  }
}
```

O PUT recebe somente o objeto `details` acima. CPF pode vir com pontuação e é armazenado com 11 dígitos após validar os verificadores. Nome, RG, CPF, endereço e profissão são obrigatórios. Aceita de zero a três vínculos, com rendimentos não negativos e até duas casas decimais. Usuário e senha não são alterados pelo PUT. A senha do cadastro usa BCrypt e precisa ter pelo menos 8 caracteres e até 72 bytes em UTF-8.

Erros de validação retornam 400; usuário ou CPF duplicado, 409; cadastro inexistente, 404; indisponibilidade do banco, 503. Mensagens de negócio usam `{ "message": "..." }`. Requisições sem sessão retornam 401 e acesso de agentes ao cadastro pessoal retorna 403. Todas as escritas exigem o token obtido em `/api/auth/csrf`. IDs fornecidos no corpo não escolhem o cliente: a API usa exclusivamente o UUID da sessão.

Conta, cadastro e vínculos são gravados em uma única transação. A exclusão exige confirmação na interface e remove os dados por cascata. As tabelas de pedidos referenciam `clients(account_id)` com `ON DELETE RESTRICT` para impedir apagar clientes com histórico contratual. As tabelas ficam no schema privado e não são acessadas diretamente pelo navegador.

A API lê `SUPABASE_DB_URL` (ou `SUPABASE_JDBC_URL`), `SUPABASE_DB_USER` e `SUPABASE_DB_PASSWORD`; configure-as antes de iniciar. Nenhuma senha do banco deve ser colocada no código ou no frontend.

### Validação

```bash
cd CaasAPI
./mvnw test
```

```bash
cd caas-web-app
pnpm build
pnpm exec tsc --noEmit
```

Os testes MVC cobrem cadastro, hash da senha, normalização, consulta/edição vinculadas à sessão, exclusão, CSRF, acesso por perfil, CPF inválido, campos obrigatórios, limites dos vínculos, rendimentos e erros. A persistência real exige aplicar as três migrations no Supabase e configurar a conexão. Para verificar o fluxo completo, crie uma conta em `/cadastro`, entre, abra **Consultar meu cadastro**, edite os dados, recarregue a página e confirme a exclusão.


### Diagnóstico de falha no cadastro

- `GET /api/auth/session` com **401 antes do login é esperado**: o frontend interpreta isso como ausência de sessão. Não impede o cadastro público.
- Na aba Network/Rede do navegador, consulte o **status e o corpo da resposta de `POST /api/clients`**. Uma linha `Failed to load resource` sem status não identifica a causa.
- Para **503**, consulte o terminal do Spring Boot: `Falha no banco durante CRUD de cliente` informa SQLState e uma orientação, sem expor credenciais. `42P01`/`3F000` indicam migrations pendentes; `42501`, permissões; `28P01`/`28000`, credenciais; `08001`/`08006`, conexão ou TLS.
- Configure as variáveis `SUPABASE_DB_URL` (ou `SUPABASE_JDBC_URL`), `SUPABASE_DB_USER` e `SUPABASE_DB_PASSWORD` **no processo que inicia o Spring Boot**. Se usar a IDE, configure-as na execução da aplicação; exportá-las em outro terminal não modifica o processo da IDE. Reinicie a API após a configuração.
- Sem a URL no ambiente, o backend não inicia. `FileNotFoundException` em TLS indica verificar o caminho de `sslrootcert` se estiver usando `verify-full`; o modo `require` do quickstart não exige esse arquivo.
- Para falha de rede ou **502**, verifique se a API iniciou na porta 8080 e se o Vite consegue encaminhar `/api`. Para **403** no cadastro, confira a obtenção do CSRF e o envio do cookie de sessão na mesma origem.


## Pedidos, veículos, pareceres e contratos

O fluxo de locações foi implementado conforme HU03–HU10, com painel, frota/catálogo, pedidos, pareceres por revisão, contratos e crédito opcional. Consulte [implementação e ativação das locações](docs/implementacao-locacoes.md) para provisionar organizações, vincular agentes, aplicar a terceira migration e validar o fluxo completo.
