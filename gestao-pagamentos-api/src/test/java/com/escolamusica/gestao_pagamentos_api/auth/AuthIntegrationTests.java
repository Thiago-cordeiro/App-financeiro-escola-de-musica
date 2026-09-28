package com.escolamusica.gestao_pagamentos_api.auth;

import com.escolamusica.gestao_pagamentos_api.models.Administrador;
import com.escolamusica.gestao_pagamentos_api.models.Role;
import com.escolamusica.gestao_pagamentos_api.repositories.AdministradorRepository;
import com.escolamusica.gestao_pagamentos_api.security.JwtProperties;
import com.escolamusica.gestao_pagamentos_api.security.SecurityUser;
import com.escolamusica.gestao_pagamentos_api.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {

    private static final String EMAIL = "admin@escolamusica.com";
    private static final String PASSWORD = "senha-segura";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        administradorRepository.deleteAll();

        Administrador administrador = new Administrador();
        administrador.setNome("Administrador");
        administrador.setEmail(EMAIL);
        administrador.setSenhaHash(passwordEncoder.encode(PASSWORD));
        administrador.setRole(Role.ADMIN);
        administrador.setAtivo(true);
        administradorRepository.saveAndFlush(administrador);
    }

    @Test
    void loginValidoRetornaJwtSemExporSenha() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ADMIN@ESCOLAMUSICA.COM","password":"senha-segura"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response)
                .get("accessToken")
                .stringValue();
        var jwt = jwtDecoder.decode(token);

        Administrador persisted = administradorRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();

        assertThat(jwt.getSubject()).isNotBlank();
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(jwtProperties.issuer());
        assertThat(jwt.getAudience()).containsExactly(jwtProperties.audience());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(persisted.getSenhaHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, persisted.getSenhaHash())).isTrue();
    }

    @Test
    void senhaIncorretaEEmailInexistenteRetornamMesmaResposta() throws Exception {
        String wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@escolamusica.com","password":"senha-incorreta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"))
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"naoexiste@escolamusica.com","password":"senha-incorreta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"))
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongPassword).contains("Credenciais inválidas");
        assertThat(unknownEmail).contains("Credenciais inválidas");
    }

    @Test
    void payloadInvalidoRetornaBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"email-invalido","password":"curta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"));
    }

    @Test
    void endpointPublicoCriaAdministradorDeTesteComSenhaProtegida() throws Exception {
        mockMvc.perform(post("/api/test/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Admin de teste",
                                  "email":"NOVO@EXEMPLO.COM",
                                  "password":"senha-de-teste"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Admin de teste"))
                .andExpect(jsonPath("$.email").value("novo@exemplo.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Administrador persisted = administradorRepository
                .findByEmailIgnoreCase("novo@exemplo.com")
                .orElseThrow();
        assertThat(persisted.getSenhaHash()).isNotEqualTo("senha-de-teste");
        assertThat(passwordEncoder.matches("senha-de-teste", persisted.getSenhaHash())).isTrue();
    }

    @Test
    void endpointPublicoRejeitaEmailDeAdministradorDuplicado() throws Exception {
        mockMvc.perform(post("/api/test/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Outro admin",
                                  "email":"ADMIN@ESCOLAMUSICA.COM",
                                  "password":"senha-de-teste"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe um administrador com este email"));
    }

    @Test
    void rotaAdministrativaSemTokenRetornaUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/recurso"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticação necessária"));
    }

    @Test
    void roleSemPermissaoRetornaForbidden() throws Exception {
        String token = token(jwtEncoder, jwtProperties.issuer(), Instant.now().plusSeconds(600), "PROFESSOR");

        mockMvc.perform(get("/api/admin/recurso").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado"));
    }

    @Test
    void tokensExpiradoComIssuerInvalidoOuAssinaturaInvalidaRetornamUnauthorized() throws Exception {
        String expired = token(jwtEncoder, jwtProperties.issuer(), Instant.now().minusSeconds(60), "ADMIN");
        String wrongIssuer = token(jwtEncoder, "outro-servico", Instant.now().plusSeconds(600), "ADMIN");
        String invalidSignature = token(newEncoder(), jwtProperties.issuer(), Instant.now().plusSeconds(600), "ADMIN");

        for (String token : List.of(expired, wrongIssuer, invalidSignature)) {
            mockMvc.perform(get("/api/admin/recurso").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Autenticação necessária"));
        }
    }

    @Test
    void swaggerPublicaLoginEEsquemaBearer() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/test/admins'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    private String token(JwtEncoder encoder, String issuer, Instant expiresAt, String role) {
        Instant issuedAt = expiresAt.isBefore(Instant.now())
                ? expiresAt.minusSeconds(600)
                : Instant.now().minusSeconds(1);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("1")
                .audience(List.of(jwtProperties.audience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of(role))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
        )).getTokenValue();
    }

    private JwtEncoder newEncoder() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }
}
