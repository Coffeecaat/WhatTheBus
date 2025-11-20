package WhatTheBus.Service.Bus;

import WhatTheBus.DTO.Bus.GbusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GbusResponseNullFieldTest {

    @Test
    void testNullFieldsAreHandled() throws Exception {
        String json = """
                {
                  "response": {
                    "msgBody": {
                      "busArrivalList": [
                        {
                          "routeName": "24",
                          "stationId": "228000331",
                          "predictTime1": null,
                          "remainSeatCnt1": null
                        }
                      ]
                    }
                  }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();

        GbusResponse res = mapper.readValue(json, GbusResponse.class);

        GbusResponse.BusInfo info = res.getResponse()
                .getMsgBody()
                .getBusArrivalList()
                .get(0);

        assertThat(info.getRouteName()).isEqualTo("24");
        assertThat(info.getPredictTime1()).isNull();
        assertThat(info.getRemainSeatCnt1()).isNull();
    }
}