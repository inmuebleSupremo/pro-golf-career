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
 * Translates known engine-boundary failures into typed GraphQL errors (capability graphql-api, design D5 /
 * mutations D4): an unknown world session or save becomes a {@link ErrorType#NOT_FOUND} error; client-fault
 * engine failures (no player assigned, acting off-event, an out-of-range index, a bad enum name) become a
 * {@link ErrorType#BAD_REQUEST} error rather than an opaque internal error. Unmapped exceptions fall through
 * to Spring GraphQL's default handling.
 */
@Component
public class GraphQlErrorResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorType type = classify(ex);
        if (type == null) {
            return null;
        }
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(ex.getMessage())
                .build();
    }

    private static ErrorType classify(Throwable ex) {
        if (ex instanceof WorldSessionNotFoundException || ex instanceof SaveNotFoundException) {
            return ErrorType.NOT_FOUND;
        }
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException
                || ex instanceof IndexOutOfBoundsException) {
            return ErrorType.BAD_REQUEST;
        }
        return null;
    }
}
