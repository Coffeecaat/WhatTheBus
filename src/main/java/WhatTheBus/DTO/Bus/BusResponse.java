package WhatTheBus.DTO.Bus;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BusResponse {

    private LocalDateTime updatedAt;
    private List<BusStopData> stops;
}

