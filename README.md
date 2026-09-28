# GestaoAppAulas

## Executar com Docker

Requisitos: Docker Desktop (ou Docker Engine com o plugin Compose).

Opcionalmente, copie `.env.example` para `.env` e altere as credenciais. Depois,
na raiz deste repositorio, execute:

```bash
docker compose up --build
```

A API fica disponivel em `http://localhost:8080` e o PostgreSQL em
`localhost:5432`. As migracoes do banco sao aplicadas automaticamente pelo
Flyway durante a inicializacao da API.

Para parar os containers:

```bash
docker compose down
```

Os dados do PostgreSQL permanecem no volume `postgres_data`. Para tambem
remover esses dados, use conscientemente `docker compose down -v`.
