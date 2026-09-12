package com.udhr.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the authenticated principal (staff number or patient ID number)
 * set by {@link JwtAuthenticationFilter}, so services never have to trust an
 * identity supplied in a request body.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (String) auth.getPrincipal() : null;
    }
}
