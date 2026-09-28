# SPEC — Login, autenticação e autorização com JWT

## 1. Objetivo

Implementar o módulo de autenticação e autorização da API utilizando:

- Java 21
- Spring Boot
- Spring Security
- Spring Security OAuth2 Resource Server
- JWT Bearer Token
- JPA / Hibernate
- PostgreSQL
- arquitetura baseada em Controller → Service → Repository
- DTOs para entrada e saída
- roles definidas através de `enum` Java

A implementação deve priorizar:

- segurança;
- código limpo;
- baixo acoplamento;
- princípios SOLID;
- boas práticas do Java 21;
- boas práticas do Spring Security;
- separação entre autenticação e regras de negócio.

---

# 2. Regras obrigatórias de segurança

## 2.1 Senhas

Nunca armazenar:

```text
senha123
```

ou qualquer senha em texto puro no banco.

Também não utilizar criptografia reversível para armazenar senhas.

As senhas deverão ser armazenadas usando hash através do `PasswordEncoder` do Spring Security.

Utilizar:

```java
BCryptPasswordEncoder
```

A aplicação deve possuir um bean:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

No cadastro:

```java
String passwordHash = passwordEncoder.encode(request.password());
```

No login, nunca descriptografar a senha.

A validação deverá utilizar o mecanismo do Spring Security ou:

```java
passwordEncoder.matches(rawPassword, passwordHash);
```

A entidade deverá utilizar um nome que deixe claro que o valor armazenado é um hash:

```java
private String passwordHash;
```

Nunca:

```java
private String password;
```

contendo a senha original.

---

# 3. Role do usuário

As permissões da aplicação deverão seguir exclusivamente o enum existente no domínio.

Exemplo:

```java
public enum Role {
    ADMIN,
    PROFESSOR
}
```

Caso o projeto possua outros valores, utilizar exatamente os valores existentes no enum atual.

Não criar uma tabela de roles caso o sistema já tenha sido projetado para utilizar enum.

Na entidade utilizar:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Role role;
```

Persistir:

```text
ADMIN
PROFESSOR
```

e não números como:

```text
0
1
```

Isso evita problemas caso a ordem do enum seja alterada.

---

# 4. Regra importante para criação de usuários

O cliente NÃO deve poder escolher livremente uma role privilegiada durante um cadastro público.

Nunca permitir algo como:

```json
{
  "email": "usuario@email.com",
  "password": "123",
  "role": "ADMIN"
}
```

em um endpoint público de cadastro.

A role deve ser determinada pelo backend.

Exemplo:

```java
user.setRole(Role.PROFESSOR);
```

Caso exista criação de administradores, ela deverá ocorrer:

- através de migration/seed inicial;
- através de endpoint protegido para ADMIN;
- ou através de mecanismo administrativo interno.

Um usuário não autenticado nunca poderá criar a própria conta como `ADMIN`.

---

# 5. Entidade responsável pela autenticação

A entidade de usuário/administrador existente deve conter ao menos:

```text
id
nome
email
passwordHash
role
ativo
```

O email deverá:

- ser obrigatório;
- ser único;
- ser normalizado antes de persistir;
- não diferenciar maiúsculas/minúsculas para autenticação.

Exemplo:

```java
email.trim().toLowerCase(Locale.ROOT)
```

O banco deve possuir constraint `UNIQUE` no email.

---

# 6. DTO de cadastro

Criar um DTO específico.

Exemplo:

```java
public record RegisterRequest(
    @NotBlank
    String nome,

    @NotBlank
    @Email
    String email,

    @NotBlank
    @Size(min = 8, max = 72)
    String password
) {}
```

Não receber a entidade JPA diretamente no controller.

---

# 7. DTO de login

Criar:

```java
public record LoginRequest(
    @NotBlank
    @Email
    String email,

    @NotBlank
    String password
) {}
```

---

# 8. Resposta do login

Retornar um objeto semelhante a:

```java
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}
```

Exemplo:

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Nunca retornar:

- password;
- passwordHash;
- chave privada;
- secret JWT;
- informações internas do Spring Security.

---

# 9. Endpoint de autenticação

Criar:

```text
POST /api/auth/login
```

Request:

```json
{
  "email": "admin@email.com",
  "password": "senhaSegura"
}
```

Processamento esperado:

```text
request
   ↓
