# Diretrizes de Segurança para Agentes de IA

Este sistema realiza gerenciamento de alunos, professores, matrículas, recebimentos e repasses financeiros.

Qualquer IA, agente ou desenvolvedor que altere este projeto deve considerar segurança como requisito obrigatório da implementação.

As regras abaixo devem ser respeitadas em todo código criado ou modificado.

## 1. Princípio geral

Nunca confiar em dados recebidos do cliente.

Todo dado vindo de:

* body;
* query string;
* path parameters;
* headers;
* formulários;
* arquivos;
* APIs externas;

deve ser tratado como entrada não confiável.

A validação do frontend serve apenas para experiência do usuário.

Toda validação de segurança e regra de negócio deve existir também no backend.

---

## 2. Autenticação

A autenticação deve ser implementada utilizando Spring Security.

Nunca implementar autenticação manual quando o Spring Security oferecer mecanismo equivalente.

Senhas nunca podem:

* ser armazenadas em texto puro;
* ser retornadas por endpoints;
* aparecer em DTOs de resposta;
* aparecer em logs;
* ser enviadas para o frontend após autenticação.

As senhas devem ser armazenadas exclusivamente utilizando algoritmo adequado de hash de senha, como BCrypt ou Argon2, através de `PasswordEncoder`.

Nunca utilizar diretamente:

* MD5;
* SHA-1;
* SHA-256 simples;
* Base64;
* criptografia reversível;

para armazenamento de senha.

O campo `senha_hash` da tabela `administrador` deve armazenar somente o resultado produzido pelo PasswordEncoder.

---

## 3. Autorização

Autenticação não significa autorização.

Toda operação protegida deve verificar se o usuário autenticado possui permissão para executar aquela ação.

O campo:

`administrador.nivel_acesso`

será utilizado para definição de permissões através de enum na aplicação.

Não confiar em informações de nível de acesso enviadas pelo frontend.

O backend deve determinar as permissões utilizando exclusivamente o usuário autenticado.

Sempre validar autorização também no backend, mesmo que determinada funcionalidade esteja escondida no frontend.

Quando apropriado, utilizar os mecanismos de autorização do Spring Security em endpoints e/ou métodos de serviço.

---

## 4. Proteção contra IDOR / acesso indevido

Nunca considerar um registro autorizado apenas porque o usuário conhece seu ID.

Exemplo inseguro:

`GET /pagamentos/150`

Não assumir que o administrador pode acessar o pagamento apenas porque enviou `150`.

Antes de retornar ou modificar informações, verificar se o usuário autenticado possui permissão para acessar aquele recurso.

Essa regra deve ser aplicada principalmente a:

* alunos;
* professores;
* matrículas;
* pagamentos de alunos;
* pagamentos de professores;
* administradores.

---

## 5. Proteção contra SQL Injection

Nunca construir SQL concatenando entrada do usuário.

PROIBIDO:

`"SELECT * FROM aluno WHERE nome = '" + nome + "'"`

Utilizar:

* Spring Data JPA;
* parâmetros de queries;
* PreparedStatement;
* named parameters.

Mesmo quando JPA for utilizado, qualquer query customizada deve utilizar parâmetros.

Valores utilizados para ordenação, filtros dinâmicos ou nomes de campos devem ser validados contra uma lista de valores permitidos.

---

## 6. Validação dos dados

Utilizar Bean Validation sempre que possível.

Exemplos:

`@NotNull`
`@NotBlank`
`@Size`
`@Email`
`@Positive`
`@PositiveOrZero`
`@PastOrPresent`
`@FutureOrPresent`

Nunca depender somente das constraints do PostgreSQL para validar requisições.

As regras também devem existir na camada de aplicação quando forem regras de negócio.

Exemplos:

* valor de pagamento não pode ser negativo;
* data final não pode ser anterior à data inicial;
* matrícula deve existir antes de gerar pagamento;
* professor deve estar vinculado à matéria da matrícula;
* pagamento não pode ser realizado duas vezes;
* pagamento de aluno não pode gerar dois repasses;
* registros inexistentes devem ser rejeitados.

---

## 7. DTOs

Controllers não devem receber entidades JPA diretamente em requests.

Criar DTOs específicos para entrada e saída.

Exemplo:

`CriarAlunoRequest`

em vez de:

`Aluno aluno`

Isso impede que campos internos sejam modificados pelo cliente acidentalmente.

O cliente nunca deve poder controlar diretamente campos como:

* IDs internos;
* senha_hash;
* criado_em;
* atualizado_em;
* administrador recebedor;
* administrador pagador;
* valor_total calculado pelo sistema;
* campos de auditoria.

Esses valores devem ser definidos pelo backend.

---

## 8. Operações financeiras

Operações relacionadas a pagamento devem ser tratadas como operações críticas.

Valores monetários devem utilizar:

`BigDecimal`

Nunca utilizar:

`float`
`double`

para cálculos monetários.

Operações financeiras que alterem múltiplos registros devem utilizar transações.

Exemplo:

`@Transactional`

