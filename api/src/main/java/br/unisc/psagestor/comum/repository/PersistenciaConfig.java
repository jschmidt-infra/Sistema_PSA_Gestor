package br.unisc.psagestor.comum.repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
        basePackages = "br.unisc.psagestor",
        repositoryBaseClass = EntidadeRepositoryImpl.class)
public class PersistenciaConfig {}