AuthController
   ↓
AuthService
   ↓
AuthenticationManager
   ↓
Spring Security
   ↓
UserDetailsService
   ↓
PasswordEncoder
   ↓
TokenService
   ↓
JWT
```

O controller não deverá possuir lógica de autenticação.

---

# 10. Endpoint de cadastro

Caso cadastro seja necessário:

```text
POST /api/auth/register
```

Responsabilidades:

1. validar DTO;
2. normalizar email;
3. verificar se email já existe;
4. validar senha;
5. gerar hash utilizando `PasswordEncoder`;
6. definir role permitida pelo backend;
7. persistir usuário;
8. nunca retornar passwordHash.

---

# 11. JWT

Utilizar JWT como access token.

O token deverá conter somente informações necessárias para autenticação/autorização.

Claims recomendadas:

```text
sub
iss
iat
exp
jti
roles
```

Exemplo conceitual:

```json
{
  "sub": "42",
  "iss": "music-school-api",
  "iat": 1790550000,
  "exp": 1790553600,
  "roles": [
    "ADMIN"
  ]
}
```

Não colocar dados sensíveis no JWT.

JWT é assinado, mas seu conteúdo não deve ser considerado secreto.

Não colocar:

```text
senha
passwordHash
CPF
chave PIX
dados bancários
```

dentro do token.

---

# 12. Identificação do usuário

Preferencialmente utilizar o ID do usuário no `sub`.

Exemplo:

```text
sub = "42"
```

Evitar utilizar informações mutáveis como nome como identificador principal.

---

# 13. Expiração

O access token deverá possuir tempo de vida limitado.

Exemplo inicial:

```text
3600 segundos
```

equivalente a:

```text
1 hora
```

A duração deverá ficar configurável:

```yaml
app:
  security:
    jwt:
      issuer: music-school-api
      access-token-expiration: 3600
```

Não espalhar valores de expiração como números mágicos pelo código.

---

# 14. Assinatura JWT

Não utilizar secret hardcoded no código:

```java
private static final String SECRET = "minha-chave";
```

Proibido.

Segredos devem vir de:

```text
variáveis de ambiente
secret manager
configuração externa segura
```

Preferencialmente utilizar assinatura assimétrica para ambientes de produção:

```text
RSA
```

com:

```text
chave privada → assinar

chave pública → validar
```

Nunca versionar a chave privada no Git.

Adicionar arquivos de segredo ao `.gitignore` quando aplicável.

---

# 15. Spring Security

Utilizar configuração moderna do Spring Security.

Não utilizar:

```java
WebSecurityConfigurerAdapter
```

Criar:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http)
```

Configurar a API como stateless.

Conceitualmente:

```java
http
    .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    );
```

A API não deverá depender de sessão HTTP para autenticação JWT.

---

# 16. Resource Server

Utilizar o suporte nativo do Spring Security para Bearer JWT sempre que possível:

```java
.oauth2ResourceServer(oauth2 ->
    oauth2.jwt(Customizer.withDefaults())
)
```

Evitar criar filtros JWT manuais quando o próprio Spring Security já fornece os componentes necessários.

Utilizar:

```text
JwtEncoder
JwtDecoder
```

para geração e validação dos tokens.

---

# 17. Rotas públicas

Permitir sem autenticação somente endpoints explicitamente públicos.

Exemplo:

```text
POST /api/auth/login
POST /api/auth/register
/swagger-ui/**
/v3/api-docs/**
```

Todas as demais rotas deverão exigir autenticação por padrão.

Conceitualmente:

```java
.requestMatchers("/api/auth/**").permitAll()
.anyRequest().authenticated()
```

Preferir estratégia:

```text
deny by default
```

ao invés de liberar tudo e posteriormente tentar proteger rotas individualmente.

---

# 18. Autorização por role

As roles deverão ser extraídas do JWT e transformadas em `GrantedAuthority`.

Padronizar authorities conforme convenção Spring:

```text
ROLE_ADMIN
ROLE_PROFESSOR
```

Exemplo:

```java
.hasRole("ADMIN")
```

ou:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Quando possível, não espalhar strings arbitrárias pelo projeto.

Criar constantes ou utilizar conversão centralizada a partir do enum.

---

# 19. Proteção de endpoints

Exemplo:

```text
/api/admin/**
```