Uma operação deve ser concluída integralmente ou revertida integralmente em caso de erro.

Nunca deixar um pagamento parcialmente registrado.

---

## 9. Integridade dos pagamentos

Nunca confiar em valores totais calculados pelo frontend.

O backend deve calcular valores financeiros utilizando dados persistidos no banco.

Exemplo:

O frontend não deve enviar:

`valorTotalProfessor = 1200`

e o backend simplesmente aceitar esse valor.

O backend deve determinar o valor total utilizando os pagamentos de alunos associados ao professor.

O frontend pode apresentar cálculos para visualização, mas o backend deve recalculá-los antes da persistência.

---

## 10. Pagamentos duplicados

Toda implementação financeira deve considerar requisições repetidas.

Uma requisição reenviada por:

* duplo clique;
* timeout;
* erro de conexão;
* retry automático;

não pode gerar dois pagamentos ou dois repasses.

Antes de registrar operações, verificar as constraints e regras de unicidade existentes.

Respeitar especialmente:

`UNIQUE (id_matricula, competencia)`

em `pagamento_aluno`.

E:

`UNIQUE (id_pagamento_aluno)`

em `pagamento_professor_item`.

Nunca remover essas restrições apenas para fazer uma operação funcionar.

Se uma restrição impedir uma operação, investigar primeiro a regra de negócio.

---

## 11. Exclusão de dados financeiros

Pagamentos e repasses não devem ser fisicamente excluídos apenas porque foram cancelados ou informados incorretamente.

Evitar:

`DELETE FROM pagamento_aluno`

e:

`DELETE FROM pagamento_professor`

para operações normais do sistema.

Utilizar status como:

* PENDENTE;
* PAGO;
* ATRASADO;
* CANCELADO;
* ESTORNADO.

A movimentação deve continuar disponível para auditoria.

---

## 12. Concorrência

Antes de implementar operações financeiras, considerar que duas requisições podem chegar simultaneamente.

Não utilizar apenas verificações Java como:

`if (!existePagamento) { criarPagamento(); }`

como única proteção contra duplicidade.

Utilizar também constraints do banco.

Quando necessário, utilizar transações e mecanismos apropriados de locking.

A integridade financeira deve continuar garantida mesmo com requisições concorrentes.

---

## 13. Tratamento de erros

Nunca retornar stack trace para o cliente.

Nunca expor:

* SQL;
* nomes internos de tabelas;
* caminhos do servidor;
* configurações;
* variáveis de ambiente;
* tokens;
* credenciais;
* detalhes internos de exceptions.

Utilizar tratamento global de exceções.

Exemplo:

`@RestControllerAdvice`

Retornar mensagens controladas e códigos HTTP apropriados.

Exemplo:

`400 Bad Request`
`401 Unauthorized`
`403 Forbidden`
`404 Not Found`
`409 Conflict`
`422 Unprocessable Entity`
`500 Internal Server Error`

Detalhes internos devem permanecer somente nos logs seguros do servidor.

---

## 14. Logs

Registrar eventos relevantes para auditoria e investigação.

Exemplos:

* autenticação;
* falha de autenticação;
* criação de pagamento;
* alteração de status;
* registro de recebimento;
* registro de repasse;
* tentativa de acesso sem autorização.

Nunca registrar:

* senha;
* senha_hash;
* token completo;
* Authorization header;
* chave privada;
* secret;
* connection string;
* credenciais do banco.

Evitar registrar dados pessoais desnecessariamente.

---

## 15. Secrets

Nunca inserir credenciais diretamente no código.

PROIBIDO:

`password = "minhaSenha123"`

`jwtSecret = "abc123"`

`databasePassword = "postgres"`

Secrets devem vir de configuração externa ou variáveis de ambiente.

Arquivos contendo secrets reais não devem ser enviados para Git.

Arquivos `.env`, chaves privadas e credenciais devem estar protegidos por `.gitignore` quando aplicável.

Criar exemplos utilizando placeholders:

`DB_PASSWORD=${DB_PASSWORD}`

Nunca inventar uma credencial real para facilitar uma implementação.

---

## 16. CORS

Nunca liberar todas as origens sem necessidade.

Evitar configurações equivalentes a:

`allowedOrigins = "*"`

principalmente em ambientes de produção.

Liberar somente os domínios utilizados pelo frontend.

Exemplo conceitual:

`https://sistema.escola.com`

Ambientes de desenvolvimento podem possuir configuração própria.

---

## 17. CSRF

Não desabilitar proteção CSRF automaticamente apenas para resolver erros.

Primeiro identificar como a autenticação da aplicação funciona.

Se a aplicação utilizar sessão/cookies no navegador, manter proteção adequada contra CSRF.

Caso a arquitetura utilize autenticação stateless e tokens enviados explicitamente em headers, avaliar a configuração apropriada do Spring Security.

Qualquer alteração relacionada a CSRF deve possuir justificativa técnica.

---

## 18. XSS

Dados informados por usuários nunca devem ser tratados automaticamente como HTML confiável.

Evitar renderizar conteúdo recebido do backend através de mecanismos equivalentes a `innerHTML`.

