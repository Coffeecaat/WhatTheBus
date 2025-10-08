package WhatTheBus.Entity;

import WhatTheBus.DTO.ReceiveLocData;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class LocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shuttleId;

    private double latitude;
    private double longitude;
    private long timestamp;

    public static LocationHistory from(ReceiveLocData locationData){
        LocationHistory history = new LocationHistory();
        history.setShuttleId(locationData.getShuttleId());
        history.setLatitude(locationData.getLatitude());
        history.setLongitude(locationData.getLongitude());
        history.setTimestamp(locationData.getTimestamp());
        return history;
    }
}
