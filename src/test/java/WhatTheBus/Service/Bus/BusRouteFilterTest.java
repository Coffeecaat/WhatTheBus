package WhatTheBus.Service.Bus;

import WhatTheBus.DTO.Bus.GbusResponse;
import WhatTheBus.DTO.Bus.ReceiveBusData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusRouteFilterTest {

    @Test
    void testFilter24And720_3() {
        // given
        GbusResponse.BusInfo info1 = new GbusResponse.BusInfo();
        info1.setRouteName("24");

        GbusResponse.BusInfo info2 = new GbusResponse.BusInfo();
        info2.setRouteName("720-3");

        GbusResponse.BusInfo info3 = new GbusResponse.BusInfo();
        info3.setRouteName("500");

        List<GbusResponse.BusInfo> items = List.of(info1, info2, info3);
        List<String> targetRoutes = List.of("24", "720-3");

        // when
        List<ReceiveBusData> result = items.stream()
                .filter(item -> targetRoutes.contains(item.getRouteName()))
                .map(item -> {
                    ReceiveBusData dto = new ReceiveBusData();
                    dto.setRouteName(item.getRouteName());
                    return dto;
                })
                .toList();

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ReceiveBusData::getRouteName)
                .containsExactlyInAnyOrder("24", "720-3");
    }
}