deverá exigir:

```text
ADMIN
```

Exemplo:

```java
.requestMatchers("/api/admin/**")
.hasRole("ADMIN")
```

Também habilitar autorização em métodos quando fizer sentido:

```java
@EnableMethodSecurity
```

permitindo:

```java
@PreAuthorize("hasRole('ADMIN')")
```

---

# 20. Regra de autorização de recurso

Role não substitui validação de propriedade.

Por exemplo:

```text
GET /api/professores/{id}
```

não deve permitir que qualquer usuário autenticado altere recursos arbitrários apenas porque descobriu outro ID.

Sempre verificar:

```text
usuário autenticado
        +
role
        +
permissão sobre o recurso
```

quando a regra de negócio exigir.

---

# 21. UserDetailsService

Criar implementação responsável por recuperar o usuário através do banco.

Exemplo conceitual:

```java
loadUserByUsername(email)
```

Ela deverá buscar o usuário através de:

```java
UserRepository
```

e transformar a role do domínio em authority do Spring Security.

Nunca carregar passwordHash em DTO de resposta da API.

---

# 22. AuthenticationManager

Utilizar o fluxo padrão do Spring Security através de:

```java
AuthenticationManager
```

O `AuthService` poderá utilizar:

```java
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        email,
        password
    )
);
```

Não implementar manualmente:

```java
if (senha.equals(usuario.getSenha()))
```

Isso é proibido.

---

# 23. Proteção contra enumeração de usuários

No login, não informar:

```text
"Email não encontrado"
```

ou:

```text
"Senha incorreta"
```

Separadamente.

Retornar mensagem genérica:

```json
{
  "message": "Credenciais inválidas"
}
```

com:

```text
HTTP 401
```

Isso reduz exposição de quais emails possuem cadastro.

---

# 24. Status HTTP

Utilizar corretamente:

```text
200 OK
```

Login efetuado.

```text
201 Created
```

Usuário cadastrado.

```text
400 Bad Request
```

Dados inválidos.

```text
401 Unauthorized
```

Token ausente, inválido, expirado ou login incorreto.

```text
403 Forbidden
```

Usuário autenticado, porém sem permissão.

```text
409 Conflict
```

Tentativa de cadastrar email já existente.

---

# 25. Tratamento de erros

Criar tratamento centralizado usando:

```java
@RestControllerAdvice
```

Não utilizar `try/catch` genérico em todos os controllers.

Criar DTO padronizado de erro.

Exemplo:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciais inválidas",
  "timestamp": "..."
}
```

Nunca retornar:

```text
stack trace
SQL
nome das tabelas
detalhes de exceções internas
segredos
```

ao cliente.

---

# 26. Validação da senha

A senha deverá possuir tamanho mínimo definido pela aplicação.

Mínimo inicial:

```text
8 caracteres
```

Não realizar transformações como:

```java
password.trim()
```

na senha do usuário.

Espaços podem fazer parte de uma senha válida.

Também não limitar a senha a regras excessivamente rígidas como obrigatoriamente:

```text
1 maiúscula
1 símbolo
1 número
```

salvo se houver requisito explícito de negócio.

O limite máximo deve respeitar o algoritmo utilizado.

Para BCrypt, limitar entrada a tamanho compatível e seguro, evitando comportamento inesperado.

---

# 27. Proteção de logs

Nunca registrar:

```java
log.info("Senha: {}", password);
```

Nunca registrar:

```text
Authorization: Bearer eyJ...
```

completo.

Nunca registrar:

```text
passwordHash
private key
JWT secret
```

Logs de autenticação podem conter informações como:

```text
userId
resultado da autenticação
timestamp
IP quando realmente necessário
```

sem expor credenciais.

---

# 28. CORS

Configurar CORS explicitamente.

Não utilizar indiscriminadamente:

```text
*
```

em produção.

A origem permitida deverá vir de configuração.

Exemplo:

```yaml
app:
  cors:
    allowed-origins:
      - https://app.exemplo.com
