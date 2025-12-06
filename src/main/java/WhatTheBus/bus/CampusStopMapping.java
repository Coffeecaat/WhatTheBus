package WhatTheBus.bus;

import java.util.Map;

public final class CampusStopMapping {

    private CampusStopMapping() {}

    public static final Map<CampusStopCode, String> STATION_ID_MAP = Map.of(
            CampusStopCode.JUKJEON_STATION, "228001028",
            CampusStopCode.DENTAL_HOSPITAL, "228001737",
            CampusStopCode.HUMANITIES_BUILDING, "228001981",
            CampusStopCode.MAIN_GATE, "228001978"
    );

    public static final Map<CampusStopCode, String> STATION_NAME_MAP = Map.of(

            CampusStopCode.JUKJEON_STATION, "죽전역",
            CampusStopCode.DENTAL_HOSPITAL, "치과병원",
            CampusStopCode.HUMANITIES_BUILDING, "인문관",
            CampusStopCode.MAIN_GATE, "정문"
    );
}


