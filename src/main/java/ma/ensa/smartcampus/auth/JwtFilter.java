package ma.ensa.smartcampus.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import ma.ensa.smartcampus.service.AccountService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountService accounts;

    public JwtFilter(JwtService jwtService, AccountService accounts) {
        this.jwtService = jwtService;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String email = jwtService.parse(header.substring(7)).getSubject();
                accounts.activeRole(email).ifPresent(role -> {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(email, null, authorities));
                });
            } catch (JwtException | IllegalArgumentException ignored) {
                // Spring Security handles anonymous requests on protected routes.
            }
        }
        chain.doFilter(request, response);
    }
}
