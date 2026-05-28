# Camadas do Cypher

Este documento explica a separação inicial de camadas do projeto. A ideia aqui não é criar uma arquitetura complexa, mas deixar claro onde cada tipo de código deve morar.

## 1. API

Pacote: `com.cypher.analysis.api`

Responsabilidade:
- Receber HTTP.
- Validar DTOs de entrada.
- Devolver DTOs de resposta.
- Não deve conter regra de negócio.

Exemplo:
- `AnalysisController`
- `AnalysisRequest`
- `AnalysisResponse`

## 2. Application

Pacote: `com.cypher.analysis.application`

Responsabilidade:
- Orquestrar um caso de uso.
- Decidir a ordem dos passos.
- Abrir transação.
- Chamar domínio, portas e serviços necessários.

Exemplo:
- `AnalyzeInvoiceUseCase`

Esta camada responde a pergunta: "o que acontece quando o usuário pede uma análise?"

## 3. Ports

Pacote: `com.cypher.analysis.application.port`

Responsabilidade:
- Definir contratos que a aplicação precisa.
- Esconder detalhes de tecnologia.

Exemplos:
- `InvoicePersistencePort`
- `RiskAnalysisPersistencePort`
- `XmlStoragePort`
- `IdempotencyPort`

O caso de uso depende dessas interfaces, não de PostgreSQL, Redis ou filesystem diretamente.

## 4. Domain

Pacote: `com.cypher.analysis.domain`

Responsabilidade:
- Modelar conceitos de negócio.
- Proteger invariantes.
- Representar entidades e value objects.

Exemplos:
- `Invoice`
- `RiskAnalysis`
- `RiskScore`
- `FinancialMetrics`
- `NFeData`

Esta camada deve saber o mínimo possível sobre Spring, HTTP, Redis ou banco.

## 5. Engine

Pacote: `com.cypher.analysis.engine`

Responsabilidade:
- Calcular score de risco.
- Executar regras.
- Produzir fatores explicáveis.

Exemplos:
- `RiskEngineService`
- `RuleRegistry`
- `RiskRule`

No MVP, ele ainda é chamado diretamente pelo use case. Isso é aceitável porque o engine é parte do domínio de análise.

## 6. Adapters

Pacote: `com.cypher.analysis.adapter`

Responsabilidade:
- Implementar portas usando tecnologia real.

Exemplos:
- `InvoiceJpaAdapter`
- `RiskAnalysisJpaAdapter`

Esses adapters traduzem a necessidade da aplicação para Spring Data JPA.

## 7. Infrastructure / Service

Pacote atual: `com.cypher.analysis.service`

Responsabilidade atual:
- Implementações técnicas ainda existentes no MVP.

Exemplos:
- `IdempotencyService` implementa `IdempotencyPort` usando Redis.
- `XmlStorageService` implementa `XmlStoragePort` usando filesystem local.
- `AnalysisService` ficou como fachada para preservar a API interna já usada pelo controller.

Com o amadurecimento do projeto, `IdempotencyService` e `XmlStorageService` podem ir para pacotes de adapter/infrastructure mais específicos.

## Fluxo atual

```text
HTTP
  -> AnalysisController
  -> AnalysisService
  -> AnalyzeInvoiceUseCase
  -> ports
      -> JPA adapters
      -> Redis idempotency
      -> XML storage
  -> domain / engine
  -> AnalysisResponse
```

## Regra prática para decidir onde colocar código

- Código que recebe HTTP: `api`
- Código que coordena passos de um caso de uso: `application`
- Interface que representa uma dependência externa: `application.port`
- Regra/conceito de negócio: `domain`
- Implementação com banco, Redis, arquivo ou HTTP externo: `adapter` ou `infrastructure`
- Código de score e regras de risco: `engine`

## Próximos passos naturais

1. Mover `IdempotencyService` para um adapter Redis.
2. Mover `XmlStorageService` para um adapter de storage.
3. Criar um port para consulta SEFAZ/Receita.
4. Corrigir idempotência com token/fingerprint.
5. Criar testes de integração com PostgreSQL e Redis reais.
