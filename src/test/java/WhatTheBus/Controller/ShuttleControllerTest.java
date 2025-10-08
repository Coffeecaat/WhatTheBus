package WhatTheBus.Controller;

import WhatTheBus.Config.SecurityConfigDev;
import WhatTheBus.Config.SecurityConfigProd;
import WhatTheBus.DTO.SendLocData;
import WhatTheBus.Security.JwtService;
import WhatTheBus.Service.RateLimitService;
import WhatTheBus.Service.ShuttleLocationBusinessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShuttleController.class)
@ActiveProfiles("test")
@Import(SecurityConfigDev.class)
class ShuttleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShuttleLocationBusinessService businessService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private JwtService jwtService;

    private SendLocData sendLocData;
    private String rawToken;
    private String bearerToken;

    @BeforeEach
    void setUp() {
        sendLocData = new SendLocData();
        sendLocData.setShuttleId("SHUTTLE_01");
        sendLocData.setLat(37.3219);
        sendLocData.setLng(127.1280);
        sendLocData.setDirection(true);

        rawToken = "valid.jwt.token";
        bearerToken = "Bearer " + rawToken;

        when(jwtService.isTokenValid(rawToken)).thenReturn(true);
        when(jwtService.extractHashedClientId(rawToken)).thenReturn("hashed-client");
        when(rateLimitService.isAllowed("hashed-client")).thenReturn(true);
    }

    @Nested
    @DisplayName("GET /api/shuttle/{id}/location")
    class GetLocation {

        @Test
        @DisplayName("정상 요청은 200 OK")
        void success() throws Exception {
            when(businessService.getShuttleLocationForClient("SHUTTLE_01")).thenReturn(sendLocData);

            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location")
                            .header("Authorization", bearerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.shuttleId").value("SHUTTLE_01"));
        }

        @Test
        @DisplayName("Authorization 헤더가 없으면 401")
        void noAuthorizationHeader() throws Exception {
            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("잘못된 Authorization 포맷은 401")
        void badAuthorizationHeader() throws Exception {
            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location")
                            .header("Authorization", "Invalid header"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("토큰 검증 실패 시 401")
        void invalidToken() throws Exception {
            when(jwtService.isTokenValid(rawToken)).thenReturn(false);

            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location")
                            .header("Authorization", bearerToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("레이트 리미트 초과 시 429")
        void rateLimited() throws Exception {
            when(rateLimitService.isAllowed("hashed-client")).thenReturn(false);

            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location")
                            .header("Authorization", bearerToken))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("IllegalArgumentException 은 400")
        void illegalArgument() throws Exception {
            when(businessService.getShuttleLocationForClient("SHUTTLE_01")).thenThrow(new IllegalArgumentException("bad shuttle"));

            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location")
                            .header("Authorization", bearerToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("RuntimeException 은 404")
        void runtimeException() throws Exception {
            when(businessService.getShuttleLocationForClient("SHUTTLE_01")).thenThrow(new RuntimeException("missing"));

            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location")
                            .header("Authorization", bearerToken))
                    .andExpect(status().isNotFound());
        }
    }
}
