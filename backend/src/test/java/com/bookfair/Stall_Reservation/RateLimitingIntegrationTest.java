package com.bookfair.Stall_Reservation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "OIDC_ISSUER=https://mock-issuer.local"
})
@AutoConfigureMockMvc
public class RateLimitingIntegrationTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testBookingCreationRateLimiting() throws Exception {
        String vendor1 = "auth0|vendor1_" + java.util.UUID.randomUUID();
        String vendor2 = "auth0|vendor2_" + java.util.UUID.randomUUID();

        // 1. Vendor 1 makes first 3 booking requests (limited to 3/min)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/reservations/book")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_STALL_VENDOR"))
                                    .jwt(j -> j
                                            .claim("sub", vendor1)
                                            .claim("email", vendor1 + "@test.com")
                                    )
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    // Expect 400 Bad Request due to validation, NOT 429
                    .andExpect(status().isBadRequest());
        }

        // 2. Vendor 1 makes 4th request -> Expect 429
        mockMvc.perform(post("/api/reservations/book")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_STALL_VENDOR"))
                                .jwt(j -> j
                                        .claim("sub", vendor1)
                                        .claim("email", vendor1 + "@test.com")
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));

        // 3. Different Vendor 2 makes a request -> Expect 400 (separate bucket)
        mockMvc.perform(post("/api/reservations/book")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_STALL_VENDOR"))
                                .jwt(j -> j
                                        .claim("sub", vendor2)
                                        .claim("email", vendor2 + "@test.com")
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testBookingCancellationRateLimiting() throws Exception {
        String vendor = "auth0|vendor_cancel_" + java.util.UUID.randomUUID();

        // Cancellation is limited to 5/min
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/reservations/1/cancel")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_STALL_VENDOR"))
                                    .jwt(j -> j
                                            .claim("sub", vendor)
                                            .claim("email", vendor + "@test.com")
                                    )
                            ))
                    // Expect non-429 response (since the entity is missing, 404 is typical, but NOT 429)
                    .andExpect(status().is(org.hamcrest.Matchers.oneOf(400, 401, 403, 404, 409)));
        }

        // 6th request -> Expect 429
        mockMvc.perform(post("/api/reservations/1/cancel")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_STALL_VENDOR"))
                                .jwt(j -> j
                                        .claim("sub", vendor)
                                        .claim("email", vendor + "@test.com")
                                )
                        ))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    public void testProfileUpdateRateLimiting() throws Exception {
        String vendor = "auth0|vendor_profile_" + java.util.UUID.randomUUID();

        // Profile updates limited to 10/min
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(put("/api/profile")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_STALL_VENDOR"))
                                    .jwt(j -> j
                                            .claim("sub", vendor)
                                            .claim("email", vendor + "@test.com")
                                    )
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest()); // validation fail
        }

        // 11th request -> Expect 429
        mockMvc.perform(put("/api/profile")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_STALL_VENDOR"))
                                .jwt(j -> j
                                        .claim("sub", vendor)
                                        .claim("email", vendor + "@test.com")
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    public void testWebSocketHandshakeRateLimiting() throws Exception {
        // Handshake endpoints are limited to 10/min per IP
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/ws/info"))
                    .andExpect(status().isOk());
        }

        // 11th handshake attempt -> Expect 429
        mockMvc.perform(get("/ws/info"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        // SockJS transport traffic (not info or Upgrade) -> should bypass
        mockMvc.perform(get("/ws/123/session/xhr"))
                .andExpect(status().is(org.hamcrest.Matchers.oneOf(404, 405))); // Bypasses rate limiter (404/405 instead of 429)
    }

    @Test
    public void testPublicApiRateLimiting() throws Exception {
        // Public API limited to 60/min per IP
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/public/events/upcoming"))
                    .andExpect(status().isOk());
        }

        // 61st request -> Expect 429
        mockMvc.perform(get("/api/public/events/upcoming"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
