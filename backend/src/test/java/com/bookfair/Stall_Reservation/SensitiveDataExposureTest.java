package com.bookfair.Stall_Reservation;

import com.bookfair.Stall_Reservation.service.EventService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "OIDC_ISSUER=https://mock-issuer.local"
})
@AutoConfigureMockMvc
public class SensitiveDataExposureTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private EventService eventService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGenericExceptionHandler() throws Exception {
        // Mock getById to throw an unexpected RuntimeException (e.g. database disconnect, null pointer, etc.)
        Mockito.when(eventService.getById(1L))
                .thenThrow(new RuntimeException("Database connection timeout or internal error description"));

        // Expect 500 with generic message and no leaked stack traces
        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    public void testSpecificExceptionHandlerIsPrepreserved() throws Exception {
        // Mock getById to throw specific IllegalArgumentException
        Mockito.when(eventService.getById(2L))
                .thenThrow(new IllegalArgumentException("Invalid event reference"));

        // Expect 400 Bad Request with the custom message (not swallowed by generic handler)
        mockMvc.perform(get("/api/events/2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid event reference"));
    }
}
