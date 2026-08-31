package com.banking.gateway;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.StatusRuntimeException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GrpcExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected List<GraphQLError> resolveToMultipleErrors(Throwable exception, DataFetchingEnvironment env) {
        if (exception instanceof StatusRuntimeException sre) {
            String message = sre.getStatus().getDescription() != null
                ? sre.getStatus().getDescription()
                : sre.getMessage();

            return List.of(GraphqlErrorBuilder.newError(env)
                .message(message)
                .errorType(ErrorType.BAD_REQUEST)
                .build());
        }
        return null; // let Spring's default handling take over for anything else
    }
}
