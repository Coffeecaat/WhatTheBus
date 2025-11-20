package WhatTheBus.Service.Bus;

import WhatTheBus.DTO.Bus.BusResponse;
import WhatTheBus.DTO.Bus.BusStopData;
import WhatTheBus.DTO.Bus.ReceiveBusData;
import WhatTheBus.Bus.CampusStopCode;
import WhatTheBus.Bus.GbusApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BusArrivalServiceEdgeTest {

    private GbusApiClient gbusApiClient;
    private RedisTemplate<String, BusStopData> redisTemplate;
    private ValueOperations<String, BusStopData> valueOps;
    private BusArrivalService service;

    @BeforeEach
    void setUp() {
        gbusApiClient = mock(GbusApiClient.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service = new BusArrivalService(gbusApiClient, redisTemplate);
        ReflectionTestUtils.setField(service, "busKeyPrefix", "bus:arrivals:");
    }

    @Test
    void testGetArrivalsWithSomeStopsMissing() {
        // given: 첫 번째 정류장은 데이터 있음
        BusStopData jukjeon = new BusStopData();
        jukjeon.setStopCode(CampusStopCode.JUKJEON_STATION);
        jukjeon.setStopName("죽전역");
        jukjeon.setBuses(List.of(new ReceiveBusData()));

        // 두 번째 정류장: null 반환 (Redis에 아직 없음)
        when(redisTemplate.opsForValue().get(anyString()))
                .thenReturn(jukjeon)  // 첫 번째 호출
                .thenReturn(null)     // 두 번째 호출
                .thenReturn(null)     // 세 번째 호출
                .thenReturn(null);    // 네 번째 호출 (정류장 개수만큼)

        // when
        BusResponse response = service.getArrivals();

        // then
        assertThat(response.getStops()).isNotEmpty();
        assertThat(response.getStops()).hasSize(1);
        assertThat(response.getStops().get(0).getStopName()).isEqualTo("죽전역");
    }
}
