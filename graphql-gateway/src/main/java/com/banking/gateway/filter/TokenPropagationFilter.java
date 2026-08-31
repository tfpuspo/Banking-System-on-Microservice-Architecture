package com.banking.gateway.filter;

import com.banking.gateway.util.RequestTokenHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the incoming "Authorization: Bearer <token>" header (if present)
 * and stashes just the token into RequestTokenHolder, where a gRPC
 * ClientInterceptor picks it up and forwards it as metadata on every
 * outgoing call to auth-service and account-service — this is the start of
 * the propagation chain described in PHASE4_NOTES.md.
 */
@Component
public class TokenPropagationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                RequestTokenHolder.set(header.substring("Bearer ".length()));
            }
            chain.doFilter(request, response);
        } finally {
            RequestTokenHolder.clear(); // always clean up, even on error
        }
    }
}
