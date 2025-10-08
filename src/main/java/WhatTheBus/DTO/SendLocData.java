package WhatTheBus.DTO;

import lombok.Data;

@Data
public class SendLocData {

    private String shuttleId;
    private double lat;
    private double lng;
    private boolean direction;
}
