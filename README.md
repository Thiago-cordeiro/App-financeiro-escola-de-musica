# GestaoAppAulas

API Spring Boot para gestão de aulas e pagamentos, com autenticação JWT RSA e documentação OpenAPI/Swagger.

## Executar com Docker

Requisitos: Docker Desktop (ou Docker Engine com o plugin Compose).

Copie `.env.example` para `.env`, altere as credenciais e configure um par de chaves RSA. As chaves devem ser informadas em Base64, sem serem versionadas. Um exemplo com OpenSSL:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl pkey -in jwt-private.pem -pubout -outform DER | openssl base64 -A
openssl pkcs8 -topk8 -nocrypt -in jwt-private.pem -outform DER | openssl base64 -A
```

Copie a saída do segundo comando para `JWT_PUBLIC_KEY` e a saída do terceiro para `JWT_PRIVATE_KEY`. O arquivo `jwt-private.pem` é secreto e não deve ser adicionado ao Git.

Para desenvolvimento local, também é possível usar `JWT_GENERATE_KEY_PAIR=true` e deixar as duas chaves vazias. Nesse modo, a API gera chaves efêmeras e os tokens deixam de ser válidos após cada reinicialização. Não utilize esse modo em produção.

Para criar o primeiro administrador de forma idempotente, defina no `.env`:

```dotenv
ADMIN_INITIAL_ENABLED=true
ADMIN_INITIAL_NAME=Administrador
ADMIN_INITIAL_EMAIL=admin@exemplo.com
ADMIN_INITIAL_PASSWORD=uma-senha-forte
```

Depois, na raiz deste repositório, execute:

```bash
docker compose up --build
```

A API fica disponível em `http://localhost:8080` e o PostgreSQL em
`localhost:5432`. As migrações do banco são aplicadas automaticamente pelo
Flyway durante a inicializacao da API.

## Autenticação

O login é público:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@exemplo.com",
  "password": "uma-senha-forte"
}
```

A resposta contém `accessToken`, `tokenType` e `expiresIn`. Nas demais rotas, envie:

```http
Authorization: Bearer <accessToken>
```

## Swagger/OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Contrato OpenAPI: `http://localhost:8080/v3/api-docs`

No Swagger UI, use **Authorize** e informe somente o JWT retornado pelo login.

## Administrador de teste

No ambiente Docker local, o endpoint público temporário `POST /api/test/admins`
fica habilitado por padrão. Ele recebe `name`, `email` e `password`, cria uma
conta com papel `ADMIN` e nunca devolve o hash da senha. Exemplo:

```json
{
  "name": "Administrador de teste",
  "email": "admin@teste.com",
  "password": "senha-segura"
}
```

Defina `TEST_ADMIN_ENDPOINT_ENABLED=false` antes de publicar a aplicação. Fora
do Docker Compose, o endpoint permanece desabilitado por padrão.

Para parar os containers:

```bash
docker compose down
```

Os dados do PostgreSQL permanecem no volume `postgres_data`. Para tambem
remover esses dados, use conscientemente `docker compose down -v`.
