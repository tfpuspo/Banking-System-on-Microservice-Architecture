package com.banking.gateway;

import com.banking.auth.grpc.*;
import com.banking.gateway.dto.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AuthGraphQlController {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    @MutationMapping
    public RegisterResult register(@Argument String email, @Argument String password, @Argument String fullName) {
        RegisterResponse res = authStub.register(RegisterRequest.newBuilder()
            .setEmail(email)
            .setPassword(password)
            .setFullName(fullName)
            .build());
        return new RegisterResult(res.getUserId(), res.getEmail());
    }

    @MutationMapping
    public AuthPayload login(@Argument String email, @Argument String password) {
        LoginResponse res = authStub.login(LoginRequest.newBuilder()
            .setEmail(email)
            .setPassword(password)
            .build());
        return new AuthPayload(res.getAccessToken(), res.getRefreshToken(), res.getExpiresIn());
    }

    @MutationMapping
    public AuthPayload refreshToken(@Argument String refreshToken) {
        LoginResponse res = authStub.refreshToken(RefreshTokenRequest.newBuilder()
            .setRefreshToken(refreshToken)
            .build());
        return new AuthPayload(res.getAccessToken(), res.getRefreshToken(), res.getExpiresIn());
    }

    @MutationMapping
    public LogoutResult logout(@Argument String refreshToken) {
        LogoutResponse res = authStub.logout(LogoutRequest.newBuilder()
            .setRefreshToken(refreshToken)
            .build());
        return new LogoutResult(res.getSuccess());
    }

    @QueryMapping
    public ValidateTokenResult validateToken(@Argument String accessToken) {
        ValidateTokenResponse res = authStub.validateToken(ValidateTokenRequest.newBuilder()
            .setAccessToken(accessToken)
            .build());
        return new ValidateTokenResult(res.getValid(), res.getUserId(), res.getRolesList());
    }

    @QueryMapping
    public AuthorizeResult authorize(@Argument String userId, @Argument String resource, @Argument String action) {
        AuthorizeResponse res = authStub.authorize(AuthorizeRequest.newBuilder()
            .setUserId(userId)
            .setResource(resource)
            .setAction(action)
            .build());
        return new AuthorizeResult(res.getAllowed(), res.getReason());
    }
}
