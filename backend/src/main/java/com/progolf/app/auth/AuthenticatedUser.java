package com.progolf.app.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the current authenticated user id from the security context (spec: resource-ownership). The id is the
 * authentication name — for a JWT-authenticated request that is the token subject set by the resource server,
 * i.e. the {@code UserAccount} id. Resolvers call this to obtain the {@code ownerId} they pass into
 * {@code WorldService}; the owner is always derived from the token, never from client input.
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    /**
     * The current user id.
     *
     * @throws IllegalStateException if there is no authenticated user (the security filter should have
     *     rejected the request before it reached a resolver)
     */
    public static String requireId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user in the security context");
        }
        return authentication.getName();
    }
}
