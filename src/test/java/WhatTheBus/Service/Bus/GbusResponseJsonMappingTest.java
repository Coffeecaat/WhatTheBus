package WhatTheBus.Service.Bus;

import WhatTheBus.DTO.Bus.GbusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GbusResponseJsonMappingTest {

    @Test
    void testJsonMappingToGbusResponse() throws Exception {
        // given: 실제 응답 형태를 흉내 낸 JSON
        String json = """
                {
                  "response": {
                    "msgBody": {
                      "busArrivalList": [
                        {
                          "routeName": "24",
                          "stationId": "228000331",
                          "predictTime1": 5,
                          "remainSeatCnt1": 10
                        },
                        {
                          "routeName": "720-3",
                          "stationId": "228000331",
                          "predictTime1": 7,
                          "remainSeatCnt1": 3
                        }
                      ]
                    }
                  }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();

        // when
        GbusResponse res = mapper.readValue(json, GbusResponse.class);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getResponse()).isNotNull();
        assertThat(res.getResponse().getMsgBody()).isNotNull();

        List<GbusResponse.BusInfo> list = res.getResponse().getMsgBody().getBusArrivalList();
        assertThat(list).hasSize(2);

        GbusResponse.BusInfo first = list.get(0);
        assertThat(first.getRouteName()).isEqualTo("24");
        assertThat(first.getStationId()).isEqualTo("228000331");
        assertThat(first.getPredictTime1()).isEqualTo(5);
        assertThat(first.getRemainSeatCnt1()).isEqualTo(10);
    }
}