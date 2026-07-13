package com.progolf.app.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;

/**
 * graphql-api (design D2): the DTO boundary — no simulation-engine ({@code com.progolf.sim.*}) type may appear
 * as a GraphQL resolver's return type (nor a list element type). Resolvers must project engine reads into
 * application DTOs. This is a pure reflection guard over the resolver controllers.
 */
class ApiBoundaryTest {

    private static final List<Class<?>> RESOLVERS =
            List.of(WorldQueryController.class, WorldMutationController.class);

    @Test
    void noResolverReturnTypeIsASimulationEngineType() {
        List<String> violations = new ArrayList<>();
        for (Class<?> resolver : RESOLVERS) {
            for (Method m : resolver.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(QueryMapping.class) && !m.isAnnotationPresent(MutationMapping.class)) {
                    continue;
                }
                for (Class<?> exposed : exposedTypes(m.getGenericReturnType())) {
                    if (exposed.getName().startsWith("com.progolf.sim.")) {
                        violations.add(resolver.getSimpleName() + "." + m.getName() + " -> " + exposed.getName());
                    }
                }
            }
        }
        assertThat(violations).as("resolvers must return DTOs, never sim.* types").isEmpty();
    }

    /** The raw type plus any generic element types (e.g. the element of a List). */
    private static List<Class<?>> exposedTypes(Type type) {
        List<Class<?>> types = new ArrayList<>();
        if (type instanceof Class<?> c) {
            types.add(c);
        } else if (type instanceof ParameterizedType p) {
            if (p.getRawType() instanceof Class<?> raw) {
                types.add(raw);
            }
            for (Type arg : p.getActualTypeArguments()) {
                types.addAll(exposedTypes(arg));
            }
        }
        return types;
    }
}
