# Product — contexto e instruções para o Claude Code

Responda em português (BR), de forma direta e concisa. Prefira edições pontuais a reescrever arquivos inteiros.

## 1. Contexto

Projeto da disciplina **Platforms, Microservices, DevOps and APIs** (Insper, 2026.2): uma aplicação web de compra e venda de produtos, feita de microserviços (Gateway, Auth, Account, Exchange, Product, Order). O repositório **root** é `store-pma` (privado) e incorpora cada microserviço como **submódulo git**.

Eu (Filipe) sou responsável **apenas pela parte Product**. Arquitetura: `internet → gateway → (auth, account, product, order) → database`. Para consumir a API o usuário precisa estar autenticado, mas isso é tratado pelo gateway/auth. O Product não implementa login.

Referências:
- Exercício: https://insper.github.io/platform/2026.2/exercises/product/
- Projeto: https://insper.github.io/platform/2026.2/exercises/project/
- Padrão de microserviço (hands-on de CRUD):
  - https://insper.github.io/platform/2026.2/hands-on/1/
  - .../hands-on/1/controller/
  - .../hands-on/1/service/
  - .../hands-on/1/containerization/
  - .../hands-on/1/repository/

## 2. RESTRIÇÕES (respeite sempre)

- Só posso criar, alterar e commitar nos repositórios **`Product`** (contrato) e **`Product-service`** (implementação).
- **Não mexer** em `store-pma` (root), `Account`, `Account-service`, `Auth` nem em nenhum outro repositório.
- **Não rodar `git submodule add`** nem alterar o compose do root. A integração no root é feita depois, fora daqui.
- Antes de qualquer alteração fora dessas duas pastas, pergunte.
- Não faça `git push` sem eu pedir.
- Os dois repos devem estar clonados lado a lado. Confirme os caminhos antes de começar.

## 3. Padrão da disciplina: 2 repositórios por microserviço

| Repo | Papel | Conteúdo |
|---|---|---|
| `Product` | Contrato (jar instalado no `.m2`) | `ProductController` (interface com `@FeignClient`), `ProductIn`, `ProductOut`. **Sem** classe Application, **sem** `resources`, **sem** testes. |
| `Product-service` | Implementação (app Spring Boot) | Resource, Service, Repository, Model, Parser, migrations Flyway, Dockerfile, compose local. |

O `Product-service` depende do contrato `store:product:1.0.0`. O microserviço Order (de outra pessoa) vai importar o `Product` via Feign, então **não mude paths nem campos do contrato depois de definidos**.

Convenções: Maven, Java 25, Spring Boot, YAML, groupId `store`, package `store.product`, Lombok, OpenFeign. Escreva os `pom.xml` à mão (sem Spring Initializr).

## 4. Especificação da API (exercício "Product API")

Base: `/products`. IDs são UUID (string).

| Endpoint | Request | Sucesso | Erros |
|---|---|---|---|
| `POST /products` | `{"name":"Tomato","price":10.12,"unit":"kg"}` | **201** + body `{"id":"...","name":"Tomato","price":10.12,"unit":"kg"}` e header `Location` | 400 (validação) |
| `GET /products` | — | **200** + lista de produtos (mesmo formato do body acima) | — |
| `GET /products/{id}` | — | **200** + produto | 404 se não existir |
| `DELETE /products/{id}` | — | **204** (sem body), inclusive para id desconhecido | — |
| `GET /products/health-check` | — | 200 (mesmo padrão do Account) | — |

Obs.: diferente do Account do hands-on (que devolve só `Location`), aqui o POST devolve **201 com o produto no body**.

## 5. Fase 1 — obrigatório (faça isto primeiro e pare para eu revisar)

### 5.1 Repo `Product` (contrato)

```
Product/
├── pom.xml
└── src/main/java/store/product/
    ├── ProductController.java
    ├── ProductIn.java
    └── ProductOut.java
```

