# Caas Car Rental — Modelagem inicial

O sistema permite que clientes solicitem, consultem, alterem e cancelem pedidos de aluguel pela internet. Representantes de empresas e bancos podem avaliar e alterar os pedidos sob sua responsabilidade. Um parecer financeiro positivo deixa o pedido disponível para o agente formalizar o contrato.

Para executar o portal e provisionar contas, consulte o [guia de configuração](../README.md).

## Documentos

| Arquivo | Conteúdo |
| --- | --- |
| [casos-de-uso.puml](casos-de-uso.puml) | Atores, limite do sistema e funcionalidades por perfil. |
| [historias-de-usuario.md](historias-de-usuario.md) | Histórias e critérios de aceitação, com referências aos casos de uso e às regras. |
| [diagrama-de-classes.puml](diagrama-de-classes.puml) | Entidades do domínio, relações, multiplicidades e estados do pedido. |
| [diagrama-de-pacotes.puml](diagrama-de-pacotes.puml) | Visão lógica dos dois subsistemas e direção das dependências. |

Os arquivos `.puml` são fontes editáveis. Abra-os em um visualizador PlantUML ou gere imagens na raiz do projeto com `java -jar /caminho/plantuml.jar -tsvg docs/*.puml`. [Documentação do PlantUML](https://plantuml.com/)

## Regras extraídas dos requisitos

| ID | Regra |
| --- | --- |
| RN01 | O uso das funções de negócio exige cadastro prévio e autenticação com usuário e senha. |
| RN02 | O cliente pode criar, consultar, alterar e cancelar seus próprios pedidos. |
| RN03 | Empresas e bancos são agentes e podem alterar e avaliar pedidos. Um parecer financeiro positivo habilita a formalização do contrato. |
| RN04 | O cadastro do contratante contém RG, CPF, nome, endereço, profissão e até três vínculos com empregadores, cada um com seu rendimento. |
| RN05 | O automóvel tem matrícula, ano, marca, modelo e placa. Seu proprietário registrado pode ser cliente, empresa ou banco, conforme o tipo de contrato. |
| RN06 | Um contrato de aluguel pode estar associado a um contrato de crédito concedido por um banco agente. |

## Fluxo proposto do pedido

Cada alteração nos dados do pedido incrementa seu número de revisão. O parecer registra a revisão analisada e o representante responsável. A formalização usa o parecer favorável da revisão atual.

| Ação | Estado de origem | Estado resultante |
| --- | --- | --- |
| Enviar pedido | Novo pedido | `EM_ANALISE` |
| Registrar parecer favorável | `EM_ANALISE` | `APROVADO` |
| Registrar parecer desfavorável | `EM_ANALISE` | `REPROVADO` |
| Alterar pedido | `EM_ANALISE`, `APROVADO` ou `REPROVADO` | `EM_ANALISE` |
| Cancelar pedido | `EM_ANALISE`, `APROVADO` ou `REPROVADO` | `CANCELADO` |
| Formalizar contrato | `APROVADO`, com parecer vigente | `CONTRATADO` |

O cancelamento conserva o pedido para consulta. A formalização cria um único contrato para o pedido. Ambas encerram o fluxo do pedido.

## Premissas originais da modelagem

As decisões adotadas na implementação estão documentadas em [implementacao-locacoes.md](implementacao-locacoes.md). A tabela abaixo preserva as perguntas da modelagem inicial para referência.

| Tema | Premissa usada na modelagem | Decisão pendente |
| --- | --- | --- |
| Contas de agentes | O cliente faz seu cadastro pela web. Cada representante de agente recebe uma conta vinculada a uma empresa ou a um banco antes do primeiro login. | Definir quem provisiona as contas dos representantes e como os pedidos são atribuídos às organizações. |
| Dados do pedido | O cliente informa um veículo e o período pretendido ao criar o pedido. | Definir como o veículo é selecionado e em que etapa se verifica a disponibilidade. |
| Avaliação e alterações | Um representante autorizado registra um parecer conclusivo por revisão. Cliente e agente podem alterar pedidos antes da formalização, inclusive após parecer desfavorável. Alterações exigem nova avaliação. | Definir se a avaliação depende de mais de um agente e quais campos cada perfil pode alterar. |
| Propriedade | O veículo mantém o proprietário registrado. O contrato guarda o proprietário registrado na formalização, conforme o tipo de contrato. | Definir os tipos de contrato, a regra de titularidade de cada tipo e quando a titularidade pode mudar. |
| Crédito | Cada contrato de aluguel pode ter até um contrato de crédito, vinculado na formalização. O crédito identifica o banco concedente. | Definir como a concessão é confirmada e quais dados financeiros precisam ser registrados. |
