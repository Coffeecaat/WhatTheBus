package WhatTheBus.Controller;

import WhatTheBus.Config.SecurityConfigDev;
import WhatTheBus.DTO.Shuttle.ReceiveLocData;
import WhatTheBus.Service.Shuttle.ShuttleLocationBusinessService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(RaspberryPiController.class)
@ActiveProfiles("test")
@Import(SecurityConfigDev.class)
class RaspberryPiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShuttleLocationBusinessService businessService;

    private ReceiveLocData buildPayload() {
        ReceiveLocData payload = new ReceiveLocData();
        payload.setShuttleId("SHUTTLE_01");
        payload.setLatitude(37.3219);
        payload.setLongitude(127.1280);
        payload.setTimestamp(System.currentTimeMillis());
        return payload;
    }

    @Nested
    @DisplayName("POST /api/pi/location")
    class UpdateLocation {

        @Test
        @DisplayName("정상 요청은 200 OK")
        void success() throws Exception {
            ReceiveLocData payload = buildPayload();
            doNothing().when(businessService).processLocationUpdate(any(ReceiveLocData.class));

            mockMvc.perform(post("/api/pi/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            verify(businessService).processLocationUpdate(any(ReceiveLocData.class));
        }

        @Test
        @DisplayName("비즈니스 로직이 IllegalArgumentException 을 던지면 400")
        void invalidPayload() throws Exception {
            ReceiveLocData payload = buildPayload();
            doThrow(new IllegalArgumentException("Invalid data"))
                    .when(businessService).processLocationUpdate(any(ReceiveLocData.class));

            mockMvc.perform(post("/api/pi/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("RuntimeException 이 발생하면 500")
        void unexpectedFailure() throws Exception {
            ReceiveLocData payload = buildPayload();
            doThrow(new RuntimeException("Redis down"))
                    .when(businessService).processLocationUpdate(any(ReceiveLocData.class));

            mockMvc.perform(post("/api/pi/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("잘못된 JSON 본문은 400")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/api/pi/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());

            verify(businessService, never()).processLocationUpdate(any(ReceiveLocData.class));
        }
    }

    @Test
    @DisplayName("GET /api/pi/health 는 인증 없이 200 OK")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/pi/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pi connection OK"));
    }
}
