package com.banking.auth.grpc;

import com.banking.auth.entity.SessionEntity;
import com.banking.auth.entity.UserEntity;
import com.banking.auth.repository.RolePermissionJpaRepository;
import com.banking.auth.repository.SessionJpaRepository;
import com.banking.auth.repository.UserJpaRepository;
import com.banking.auth.entity.RolePermissionEntity;
import com.banking.auth.util.JwtUtil;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 days

    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    private final UserJpaRepository userRepository;
    private final SessionJpaRepository sessionRepository;
    private final RolePermissionJpaRepository permissionRepository;
    private final JwtUtil jwtUtil;

    public AuthGrpcService(
            UserJpaRepository userRepository,
            SessionJpaRepository sessionRepository,
            RolePermissionJpaRepository permissionRepository,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.permissionRepository = permissionRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        if (request.getEmail().isBlank() || request.getPassword().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("email and password are required").asRuntimeException());
            return;
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription("an account with this email already exists").asRuntimeException());
            return;
        }

        String hash = passwordEncoder.encode(request.getPassword());
        UserEntity user = new UserEntity(request.getEmail(), hash, request.getFullName());
        userRepository.save(user);

        responseObserver.onNext(RegisterResponse.newBuilder()
            .setUserId(user.getId().toString())
            .setEmail(user.getEmail())
            .build());
        responseObserver.onCompleted();
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(request.getEmail());

        // Deliberately vague error either way — don't reveal whether the email exists
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription("invalid email or password").asRuntimeException());
            return;
        }

        responseObserver.onNext(issueTokenPair(userOpt.get()));
        responseObserver.onCompleted();
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<LoginResponse> responseObserver) {
        Optional<SessionEntity> sessionOpt = sessionRepository.findByRefreshToken(request.getRefreshToken());

        if (sessionOpt.isEmpty() || sessionOpt.get().isRevoked()
                || sessionOpt.get().getExpiresAt().isBefore(Instant.now())) {
            responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription("refresh token is invalid, expired, or revoked").asRuntimeException());
            return;
        }

        // rotate: invalidate the old refresh token, issue a fresh pair
        SessionEntity oldSession = sessionOpt.get();
        oldSession.setRevoked(true);
        sessionRepository.save(oldSession);

        UserEntity user = userRepository.findById(oldSession.getUserId())
            .orElseThrow(() -> Status.NOT_FOUND.withDescription("user no longer exists").asRuntimeException());

        responseObserver.onNext(issueTokenPair(user));
        responseObserver.onCompleted();
    }

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        JwtUtil.ParsedToken parsed = jwtUtil.verify(request.getAccessToken());

        ValidateTokenResponse response;
        if (parsed == null) {
            response = ValidateTokenResponse.newBuilder().setValid(false).build();
        } else {
            response = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId(parsed.userId())
                .addAllRoles(parsed.roles() == null ? List.of() : parsed.roles())
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void authorize(AuthorizeRequest request, StreamObserver<AuthorizeResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("user not found").asRuntimeException());

            // "account:1234" -> resourceType="account", resourceId="1234"
            // A bare resource with no colon (e.g. "reports") is treated as
            // the whole resourceType, with no specific ID to check ownership against.
            String resource = request.getResource();
            String resourceType;
            String resourceId;
            int colonIndex = resource.indexOf(':');
            if (colonIndex >= 0) {
                resourceType = resource.substring(0, colonIndex);
                resourceId = resource.substring(colonIndex + 1);
            } else {
                resourceType = resource;
                resourceId = "";
            }

            List<RolePermissionEntity> grants = permissionRepository.findByRoleIn(user.getRoles());

            boolean matchesResourceAndAction = false;
            boolean hasUnrestrictedGrant = false;
            boolean hasOwnershipGrant = false;

            for (RolePermissionEntity grant : grants) {
                boolean resourceMatches = grant.getResourceType().equals("*") || grant.getResourceType().equals(resourceType);
                boolean actionMatches = grant.getAction().equals("*") || grant.getAction().equals(request.getAction());
                if (!resourceMatches || !actionMatches) continue;

                matchesResourceAndAction = true;
                if (!grant.isOwnershipRequired()) {
                    hasUnrestrictedGrant = true;
                } else {
                    hasOwnershipGrant = true;
                }
            }

            if (!matchesResourceAndAction) {
                respondAuthorize(responseObserver, false, "no role grants " + request.getAction() + " on " + resourceType);
                return;
            }

            if (hasUnrestrictedGrant) {
                respondAuthorize(responseObserver, true, "permitted");
                return;
            }

            if (hasOwnershipGrant) {
                if (resourceType.equals("user")) {
                    // "user" is the one resource type auth-service itself owns data
                    // for, so it can verify ownership directly.
                    boolean isOwn = resourceId.equals(userId.toString());
                    respondAuthorize(responseObserver, isOwn,
                        isOwn ? "permitted (own resource)" : "not your own resource");
                } else {
                    // Phase 4 update: account-service now exists and holds the
                    // real ownership data for "account" resources — it does its
                    // own local ownership check (see AccountGrpcService.getAccount).
                    // auth-service's job here is only to confirm the role has
                    // this CAPABILITY at all; instance-level ownership is the
                    // owning service's responsibility, since only it has the data.
                    respondAuthorize(responseObserver, true,
                        "role permits " + request.getAction() + " on " + resourceType
                            + "; caller must verify resource ownership locally");
                }
                return;
            }

            respondAuthorize(responseObserver, false, "no matching grant");
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("invalid user_id").asRuntimeException());
        }
    }

    private void respondAuthorize(StreamObserver<AuthorizeResponse> responseObserver, boolean allowed, String reason) {
        responseObserver.onNext(AuthorizeResponse.newBuilder()
            .setAllowed(allowed)
            .setReason(reason)
            .build());
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        sessionRepository.findByRefreshToken(request.getRefreshToken()).ifPresent(session -> {
            session.setRevoked(true);
            sessionRepository.save(session);
        });
        responseObserver.onNext(LogoutResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    private LoginResponse issueTokenPair(UserEntity user) {
        String accessToken = jwtUtil.issueAccessToken(user.getId(), user.getRoles());

        String refreshToken = generateOpaqueToken();
        Instant expiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_TTL_SECONDS);
        sessionRepository.save(new SessionEntity(user.getId(), refreshToken, expiresAt));

        return LoginResponse.newBuilder()
            .setAccessToken(accessToken)
            .setRefreshToken(refreshToken)
            .setExpiresIn(jwtUtil.getTtlSeconds())
            .build();
    }

    /** A random, unguessable string — not a JWT, just an opaque lookup key stored in the sessions table. */
    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
