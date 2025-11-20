package WhatTheBus.DTO.Bus;

import lombok.Data;

@Data
public class ReceiveBusData {

    private String routeName;
    private Integer minutesLeft;
    private Integer remainingSeats;


}
