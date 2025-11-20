package WhatTheBus.Controller;

import WhatTheBus.Config.SecurityConfigDev;
import WhatTheBus.DTO.Shuttle.SendLocData;
import WhatTheBus.Service.Shuttle.ShuttleLocationBusinessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShuttleController.class)
@ActiveProfiles("test")
@Import(SecurityConfigDev.class)
class ShuttleControllerOpenAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShuttleLocationBusinessService businessService;

    @Nested
    @DisplayName("GET /api/shuttle/{id}/location - 공개 접근")
    class GetSingleShuttleLocation {

        @Test
        @DisplayName("정상 셔틀 요청은 200과 위치 정보를 반환한다")
        void returnsLocation() throws Exception {
            SendLocData data = new SendLocData();
            data.setShuttleId("SHUTTLE_01");
            data.setLat(37.32);
            data.setLng(127.12);
            data.setDirection(true);

            when(businessService.getShuttleLocationForClient("SHUTTLE_01")).thenReturn(data);

            mockMvc.perform(get("/api/shuttle/SHUTTLE_01/location")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shuttleId").value("SHUTTLE_01"))
                    .andExpect(jsonPath("$.lat").value(37.32));
        }

        @Test
        @DisplayName("잘못된 셔틀 ID는 400을 반환한다")
        void invalidId() throws Exception {
            when(businessService.getShuttleLocationForClient("BAD"))
                    .thenThrow(new IllegalArgumentException("Invalid shuttle ID"));

            mockMvc.perform(get("/api/shuttle/BAD/location"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("데이터가 없으면 404를 반환한다")
        void notFound() throws Exception {
            when(businessService.getShuttleLocationForClient("SHUTTLE_09"))
                    .thenThrow(new RuntimeException("missing"));

            mockMvc.perform(get("/api/shuttle/SHUTTLE_09/location"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/shuttle/locations - 공개 접근")
    class GetAllShuttleLocations {

        @Test
        @DisplayName("활성 셔틀이 있으면 200과 목록을 반환한다")
        void returnsList() throws Exception {
            SendLocData first = new SendLocData();
            first.setShuttleId("SHUTTLE_01");
            first.setLat(37.32);
            first.setLng(127.12);
            first.setDirection(true);

            SendLocData second = new SendLocData();
            second.setShuttleId("SHUTTLE_02");
            second.setLat(37.33);
            second.setLng(127.13);
            second.setDirection(false);

            when(businessService.getAllActiveShuttleLocations()).thenReturn(List.of(first, second));

            mockMvc.perform(get("/api/shuttle/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].shuttleId").value("SHUTTLE_01"))
                    .andExpect(jsonPath("$[1].shuttleId").value("SHUTTLE_02"));
        }

        @Test
        @DisplayName("활성 셔틀이 없으면 204와 안내 헤더를 반환한다")
        void returnsNoContent() throws Exception {
            when(businessService.getAllActiveShuttleLocations()).thenReturn(List.of());

            mockMvc.perform(get("/api/shuttle/locations"))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string("X-Result-Message", "현재 운행 중인 셔틀이 없습니다."));
        }

        @Test
        @DisplayName("잘못된 요청이면 400과 에러 메시지를 반환한다")
        void invalidRequest() throws Exception {
            when(businessService.getAllActiveShuttleLocations())
                    .thenThrow(new IllegalArgumentException("invalid"));

            mockMvc.perform(get("/api/shuttle/locations"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("예상치 못한 오류는 500과 일반 에러 메시지를 반환한다")
        void internalError() throws Exception {
            when(businessService.getAllActiveShuttleLocations())
                    .thenThrow(new RuntimeException("boom"));

            mockMvc.perform(get("/api/shuttle/locations"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        }
    }
}