**pom.xml**: copie o pom do `account` do hands-on "Controller", trocando `account` por `product`.
- parent `spring-boot-starter-parent` 4.0.3; groupId `store`; artifactId `product`; version `1.0.0`
- properties: `java.version=25`, `spring-cloud.version=2025.1.0`, `maven.compiler.proc=full`
- dependências: `spring-cloud-starter-openfeign` e `lombok` (optional)
- `dependencyManagement` importando `spring-cloud-dependencies`
- `maven-compiler-plugin` com `annotationProcessorPaths` do Lombok e source/target/release = `${java.version}`

**Código** (preços em `BigDecimal`):

```java
package store.product;

@FeignClient(name = "product", url = "http://product:8080")
public interface ProductController {

    @PostMapping("/products")
    ResponseEntity<ProductOut> create(@RequestBody ProductIn in);

    @GetMapping("/products")
    ResponseEntity<List<ProductOut>> findAll();

    @GetMapping("/products/{id}")
    ResponseEntity<ProductOut> findById(@PathVariable String id);

    @DeleteMapping("/products/{id}")
    ResponseEntity<Void> delete(@PathVariable String id);

    @GetMapping("/products/health-check")
    ResponseEntity<Void> healthCheck();
}
```

```java
@Builder public record ProductIn(String name, BigDecimal price, String unit) {}
@Builder public record ProductOut(String id, String name, BigDecimal price, String unit) {}
```

Depois rode `mvn clean install` dentro de `Product`.

### 5.2 Repo `Product-service` (implementação)

```
Product-service/
├── pom.xml
├── Dockerfile
├── compose.yaml
└── src/main/
    ├── java/store/product/
    │   ├── Product.java
    │   ├── ProductModel.java
    │   ├── ProductParser.java
    │   ├── ProductRepository.java
    │   ├── ProductResource.java
    │   ├── ProductService.java
    │   └── ProductServiceApplication.java
    └── resources/
        ├── application.yaml
        └── db/migration/
            ├── V2026.11.09.001__create_schema.sql
            └── V2026.11.09.002__create_table.sql
```

**pom.xml**: baseado no pom do `account-service` do hands-on "Repository".
- parent `spring-boot-starter-parent` 4.1.1; groupId `store`; artifactId `product-service`; version `1.0.0`
- properties: `java.version=25`, `spring-cloud.version=2025.1.3`, `maven.compiler.proc=full`
- dependências: `spring-boot-starter-webmvc`, `spring-cloud-starter-openfeign`, `lombok` (optional), `spring-boot-starter-data-jpa`, `postgresql` (runtime), `spring-boot-starter-flyway`, `flyway-database-postgresql` (runtime), `store:product:1.0.0`
- `dependencyManagement` do Spring Cloud; plugins `spring-boot-maven-plugin` e `maven-compiler-plugin` (Lombok nos executions `default-compile` e `default-testCompile`)
- Use exatamente as versões acima (são as do hands-on). Se houver conflito de build, alinhe o contrato à versão do service e me avise.

**Classes** (Clean Architecture, igual ao Account):
- `Product`: entidade de domínio (`@Builder @Data @Accessors(fluent = true)`) com `id`, `name`, `price`, `unit`.
- `ProductModel`: `@Entity @Table(name = "products")`, id `String` com `@GeneratedValue(strategy = GenerationType.UUID)`, colunas `id`, `name`, `price`, `unit`. Construtor `ProductModel(Product)` e método `to()`.
- `ProductParser`: métodos estáticos `ProductIn → Product`, `Product → ProductOut`, `List<Product> → List<ProductOut>`.
- `ProductRepository`: `extends CrudRepository<ProductModel, String>`, redeclarando `List<ProductModel> findAll()`.
- `ProductService`:
  - `create`: valida `name` não vazio, `price` > 0 e `unit` não vazio; senão `ResponseStatusException(400, ...)`. Salva e devolve o `Product`.
  - `findById`: devolve `null` se não existir (o Resource converte em 404).
  - `delete`: `deleteById` (204 mesmo se não existir).
  - `findAll`.
- `ProductResource`: `@Controller implements ProductController`. `create` devolve `201` com `Location` (`ServletUriComponentsBuilder`) **e** o `ProductOut` no body; `findById` lança `ResponseStatusException(NOT_FOUND)` se `null`; `delete` devolve `204`; `healthCheck` devolve `200`.
- `ProductServiceApplication`: leia o hands-on "c. Service" (link na seção 1) para ver como a classe Application é feita. Não habilite `@EnableFeignClients` sem confirmar lá, pois o service **implementa** a interface em vez de consumi-la.

