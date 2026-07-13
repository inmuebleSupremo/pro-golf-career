package com.progolf.app.api;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL runtime wiring (capability graphql-api): registers the {@code Long} scalar declared in the schema,
 * which the engine needs for ids/seeds/targets that exceed GraphQL's 32-bit {@code Int}. Confined to the
 * application layer.
 */
@Configuration
public class GraphQlConfig {

    @Bean
    RuntimeWiringConfigurer longScalarConfigurer() {
        return wiring -> wiring.scalar(ExtendedScalars.GraphQLLong);
    }
}
