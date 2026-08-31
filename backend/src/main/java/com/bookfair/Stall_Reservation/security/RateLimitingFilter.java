package com.bookfair.Stall_Reservation.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RateLimitingFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Determine Endpoint Category & Policy
        String category = getEndpointCategory(path, method, request);
        if (category == null) {
            // Bypass rate limiting for all other endpoints
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Determine Rate Limit Key
        String key = resolveRateLimitKey(request, category);
        if (key == null) {
            // Safe fallback if key cannot be resolved
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Retrieve or Create Bucket Atomically
        Bucket bucket = cache.get(key, k -> createNewBucket(category));

        // 4. Consume Token & Evaluate Limit
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            // Calculate Retry-After header in seconds (rounded UP)
            long waitTimeNanos = probe.getNanosToWaitForRefill();
            long waitTimeSeconds = (long) Math.ceil(waitTimeNanos / 1_000_000_000.0);
            if (waitTimeSeconds <= 0) {
                waitTimeSeconds = 1;
            }

            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(waitTimeSeconds));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\": \"Too many requests. Please try again later.\", \"error\": \"Too Many Requests\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getEndpointCategory(String path, String method, HttpServletRequest request) {
        // A. Booking Creation
        if ("/api/reservations/book".equals(path) && "POST".equalsIgnoreCase(method)) {
            return "book";
        }
        // B. Booking Cancellation
        if (path.startsWith("/api/reservations/") && path.endsWith("/cancel") && "POST".equalsIgnoreCase(method)) {
            return "cancel";
        }
        // C. Profile Modification
        if (("/api/profile".equals(path) || "/api/admin/profile".equals(path)) && "PUT".equalsIgnoreCase(method)) {
            return "profile";
        }
        // D. WebSocket Handshake Attempts
        if (path.startsWith("/ws/")) {
            // Only limit initial handshake info request or WebSocket upgrade requests
            boolean isInfo = path.endsWith("/info");
            boolean isUpgrade = "websocket".equalsIgnoreCase(request.getHeader("Upgrade"));
            if (isInfo || isUpgrade) {
                return "ws";
            }
        }
        // E. Public GET Endpoints
        if (path.startsWith("/api/public/") && "GET".equalsIgnoreCase(method)) {
            return "public";
        }

        return null;
    }

    private String resolveRateLimitKey(HttpServletRequest request, String category) {
        // If the category is authenticated (book, cancel, profile), extract sub
        if ("book".equals(category) || "cancel".equals(category) || "profile".equals(category)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                String sub = jwtAuth.getToken().getSubject();
                if (sub != null) {
                    return "auth:" + sub + ":" + category;
                }
            }
            // Fallback to IP if token is missing or not authenticated
            return "ip:" + request.getRemoteAddr() + ":" + category;
        }

        // For public and ws, limit strictly by IP address (remoteAddr)
        return "ip:" + request.getRemoteAddr() + ":" + category;
    }

    private Bucket createNewBucket(String category) {
        Bandwidth limit = switch (category) {
            case "book" -> Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(1)));
            case "cancel" -> Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
            case "profile" -> Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
            case "ws" -> Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
            default -> Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))); // public
        };
        return Bucket.builder().addLimit(limit).build();
    }
}