```

Em desenvolvimento poderá ser permitido:

```text
http://localhost:4200
```

ou o endereço utilizado pelo frontend.

---

# 29. CSRF

Para uma API REST stateless utilizando Bearer Token enviado através do header:

```http
Authorization: Bearer TOKEN
```

configurar CSRF de acordo com essa arquitetura.

Não copiar configurações de segurança sem compreender o modelo de autenticação.

Caso futuramente tokens sejam armazenados e enviados automaticamente por cookies, revisar a proteção contra CSRF.

---

# 30. Token recebido pelo backend

O cliente deverá enviar:

```http
Authorization: Bearer <access_token>
```

O backend deverá:

1. extrair o Bearer Token através do Spring Security;
2. validar assinatura;
3. validar expiração;
4. validar issuer;
5. validar claims obrigatórias;
6. construir o Authentication;
7. carregar authorities;
8. verificar autorização da rota.

Não confiar em informações de role enviadas separadamente pelo frontend.

O frontend não determina permissões.

---

# 31. Validação do issuer

Definir um issuer próprio da aplicação.

Exemplo:

```text
music-school-api
```

Tokens com issuer diferente deverão ser rejeitados.

---

# 32. Validação de audience

Caso seja utilizada audience, definir explicitamente qual aplicação/API pode aceitar o token.

Exemplo:

```text
aud = music-school-api
```

Tokens direcionados a outro serviço deverão ser rejeitados.

---

# 33. Dependências

Utilizar dependências oficiais do Spring.

Exemplo:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Utilizar também as dependências existentes de:

```text
Spring Data JPA
PostgreSQL
Flyway
Swagger/OpenAPI
```

Não adicionar biblioteca JWT externa se o suporte nativo do Spring Security atender aos requisitos.

---

# 34. Estrutura sugerida

```text
src/main/java/.../

├── auth
│   ├── controller
│   │   └── AuthController.java
│   │
│   ├── dto
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   └── RegisterRequest.java
│   │
│   └── service
│       └── AuthService.java
│
├── security
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   ├── TokenService.java
│   ├── CustomUserDetailsService.java
│   └── SecurityUser.java
│
├── user
│   ├── User.java
│   ├── Role.java
│   └── UserRepository.java
│
└── exception
    ├── GlobalExceptionHandler.java
    ├── InvalidCredentialsException.java
    └── EmailAlreadyExistsException.java
```

Adaptar os nomes às entidades já existentes no projeto.

Não duplicar entidades existentes.

---

# 35. Separação de responsabilidades

## AuthController

Responsável somente por:

```text
HTTP request
↓
validação
↓
chamada do service
↓
HTTP response
```

---

## AuthService

Responsável por:

```text
cadastro
autenticação
regras relacionadas ao login
geração do token
```

---

## TokenService

Responsável exclusivamente por:

```text
criação dos JWTs
```

Não colocar regras de negócio dentro dele.

---

## SecurityConfig

Responsável por:

```text
SecurityFilterChain
rotas públicas
rotas protegidas
roles
OAuth2 Resource Server
PasswordEncoder
AuthenticationManager
CORS
session policy
```

---

# 36. Java 21

Utilizar recursos modernos quando fizer sentido.

DTOs devem preferencialmente utilizar:

```java
record
```

Exemplo:

```java
public record LoginRequest(
    String email,
    String password
) {}
```

Evitar classes DTO enormes com:

```text
getters
setters
constructors
equals
hashCode
```

quando um `record` resolver corretamente.

Não utilizar recursos modernos apenas por estilo; priorizar clareza.

---

# 37. Banco de dados

A migration deverá armazenar apenas o hash.

Exemplo:

```sql
password_hash VARCHAR(255) NOT NULL
```

Nunca:

```sql
password VARCHAR(255)
```

se o conteúdo representar a senha em texto puro.

A migration deverá possuir constraint de email único.

Exemplo:

```sql
CONSTRAINT uk_user_email UNIQUE (email)
```

---

# 38. Usuário ADMIN inicial

Caso seja necessário criar um ADMIN inicial, não colocar senha em texto puro em migration versionada.

Evitar:

```sql
INSERT INTO administrador (
    email,
    password
)
VALUES (
    'admin@email.com',
    'admin123'
);
```

Preferir criação controlada através de variável de ambiente ou bootstrap seguro.

Caso um hash seja utilizado em ambiente de desenvolvimento, documentar claramente que se trata apenas de ambiente local/teste.

---

# 39. Swagger

Manter:

```text
/api/auth/login
```

acessível sem JWT.

Adicionar suporte à autenticação Bearer no Swagger/OpenAPI.

O Swagger deverá permitir:

```text
Authorize
↓
Bearer JWT
```

para testar endpoints protegidos.

Não incluir automaticamente secrets ou credenciais reais na documentação.

---

# 40. Testes obrigatórios

Criar testes cobrindo pelo menos:

### Cadastro

```text
✓ cadastra usuário válido
✓ senha armazenada não é igual à senha original
✓ email duplicado retorna erro
✓ email inválido é rejeitado
✓ usuário público não consegue criar role ADMIN
```

### Login

```text
✓ credenciais corretas retornam JWT
✓ senha incorreta retorna 401
✓ email inexistente retorna 401
✓ resposta não contém passwordHash
```

### JWT

```text
✓ JWT válido permite acesso
✓ JWT expirado retorna 401
✓ JWT com assinatura inválida retorna 401
✓ JWT com issuer inválido é rejeitado
```

### Roles

```text
✓ ADMIN acessa endpoint administrativo
✓ PROFESSOR não acessa endpoint exclusivo de ADMIN
✓ ausência de JWT retorna 401
✓ usuário autenticado sem role necessária recebe 403
```

---

# 41. Critério principal de autorização

Considere sempre:

```text
Frontend ≠ segurança
```

Esconder um botão no Angular/React não significa proteger uma operação.

Toda regra de autorização deverá obrigatoriamente existir no backend.

Exemplo:

```text
Frontend:
oculta botão "Cadastrar professor"

