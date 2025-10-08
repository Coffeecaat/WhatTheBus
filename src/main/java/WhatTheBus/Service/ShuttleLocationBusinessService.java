package WhatTheBus.Service;

import WhatTheBus.DTO.ReceiveLocData;
import WhatTheBus.DTO.SendLocData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShuttleLocationBusinessService {

    private final ShuttleLocationService shuttleLocationService;

    public ShuttleLocationBusinessService(ShuttleLocationService shuttleLocationService) {
        this.shuttleLocationService = shuttleLocationService;
    }

    public ReceiveLocData getShuttleLocation(String shuttleId) {
        // Business logic validation
        if (shuttleId == null || shuttleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Shuttle ID cannot be null or empty");
        }

        if (!isValidShuttleId(shuttleId)) {
            throw new IllegalArgumentException("Invalid shuttle ID format: " + shuttleId);
        }

        // Delegate to data access service
        ReceiveLocData location = shuttleLocationService.getLatestLocation(shuttleId);

        if (location == null) {
            throw new RuntimeException("Shuttle not found or no location data available: " + shuttleId);
        }

        return location;
    }

    public void processLocationUpdate(ReceiveLocData locationData) {
        // Business validation
        if (locationData == null) {
            throw new IllegalArgumentException("Location data cannot be null");
        }

        if (!isValidLocationData(locationData)) {
            throw new IllegalArgumentException("Invalid location data");
        }

        // Delegate to data service
        shuttleLocationService.processLocationUpdate(locationData);
    }

    private boolean isValidShuttleId(String shuttleId) {
        // DKU shuttle ID format validation (e.g., SHUTTLE_01, SHUTTLE_02)
        return shuttleId.matches("^SHUTTLE_\\d{2}$");
    }

    private boolean isValidLocationData(ReceiveLocData locationData) {
        return locationData.getShuttleId() != null &&
               !locationData.getShuttleId().trim().isEmpty() &&
               isValidCoordinates(locationData.getLatitude(), locationData.getLongitude()) &&
               locationData.getTimestamp() > 0;
    }

    private boolean isValidCoordinates(double latitude, double longitude) {
        // DKU campus coordinates validation (rough bounds for Korea)
        return latitude >= 35.0 && latitude <= 38.0 &&
               longitude >= 126.0 && longitude <= 129.0;
    }

    public SendLocData getShuttleLocationForClient(String shuttleId) {
        // Business logic validation
        if (shuttleId == null || shuttleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Shuttle ID cannot be null or empty");
        }

        if (!isValidShuttleId(shuttleId)) {
            throw new IllegalArgumentException("Invalid shuttle ID format: " + shuttleId);
        }

        // Get raw data from service
        ReceiveLocData rawData = shuttleLocationService.getLatestLocation(shuttleId);

        if (rawData == null) {
            throw new RuntimeException("Shuttle not found or no location data available: " + shuttleId);
        }

        // Convert to client format
        return convertToSendLocData(rawData);
    }

    public List<SendLocData> getAllActiveShuttleLocations() {
        List<ReceiveLocData> allRawData = shuttleLocationService.getAllActiveLocations();

        return allRawData.stream()
                .map(this::convertToSendLocData)
                .collect(Collectors.toList());
    }

    private SendLocData convertToSendLocData(ReceiveLocData receiveData) {
        SendLocData sendData = new SendLocData();
        sendData.setShuttleId(receiveData.getShuttleId());
        sendData.setLat(receiveData.getLatitude());
        sendData.setLng(receiveData.getLongitude());

        // Placeholder direction logic - will be implemented later with movement tracking
        sendData.setDirection(true); // TODO: Implement based on previous vs current location

        return sendData;
    }
}