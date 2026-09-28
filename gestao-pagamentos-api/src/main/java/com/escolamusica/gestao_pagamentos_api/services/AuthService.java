package com.escolamusica.gestao_pagamentos_api.services;

import com.escolamusica.gestao_pagamentos_api.dtos.LoginRequest;
import com.escolamusica.gestao_pagamentos_api.dtos.LoginResponse;
import com.escolamusica.gestao_pagamentos_api.exception.InvalidCredentialsException;
import com.escolamusica.gestao_pagamentos_api.exception.InvalidRequestException;
import com.escolamusica.gestao_pagamentos_api.security.EmailNormalizer;
import com.escolamusica.gestao_pagamentos_api.security.SecurityUser;
import com.escolamusica.gestao_pagamentos_api.security.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException();
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            EmailNormalizer.normalize(request.email()),
                            request.password()
                    )
            );

            if (!(authentication.getPrincipal() instanceof SecurityUser user)) {
                throw new InvalidCredentialsException();
            }

            return new LoginResponse(
                    tokenService.createAccessToken(user),
                    "Bearer",
                    tokenService.accessTokenExpiresInSeconds()
            );
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }
}
