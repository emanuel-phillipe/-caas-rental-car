# Caas Car Rental — Histórias do usuário

As referências UC apontam para o [diagrama de casos de uso](casos-de-uso.puml). As referências RN apontam para as [regras e transições do pedido](README.md).

## Acesso e cadastro

### HU01 · Cadastrar-me como cliente

Como visitante, quero criar minha conta e informar meus dados de contratante para poder solicitar aluguéis.

**Critérios de aceitação** · UC01 · RN01, RN04

- O cadastro registra um nome de usuário, a senha protegida por hash, RG, CPF, nome, endereço e profissão.
- O cadastro aceita de zero a três vínculos empregatícios, cada um com empregador e rendimento. Se houver um quarto vínculo, o cadastro é rejeitado com uma indicação do limite.
- Um nome de usuário já cadastrado impede a criação de outra conta com o mesmo identificador.

### HU02 · Entrar no sistema

Como cliente ou representante de agente cadastrado, quero entrar com usuário e senha para acessar as funções do meu perfil.

**Critérios de aceitação** · UC02 · RN01

- Credenciais válidas iniciam uma sessão com as permissões do perfil cadastrado.
- Credenciais inválidas impedem o acesso e retornam uma mensagem de falha sem revelar qual credencial está incorreta.
- As operações de negócio recusam requisições sem autenticação ou sem autorização para a ação e o registro.

## Pedidos de aluguel

### HU03 · Criar um pedido

Como cliente, quero enviar um pedido de aluguel para iniciar a avaliação financeira.

**Critérios de aceitação** · UC03 · RN02, RN03, RN05

- Ao enviar o pedido com um veículo válido e um período com fim posterior ao início, o sistema registra o cliente autenticado, gera um identificador e inicia o pedido em `EM_ANALISE`.
- O sistema rejeita o envio com veículo inexistente ou período inválido e indica o que precisa ser corrigido.

### HU04 · Consultar meus pedidos

Como cliente, quero consultar meus pedidos para acompanhar seu andamento.

**Critérios de aceitação** · UC04 · RN02

- A lista apresenta somente pedidos do cliente autenticado, incluindo os cancelados e contratados.
- O detalhe de um pedido próprio mostra o veículo, o período e o estado atual. O acesso direto a um pedido de outro cliente é recusado.

### HU05 · Alterar um pedido

Como cliente ou representante de agente autorizado, quero corrigir um pedido antes da formalização para que a avaliação use os dados atualizados.

**Critérios de aceitação** · UC05 · RN02, RN03

- O cliente pode alterar pedidos próprios. O representante pode alterar pedidos atribuídos à sua organização.
- Uma alteração aceita cria uma nova revisão e coloca o pedido em `EM_ANALISE`. O sistema conserva os pareceres anteriores com o número da revisão a que se referem.
- Pedidos `CANCELADO` ou `CONTRATADO` recusam alterações.

### HU06 · Cancelar meu pedido

Como cliente, quero cancelar um pedido próprio quando eu desistir do aluguel.

**Critérios de aceitação** · UC06 · RN02

- O cliente pode cancelar um pedido próprio em `EM_ANALISE`, `APROVADO` ou `REPROVADO`. O pedido passa para `CANCELADO` e continua disponível para consulta.
- Pedidos de outro cliente, já cancelados ou com contrato formalizado recusam a operação.

### HU07 · Consultar pedidos atribuídos

Como representante de empresa ou banco, quero consultar os pedidos atribuídos à minha organização para identificar os que precisam de avaliação.

**Critérios de aceitação** · UC07 · RN03

- A lista inclui somente os pedidos atribuídos à organização do representante e mostra o estado de cada pedido.
- O representante pode abrir os detalhes de um pedido atribuído e consultar os dados de identificação e renda necessários à avaliação.
- O acesso direto ao detalhe de um pedido fora das atribuições da organização é recusado.

## Avaliação e contratos

### HU08 · Registrar o parecer financeiro

Como representante de empresa ou banco, quero avaliar financeiramente um pedido para registrar se ele pode seguir para contratação.

**Critérios de aceitação** · UC08 · RN03, RN04

- Um representante autorizado registra um único parecer conclusivo para a revisão atual de um pedido em `EM_ANALISE`. O registro identifica a revisão, o representante, a data e o resultado.
- Um parecer favorável muda o estado para `APROVADO`. Um parecer desfavorável muda o estado para `REPROVADO`.
- Se o pedido mudar de revisão ou sair de `EM_ANALISE` antes da conclusão da avaliação, o parecer submetido é recusado.

### HU09 · Formalizar o contrato de aluguel

Como representante de empresa ou banco, quero formalizar o contrato de um pedido aprovado para registrar o aluguel.

**Critérios de aceitação** · UC09 · RN03, RN05

- O representante autorizado pode formalizar um pedido em `APROVADO` com parecer favorável para a revisão atual.
- A formalização registra um único contrato, o tipo e o proprietário registrado do veículo na formalização, conforme a regra de titularidade do tipo. O pedido passa para `CONTRATADO` na mesma operação.
- Pedidos sem parecer favorável vigente ou que já tenham contrato recusam a formalização.

### HU10 · Vincular o crédito ao aluguel

Como representante autorizado a formalizar o aluguel, quero vincular um crédito concedido por um banco agente para registrar a associação entre os contratos.

**Critérios de aceitação** · UC10 · RN06

- A formalização aceita um contrato de crédito opcional e registra o banco agente que o concedeu.
- Uma referência de crédito inválida ou já vinculada a outro aluguel impede a formalização com esse crédito.
- A formalização pode ser concluída sem vínculo de crédito.
