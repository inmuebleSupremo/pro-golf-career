package com.progolf.app.api;

import com.progolf.app.persistence.SaveConflictException;
import com.progolf.app.persistence.SaveNotFoundException;
import com.progolf.app.world.CompletedWithoutAutosaveException;
import com.progolf.app.world.WorldSessionNotFoundException;
import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Translates known engine-boundary failures into typed GraphQL errors (capability graphql-api, design D5 /
 * mutations D4): an unknown world session or save becomes a {@link ErrorType#NOT_FOUND} error, a stale save
 * becomes CONFLICT, and client-fault engine failures become {@link ErrorType#BAD_REQUEST}. Unmapped exceptions
 * fall through to Spring GraphQL's default handling.
 */
@Component
public class GraphQlErrorResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorClassification type = classify(ex);
        if (type == null) {
            return null;
        }
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(ex.getMessage())
                .build();
    }

    private static ErrorClassification classify(Throwable ex) {
        if (ex instanceof CompletedWithoutAutosaveException) {
            return ErrorClassification.errorClassification("COMPLETED_UNSAVED");
        }
        if (ex instanceof SaveConflictException) {
            return ErrorClassification.errorClassification("CONFLICT");
        }
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
