package com.swiftroute.service;

import com.swiftroute.domain.repository.UserRepository;
import com.swiftroute.dto.request.LoginRequest;
import com.swiftroute.dto.request.RefreshTokenRequest;
import com.swiftroute.dto.response.AuthResponse;
import com.swiftroute.dto.response.UserResponse;
import com.swiftroute.exception.BusinessRuleException;
import com.swiftroute.security.CustomUserDetailsService;
import com.swiftroute.security.JwtTokenProvider;
import com.swiftroute.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Value("${swiftroute.jwt.access-token-expiration-ms:86400000}")
    private long accessTokenExpirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       CustomUserDetailsService userDetailsService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshToken = tokenProvider.generateRefreshToken(principal);

        UserResponse userResponse = mapToUserResponse(principal);
        long expiresInSeconds = accessTokenExpirationMs / 1000;

        return AuthResponse.of(accessToken, refreshToken, expiresInSeconds, userResponse);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!tokenProvider.validateToken(token)) {
            throw new BusinessRuleException("Invalid or expired refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(token);
        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);

        String newAccessToken = tokenProvider.generateAccessToken(principal);
        long expiresInSeconds = accessTokenExpirationMs / 1000;

        return AuthResponse.of(newAccessToken, token, expiresInSeconds, mapToUserResponse(principal));
    }

    public UserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new org.springframework.security.authentication.BadCredentialsException("No authenticated user in context");
        }
        return mapToUserResponse(principal);
    }

    private UserResponse mapToUserResponse(UserPrincipal principal) {
        String role = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_TECHNICIAN");

        return new UserResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getEmail(),
                principal.getFullName(),
                role
        );
    }
}
