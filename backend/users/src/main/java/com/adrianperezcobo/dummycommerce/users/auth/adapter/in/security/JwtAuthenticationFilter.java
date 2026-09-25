package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.security;

import com.adrianperezcobo.dummycommerce.users.auth.application.exception.InvalidAccessTokenException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.AccessTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final AccessTokenPort accessTokenPort;

    public JwtAuthenticationFilter(
            AccessTokenPort accessTokenPort
    ) {
        this.accessTokenPort = accessTokenPort;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader("Authorization");

        if (
                authorization == null ||
                        !authorization.startsWith("Bearer ")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {
            AuthenticatedUser user =
                    accessTokenPort.parse(token);

            var authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + user.role().name()
                    );

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (InvalidAccessTokenException ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}