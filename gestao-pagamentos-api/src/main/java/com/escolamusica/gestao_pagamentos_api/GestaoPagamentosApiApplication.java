package com.escolamusica.gestao_pagamentos_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GestaoPagamentosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestaoPagamentosApiApplication.class, args);
	}

}
