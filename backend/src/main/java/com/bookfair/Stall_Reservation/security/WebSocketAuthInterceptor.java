package com.bookfair.Stall_Reservation.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    public WebSocketAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);

        // 1. Authenticate CONNECT command
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorization =
                    accessor.getFirstNativeHeader("Authorization");

            if (authorization == null ||
                    !authorization.startsWith("Bearer ")) {

                throw new IllegalArgumentException(
                        "Missing WebSocket authorization token"
                );
            }

            String token = authorization.substring(7);

            // Uses the same JwtDecoder configured for Auth0.
            // Therefore issuer, signature, expiry and audience validation are applied.
            Jwt jwt = jwtDecoder.decode(token);

            JwtAuthenticationToken authentication =
                    new JwtAuthenticationToken(jwt);

            accessor.setUser(authentication);
        }

        // 2. Reject all client SEND messages to make WebSocket strictly read-only
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            throw new AccessDeniedException(
                    "Publishing messages is not allowed for clients"
            );
        }

        // 3. Enforce SUBSCRIBE destination controls
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            String destination = accessor.getDestination();
            if (destination == null) {
                throw new IllegalArgumentException("Missing subscription destination");
            }

            // A. Restrict subscription to allowed topics
            if (destination.startsWith("/topic/stalls/")) {
                // Require an authenticated WebSocket user
                if (!(accessor.getUser() instanceof JwtAuthenticationToken)) {
                    throw new AccessDeniedException(
                            "Authentication required for stall WebSocket topic"
                    );
                }

                // Ensure event ID path variable is strictly numeric to reject path manipulation
                String eventIdStr = destination.substring("/topic/stalls/".length());
                try {
                    Long.parseLong(eventIdStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid event ID in subscription destination"
                    );
                }

            } else if ("/topic/admin/updates".equals(destination)) {
                if (!(accessor.getUser() instanceof JwtAuthenticationToken jwtAuth)) {
                    throw new AccessDeniedException(
                            "Authentication required for admin WebSocket topic"
                    );
                }

                List<String> roles = jwtAuth.getToken()
                        .getClaimAsStringList("https://bookfair-app/roles");

                boolean isOrganizer =
                        roles != null &&
                                (roles.contains("EXHIBITION_ORGANIZER") ||
                                        roles.contains("ROLE_EXHIBITION_ORGANIZER"));

                if (!isOrganizer) {
                    throw new AccessDeniedException(
                            "Access denied to admin WebSocket topic"
                    );
                }
            } else {
                // Reject subscription to any other destination
                throw new AccessDeniedException(
                        "Subscription to this destination is not allowed"
                );
            }
        }

        return message;
    }
}