package com.progolf.app.api;

import com.progolf.app.persistence.SaveNotFoundException;
import com.progolf.app.world.WorldSessionNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Translates known engine-boundary failures into typed GraphQL errors (capability graphql-api, design D5):
 * an unknown world session or an unknown save becomes a {@link ErrorType#NOT_FOUND}-classified error rather
 * than an opaque internal error. Unmapped exceptions fall through to Spring GraphQL's default handling.
 */
@Component
public class GraphQlErrorResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof WorldSessionNotFoundException || ex instanceof SaveNotFoundException) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.NOT_FOUND)
                    .message(ex.getMessage())
                    .build();
        }
        return null;
    }
}
