package WhatTheBus.Service.Bus;

import WhatTheBus.DTO.Bus.BusResponse;
import WhatTheBus.DTO.Bus.BusStopData;
import WhatTheBus.DTO.Bus.GbusResponse;
import WhatTheBus.DTO.Bus.ReceiveBusData;
import WhatTheBus.Bus.CampusStopCode;
import WhatTheBus.Bus.GbusApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BusArrivalServiceTest {

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
    void testStopsRefresh() {

        // 1) Mock GBUS API 응답 구성
        GbusResponse.BusInfo info = new GbusResponse.BusInfo();
        info.setRouteName("24");
        info.setPredictTime1(3);
        info.setRemainSeatCnt1(12);

        GbusResponse.MsgBody msgBody = new GbusResponse.MsgBody();
        msgBody.setBusArrivalList(List.of(info));

        GbusResponse.ResponseBody responseBody = new GbusResponse.ResponseBody();
        responseBody.setMsgBody(msgBody);

        GbusResponse gbusResponse = new GbusResponse();
        gbusResponse.setResponse(responseBody);

        // 모든 정류장 호출시 같은 응답 리턴하도록 설정
        when(gbusApiClient.getStationId(any())).thenReturn(gbusResponse);

        // when
        service.stopsRefresh();

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BusStopData> valueCaptor = ArgumentCaptor.forClass(BusStopData.class);

        // Redis 저장 호출 검증
        verify(valueOps, atLeastOnce()).set(keyCaptor.capture(), valueCaptor.capture());

        List<String> savedKeys = keyCaptor.getAllValues();
        List<BusStopData> savedValues = valueCaptor.getAllValues();

        // 저장된 key에 정류장 코드 이름이 포함되어야 함
        assertThat(savedKeys).anyMatch(k -> k.contains("JUKJEON_STATION"));

        // BusStopData 내부 데이터 검증
        BusStopData first = savedValues.get(0);
        assertThat(first.getBuses()).isNotEmpty();
        assertThat(first.getBuses().get(0).getRouteName()).isEqualTo("24");
    }

    @Test
    void testGetArrivals() {
        // given
        BusStopData data = new BusStopData();
        data.setStopCode(CampusStopCode.JUKJEON_STATION);
        data.setStopName("죽전역");
        data.setBuses(List.of(new ReceiveBusData()));

        when(redisTemplate.opsForValue().get(any())).thenReturn(data);

        // when
        BusResponse result = service.getArrivals();

        // then
        assertThat(result.getStops()).isNotEmpty();
        assertThat(result.getStops().get(0).getStopName()).isEqualTo("죽전역");
    }
}