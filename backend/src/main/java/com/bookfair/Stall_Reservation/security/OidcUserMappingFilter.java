package com.bookfair.Stall_Reservation.security;

import com.bookfair.Stall_Reservation.entity.User;
import com.bookfair.Stall_Reservation.enums.UserRole;
import com.bookfair.Stall_Reservation.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class OidcUserMappingFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public OidcUserMappingFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String sub = jwt.getSubject();
            String email = jwt.getClaimAsString("https://bookfair-app/email");
            if (email == null) email = jwt.getClaimAsString("email");

            String name = jwt.getClaimAsString("https://bookfair-app/name");
            if (name == null) name = jwt.getClaimAsString("name");
            if (name == null) name = email;

            final String finalEmail = email;
            final String finalName = name;

            if (sub != null) {
                // Find local user by sub, or by email if email is present
                java.util.Optional<User> existingUser = userRepository.findBySub(sub);
                if (existingUser.isEmpty() && finalEmail != null) {
                    existingUser = userRepository.findByEmail(finalEmail);
                }

                User user = existingUser.orElseGet(() -> {
                    if (finalEmail == null) {
                        return null; // Cannot create a new user profile without an email
                    }
                    User newUser = new User();
                    newUser.setSub(sub);
                    newUser.setEmail(finalEmail);
                    newUser.setName(finalName != null ? finalName : finalEmail);
                    newUser.setPhone("");
                    
                    // Map role from groups or roles claim
                    List<String> roles = jwt.getClaimAsStringList("https://bookfair-app/roles");
                    if (roles != null && (roles.contains("EXHIBITION_ORGANIZER") || roles.contains("ROLE_EXHIBITION_ORGANIZER"))) {
                        newUser.setRole(UserRole.EXHIBITION_ORGANIZER);
                    } else {
                        newUser.setRole(UserRole.STALL_VENDOR);
                    }
                    return userRepository.save(newUser);
                });

                if (user == null) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"message\":\"OIDC email claim is missing and local profile does not exist. Please configure your Identity Provider to include email in the Access Token.\"}"
                    );
                    return;
                }

                if (!user.isActive()) {
                    SecurityContextHolder.clearContext();

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"message\":\"Account is deactivated\"}"
                    );

                    return;
                }

                List<String> tokenRoles =
                        jwt.getClaimAsStringList("https://bookfair-app/roles");

                UserRole tokenRole =
                        tokenRoles != null &&
                                (tokenRoles.contains("EXHIBITION_ORGANIZER") ||
                                        tokenRoles.contains("ROLE_EXHIBITION_ORGANIZER"))
                                ? UserRole.EXHIBITION_ORGANIZER
                                : UserRole.STALL_VENDOR;
                // Link sub to user profile if not set yet
                boolean userUpdated = false;

                if (user.getSub() == null) {
                    user.setSub(sub);
                    userUpdated = true;
                }

                if (user.getRole() != tokenRole) {
                    user.setRole(tokenRole);
                    userUpdated = true;
                }

                if (userUpdated) {
                    userRepository.save(user);
                }

                // Map authorities from access token claims
                var newAuth = new UsernamePasswordAuthenticationToken(
                        user.getId(),
                        null,
                        jwtAuth.getAuthorities()
                );
                newAuth.setDetails(jwtAuth.getDetails());
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