Backend:
.hasRole("ADMIN")
```

O backend é a autoridade final.

---

# 42. O que NÃO implementar

Não implementar:

```text
❌ senha em texto puro
❌ senha criptografada reversivelmente
❌ MD5
❌ SHA-1 para senha
❌ comparação manual de senha
❌ JWT secret hardcoded
❌ token sem expiração
❌ role enviada pelo frontend como fonte de confiança
❌ role baseada apenas em esconder elementos da UI
❌ WebSecurityConfigurerAdapter
❌ sessão HTTP para manter login JWT
❌ filtros JWT customizados desnecessários
❌ lógica de autenticação dentro de Controller
❌ entidade JPA diretamente como request
❌ stack trace retornado pela API
```

---

# 43. Fluxo completo esperado

```text
LOGIN

Frontend
   │
   │ POST /api/auth/login
   │ email + password
   ▼
AuthController
   │
   ▼
AuthService
   │
   ▼
AuthenticationManager
   │
   ▼
UserDetailsService
   │
   ▼
UserRepository
   │
   ▼
PasswordEncoder
   │
   │ credenciais válidas
   ▼
TokenService
   │
   ▼
JWT
   │
   ▼
Frontend
```

Nas próximas requisições:

```text
Frontend
   │
   │ Authorization: Bearer JWT
   ▼
Spring Security
   │
   ├── assinatura
   ├── expiração
   ├── issuer
   ├── claims
   └── authorities
          │
          ▼
     Controller
```

---

# 44. Critérios de aceite

A implementação somente estará concluída quando:

- nenhuma senha for persistida em texto puro;
- BCrypt/PasswordEncoder for utilizado;
- login utilizar AuthenticationManager/Spring Security;
- JWT possuir expiração;
- JWT possuir assinatura válida;
- secrets não estiverem hardcoded;
- API for stateless;
- Bearer Token for utilizado;
- roles forem derivadas do enum Java existente;
- ADMIN e demais perfis respeitarem suas permissões;
- um usuário não puder elevar a própria role;
- endpoints privados exigirem autenticação;
- endpoints administrativos exigirem role correta;
- erros 401 e 403 forem tratados corretamente;
- dados sensíveis não forem retornados;
- Swagger suportar Bearer Token;
- existirem testes de autenticação e autorização;
- a implementação seguir Java 21 e APIs atuais do Spring Security.

## Instrução final para a IA

Antes de criar qualquer classe, examine a estrutura atual do projeto.

Reutilize:

- entidades existentes;
- enum de roles existente;
- repositories existentes;
- migrations existentes;
- convenções de package já adotadas.

Não recrie classes ou tabelas que já existam.

Não altere o modelo de domínio sem necessidade.

Implemente a autenticação como uma camada adicional sobre a arquitetura existente.

Caso encontre uma decisão de segurança conflitante com esta especificação, priorize a alternativa mais segura e documente claramente a decisão no código.
