package WhatTheBus.Service.Bus;

import WhatTheBus.Bus.CampusStopCode;
import WhatTheBus.Bus.CampusStopMapping;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CampusStopMappingTest {

    @Test
    void testStationIdMapping() {

        String jukjeonStationId = CampusStopMapping.STATION_ID_MAP.get(CampusStopCode.JUKJEON_STATION);
        String dentalStationId = CampusStopMapping.STATION_ID_MAP.get(CampusStopCode.DENTAL_HOSPITAL);

        assertThat(jukjeonStationId).isNotBlank();
        assertThat(dentalStationId).isNotBlank();

    }

    @Test
    void testStationNameMapping() {
        String jukjeonName = CampusStopMapping.STATION_NAME_MAP.get(CampusStopCode.JUKJEON_STATION);
        String mainGateName = CampusStopMapping.STATION_NAME_MAP.get(CampusStopCode.MAIN_GATE);

        assertThat(jukjeonName).isEqualTo("죽전역");
        assertThat(mainGateName).isEqualTo("단국대 정문");
    }
}