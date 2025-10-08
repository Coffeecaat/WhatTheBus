package WhatTheBus.Entity;

import WhatTheBus.DTO.ReceiveLocData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationHistoryTest {

    private ReceiveLocData testReceiveData;

    @BeforeEach
    void setUp() {
        testReceiveData = new ReceiveLocData();
        testReceiveData.setShuttleId("SHUTTLE_01");
        testReceiveData.setLatitude(37.3219);
        testReceiveData.setLongitude(127.1280);
        testReceiveData.setTimestamp(1640995200000L); // 2022-01-01 00:00:00 UTC
    }

    @Test
    void from_shouldCreateLocationHistory_whenValidReceiveLocData() {
        LocationHistory history = LocationHistory.from(testReceiveData);

        assertNotNull(history);
        assertEquals("SHUTTLE_01", history.getShuttleId());
        assertEquals(37.3219, history.getLatitude());
        assertEquals(127.1280, history.getLongitude());
        assertEquals(1640995200000L, history.getTimestamp());
        assertNull(history.getId()); // ID should be null before persistence
    }

    @Test
    void from_shouldHandleNullInput() {
        assertThrows(NullPointerException.class, () -> {
            LocationHistory.from(null);
        });
    }

    @Test
    void from_shouldHandleNullShuttleId() {
        testReceiveData.setShuttleId(null);

        LocationHistory history = LocationHistory.from(testReceiveData);

        assertNull(history.getShuttleId());
        assertEquals(37.3219, history.getLatitude());
        assertEquals(127.1280, history.getLongitude());
        assertEquals(1640995200000L, history.getTimestamp());
    }

    @Test
    void from_shouldHandleZeroCoordinates() {
        testReceiveData.setLatitude(0.0);
        testReceiveData.setLongitude(0.0);

        LocationHistory history = LocationHistory.from(testReceiveData);

        assertEquals("SHUTTLE_01", history.getShuttleId());
        assertEquals(0.0, history.getLatitude());
        assertEquals(0.0, history.getLongitude());
        assertEquals(1640995200000L, history.getTimestamp());
    }

    @Test
    void from_shouldHandleNegativeCoordinates() {
        testReceiveData.setLatitude(-37.3219);
        testReceiveData.setLongitude(-127.1280);

        LocationHistory history = LocationHistory.from(testReceiveData);

        assertEquals("SHUTTLE_01", history.getShuttleId());
        assertEquals(-37.3219, history.getLatitude());
        assertEquals(-127.1280, history.getLongitude());
        assertEquals(1640995200000L, history.getTimestamp());
    }

    @Test
    void from_shouldHandleZeroTimestamp() {
        testReceiveData.setTimestamp(0L);

        LocationHistory history = LocationHistory.from(testReceiveData);

        assertEquals("SHUTTLE_01", history.getShuttleId());
        assertEquals(37.3219, history.getLatitude());
        assertEquals(127.1280, history.getLongitude());
        assertEquals(0L, history.getTimestamp());
    }

    @Test
    void from_shouldHandleNegativeTimestamp() {
        testReceiveData.setTimestamp(-1000L);

        LocationHistory history = LocationHistory.from(testReceiveData);

        assertEquals("SHUTTLE_01", history.getShuttleId());
        assertEquals(37.3219, history.getLatitude());
        assertEquals(127.1280, history.getLongitude());
        assertEquals(-1000L, history.getTimestamp());
    }

    @Test
    void from_shouldHandleExtremeCoordinates() {
        testReceiveData.setLatitude(90.0);  // North pole
        testReceiveData.setLongitude(180.0); // Date line

        LocationHistory history = LocationHistory.from(testReceiveData);

        assertEquals("SHUTTLE_01", history.getShuttleId());
        assertEquals(90.0, history.getLatitude());
        assertEquals(180.0, history.getLongitude());
        assertEquals(1640995200000L, history.getTimestamp());
    }

    @Test
    void from_shouldCreateMultipleInstances() {
        LocationHistory history1 = LocationHistory.from(testReceiveData);
        LocationHistory history2 = LocationHistory.from(testReceiveData);

        assertNotSame(history1, history2);
        assertEquals(history1.getShuttleId(), history2.getShuttleId());
        assertEquals(history1.getLatitude(), history2.getLatitude());
        assertEquals(history1.getLongitude(), history2.getLongitude());
        assertEquals(history1.getTimestamp(), history2.getTimestamp());
    }

    @Test
    void noArgsConstructor_shouldCreateEmptyInstance() {
        LocationHistory history = new LocationHistory();

        assertNull(history.getId());
        assertNull(history.getShuttleId());
        assertEquals(0.0, history.getLatitude());
        assertEquals(0.0, history.getLongitude());
        assertEquals(0L, history.getTimestamp());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        LocationHistory history = new LocationHistory();

        history.setId(1L);
        history.setShuttleId("TEST_SHUTTLE");
        history.setLatitude(40.7128);
        history.setLongitude(-74.0060);
        history.setTimestamp(1640995200000L);

        assertEquals(1L, history.getId());
        assertEquals("TEST_SHUTTLE", history.getShuttleId());
        assertEquals(40.7128, history.getLatitude());
        assertEquals(-74.0060, history.getLongitude());
        assertEquals(1640995200000L, history.getTimestamp());
    }
}