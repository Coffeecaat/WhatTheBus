package WhatTheBus.DTO;

import lombok.Data;

@Data
public class ReceiveLocData {
    private String shuttleId;
    private double latitude;
    private double longitude;
    private long timestamp;

}
