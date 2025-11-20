package WhatTheBus;

import WhatTheBus.DTO.Shuttle.ReceiveLocData;
import WhatTheBus.Service.RateLimitService;
import WhatTheBus.Service.Shuttle.ShuttleLocationBusinessService;
import WhatTheBus.Service.DtlsServerService;
import WhatTheBus.Security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WhatTheBusApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DtlsServerService dtlsServerService;

    @MockitoBean
    private ShuttleLocationBusinessService businessService;

    @MockitoBean
    private RateLimitService rateLimitService;

    private String jwtToken;
    private String bearerToken;
    private String hashedClientId;

    @BeforeEach
    void setUp() {
        jwtToken = jwtService.generateToken("WhatTheBus", "1.0.0", "Android");
        bearerToken = "Bearer " + jwtToken;
        hashedClientId = jwtService.extractHashedClientId(jwtToken);
        when(rateLimitService.isAllowed(hashedClientId)).thenReturn(true);
    }

    @Test
    @DisplayName("애플리케이션 기동 시 DTLS 커넥터가 실행된다")
    void contextLoads_andDtlsConnectorRunning() {
        assertThat(dtlsServerService).isNotNull();
        assertThat(dtlsServerService.isRunning()).isTrue();
    }

    @Nested
    @DisplayName("라즈베리파이 API")
    class RaspberryPiApi {

        @Test
        @DisplayName("/api/pi/health 는 200 OK")
        void healthCheck() throws Exception {
            mockMvc.perform(get("/api/pi/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Pi connection OK"));
        }

        @Test
        @DisplayName("정상 위치 업데이트 요청은 200 OK\"")
        void updateLocation_ok() throws Exception {
            ReceiveLocData payload = new ReceiveLocData();
            payload.setShuttleId("SHUTTLE_01");
            payload.setLatitude(37.3219);
            payload.setLongitude(127.1280);
            payload.setTimestamp(System.currentTimeMillis());

            doNothing().when(businessService).processLocationUpdate(any(ReceiveLocData.class));

            mockMvc.perform(post("/api/pi/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            verify(businessService).processLocationUpdate(any(ReceiveLocData.class));
        }

        @Test
        @DisplayName("잘못된 JSON은 400 Bad Request")
        void updateLocation_invalidJson() throws Exception {
            mockMvc.perform(post("/api/pi/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());

            verify(businessService, never()).processLocationUpdate(any(ReceiveLocData.class));
        }
    }

    @Nested
    @DisplayName("클라이언트 셔틀 조회 API")
    class ShuttleApi {

        @Test
        @DisplayName("인증 헤더가 없으면 401")
        void getLocation_requiresAuthHeader() throws Exception {
            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("레이트 리미트 초과 시 429")
        void getLocation_rateLimited() throws Exception {
            when(rateLimitService.isAllowed(hashedClientId)).thenReturn(false);

            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location")
                            .header("Authorization", bearerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("서비스가 RuntimeException을 던지면 404")
        void getLocation_notFound() throws Exception {
            when(businessService.getShuttleLocationForClient("SHUTTLE_01")).thenThrow(new RuntimeException("Not found"));

            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location")
                            .header("Authorization", bearerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}
