package WhatTheBus.DTO.Bus;

import WhatTheBus.bus.CampusStopCode;
import lombok.Data;

import java.util.List;

@Data
public class BusStopData {

    private CampusStopCode stopCode;
    private String stopName;
    private List<ReceiveBusData> buses;


}