**Migrations Flyway** (mesmo formato do hands-on, com a data de hoje):

```sql
-- V2026.11.09.001__create_schema.sql
CREATE SCHEMA IF NOT EXISTS products;
```

```sql
-- V2026.11.09.002__create_table.sql
CREATE TABLE products.products (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    unit VARCHAR(32) NOT NULL
);
```

**application.yaml**:

```yaml
spring:
  application:
    name: product

  datasource:
    url: jdbc:postgresql://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_DB}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver

  flyway:
    baseline-on-migrate: true
    schemas: products

  jpa:
    properties:
      hibernate:
        default_schema: products
```

**Dockerfile**: siga o hands-on "d. Containerization" (link na seção 1). Build com Maven, runtime com JRE 25, expõe 8080.

**compose.yaml** (local, dentro do `Product-service`, para não tocar no compose do root):

```yaml
name: store

services:
  db:
    image: postgres:17
    volumes:
      - db-data:/var/lib/postgresql/data
    environment:
      POSTGRES_USER: ${DB_USER:-store}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-store}
      POSTGRES_DB: ${DB_NAME:-store}

  product:
    build: .
    ports:
      - 8080:8080
    environment:
      DATABASE_HOST: db
      DATABASE_PORT: 5432
      DATABASE_DB: ${DB_NAME:-store}
      DATABASE_USERNAME: ${DB_USER:-store}
      DATABASE_PASSWORD: ${DB_PASSWORD:-store}
    depends_on:
      - db

volumes:
  db-data:
```

### 5.3 Critério de pronto da Fase 1

1. `mvn clean install` passa em `Product`.
2. `docker compose up --build` sobe em `Product-service`, e o log mostra as 2 migrations do Flyway aplicadas.
3. Testes manuais com curl (sem auth, pois não há gateway local):
   ```bash
   curl -i -X POST localhost:8080/products -H 'Content-Type: application/json' \
     -d '{"name":"Tomato","price":10.12,"unit":"kg"}'      # 201 + body
   curl -i localhost:8080/products                          # 200 + lista
   curl -i localhost:8080/products/<id>                     # 200
   curl -i localhost:8080/products/<id-inexistente>         # 404
   curl -i -X DELETE localhost:8080/products/<id>           # 204
   curl -i -X POST localhost:8080/products -H 'Content-Type: application/json' \
     -d '{"name":"","price":-1,"unit":""}'                  # 400
   ```

## 6. Fase 2 — adicionais (só depois de eu aprovar a Fase 1)

- Busca por nome com "like": `GET /products?name=...` (`findByNameContainingIgnoreCase`). Mantenha compatível com o contrato base.
- Validações de entrada e tratamento de erro padronizado.
- Autorização por role (admin cria/atualiza/deleta produtos; user só cria orders): **leia antes** https://insper.github.io/platform/2026.2/hands-on/3/authorization/ e me pergunte como o gateway repassa token/role.
- Endpoint de update, se fizer sentido (não está no escopo mínimo).

## 7. Fase 3 — nice to have

- Swagger com SpringDoc.
- Redis como banco in-memory/cache no Product (hands-on 8).
- Observabilidade com Prometheus e Grafana (hands-on 6).

## 8. Regras da disciplina sobre uso de IA

- O uso de IA na implementação é permitido de forma parcial (boilerplate, sugestões, correção de erros) e **precisa ser documentado**. Ao final de cada fase, liste o que foi gerado com IA e o que eu escrevi ou ajustei.
- Eu preciso conseguir **explicar e defender cada parte** do código. Ao entregar cada classe, explique rapidamente o que ela faz e por quê.

## 9. Fora do seu escopo (só para contexto)

Depois, o dono do root (ou eu, se permitido) faz no `store-pma`:

```bash
git submodule add <url-do-Product> api/product
git submodule add <url-do-Product-service> api/product-service
```

Regra dos submódulos: dar push no submódulo **antes** de atualizar o ponteiro no root. Não faça nada disso aqui.