Quando conteúdo HTML realmente for necessário, utilizar sanitização adequada.

Nomes, observações, descrições, alunos, professores e matérias devem ser tratados como texto por padrão.

---

## 19. Mass Assignment

Nunca permitir que o cliente controle campos internos simplesmente enviando campos adicionais no JSON.

Exemplo de requisição maliciosa:

{
"nome": "Teste",
"nivelAcesso": "ADMIN",
"ativo": true
}

Se o endpoint deveria permitir somente alteração de nome, os outros campos devem ser ignorados/rejeitados.

Utilizar DTOs específicos para cada operação.

---

## 20. Segurança das entidades JPA

Evitar serializar diretamente entidades JPA para respostas da API.

Além de problemas de arquitetura, isso pode expor relacionamentos ou informações que não deveriam chegar ao usuário.

Utilizar DTOs de resposta.

Não utilizar `EAGER` indiscriminadamente apenas para resolver problemas de serialização.

---

## 21. Banco de dados

Nunca desabilitar Foreign Keys, UNIQUE constraints ou CHECK constraints apenas para facilitar uma implementação.

Alterações estruturais devem ser realizadas através de migrations.

Nunca modificar manualmente o banco de produção para corrigir uma feature.

Se o projeto utilizar Flyway:

`V1__...`
`V2__...`
`V3__...`

Uma migration já aplicada não deve ser modificada.

Criar uma nova migration para alterações futuras.

---

## 22. Princípio do menor privilégio

Usuários, serviços e banco de dados devem possuir somente as permissões necessárias.

A aplicação não deve utilizar superusuário do PostgreSQL em produção.

A conta utilizada pela aplicação deve possuir apenas as permissões necessárias para executar o sistema.

---

## 23. Dependências

Antes de adicionar uma dependência:

1. verificar se ela é realmente necessária;
2. preferir bibliotecas amplamente mantidas;
3. evitar dependências abandonadas;
4. não adicionar bibliotecas apenas para funções simples;
5. verificar vulnerabilidades conhecidas quando possível.

Nunca reduzir a versão de uma biblioteca de segurança para contornar incompatibilidades sem justificar a mudança.

---

## 24. Endpoints administrativos

Endpoints administrativos devem exigir autenticação e autorização explicitamente.

Nunca criar temporariamente endpoints como:

`/admin/delete-all`
`/debug/users`
`/debug/payments`
`/test/login`

sem proteção.

Código de debug não deve permanecer acessível em produção.

---

## 25. Rate limiting e autenticação

Endpoints de autenticação devem possuir proteção contra tentativas automatizadas quando a aplicação for disponibilizada em rede.

Considerar:

* rate limiting;
* atrasos progressivos;
* bloqueios temporários;
* monitoramento de tentativas suspeitas.

Nunca revelar através da mensagem de login se determinado email existe no sistema.

Preferir respostas genéricas como:

`Email ou senha inválidos.`

---

## 26. Alterações feitas por IA

Antes de finalizar qualquer implementação, a IA deve revisar especificamente:

* autenticação;
* autorização;
* validação de entrada;
* acesso indevido por IDs;
* SQL Injection;
* XSS;
* CSRF;
* CORS;
* exposição de informações;
* armazenamento de senhas;
* secrets;
* duplicidade de pagamentos;
* concorrência;
* integridade financeira;
* tratamento de exceções;
* logs;
* constraints do banco.

A IA não deve reduzir controles de segurança existentes para solucionar problemas de implementação.

Exemplos proibidos:

* liberar `permitAll()` para resolver erro 403;
* desabilitar CSRF sem analisar a arquitetura;
* utilizar CORS `*` para resolver erro do frontend;
* remover uma constraint UNIQUE para resolver duplicidade;
* remover autenticação temporariamente;
* aceitar diretamente entidade JPA no controller;
* concatenar SQL;
* desativar validações;
* armazenar senha sem hash.

Se uma proteção estiver impedindo uma funcionalidade, corrigir a integração em vez de remover a proteção.

---

## 27. Segurança por padrão

Quando houver mais de uma solução possível, escolher a alternativa que:

1. conceda menos permissões;
2. exponha menos dados;
3. aceite menos entradas inválidas;
4. preserve maior rastreabilidade;
5. mantenha maior integridade financeira;
6. dependa menos de confiança no frontend.

Na dúvida entre liberar ou restringir acesso, manter o acesso restrito até que a regra de negócio esteja clara.

---

## 28. Critério de conclusão

Uma funcionalidade envolvendo dados sensíveis ou pagamentos não deve ser considerada concluída apenas porque funciona no cenário esperado.

Também devem ser considerados pelo menos:

* requisição sem autenticação;
* usuário sem permissão;
* ID inexistente;
* ID válido porém não autorizado;
* campos ausentes;
* valores negativos;
* strings excessivamente grandes;
* requisição duplicada;
* requisições concorrentes;
* payload com campos extras;
* tentativa de SQL Injection;
* estados financeiros inválidos.

Sempre que possível, criar testes automatizados para esses casos.
