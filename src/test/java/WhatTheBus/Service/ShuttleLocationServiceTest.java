package WhatTheBus.Service;

import WhatTheBus.DTO.Shuttle.ReceiveLocData;
import WhatTheBus.Entity.LocationHistory;
import WhatTheBus.Repository.LocationHistoryRepository;
import WhatTheBus.Service.Shuttle.ShuttleLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShuttleLocationService")
class ShuttleLocationServiceTest {

    @Mock
    private LocationHistoryRepository locationHistoryRepository;

    @Mock
    private RedisTemplate<String, ReceiveLocData> redisTemplate;

    @Mock
    private ValueOperations<String, ReceiveLocData> valueOperations;

    @InjectMocks
    private ShuttleLocationService shuttleLocationService;

    private ReceiveLocData receiveLocData;
    private LocationHistory locationHistory;

    @BeforeEach
    void setUp() {
        receiveLocData = new ReceiveLocData();
        receiveLocData.setShuttleId("SHUTTLE_01");
        receiveLocData.setLatitude(37.3219);
        receiveLocData.setLongitude(127.1280);
        receiveLocData.setTimestamp(System.currentTimeMillis());

        locationHistory = LocationHistory.from(receiveLocData);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(shuttleLocationService, "shuttleLocationKey", "shuttle:location:");
    }

    @Nested
    @DisplayName("processLocationUpdate")
    class ProcessLocationUpdate {

        @Test
        @DisplayName("Redis와 JPA에 위치 데이터 저장")
        void savesToRedisAndDatabase() {
            when(locationHistoryRepository.save(any(LocationHistory.class))).thenReturn(locationHistory);

            shuttleLocationService.processLocationUpdate(receiveLocData);

            verify(valueOperations).set("shuttle:location:SHUTTLE_01", receiveLocData);
            verify(locationHistoryRepository).save(any(LocationHistory.class));
        }
    }

    @Nested
    @DisplayName("getLatestLocation")
    class GetLatestLocation {

        @Test
        @DisplayName("Redis에 데이터가 있으면 반환")
        void returnsPresentValue() {
            when(valueOperations.get("shuttle:location:SHUTTLE_01")).thenReturn(receiveLocData);

            ReceiveLocData result = shuttleLocationService.getLatestLocation("SHUTTLE_01");

            assertThat(result).isNotNull();
            assertThat(result.getShuttleId()).isEqualTo("SHUTTLE_01");
        }

        @Test
        @DisplayName("Redis에 없으면 null")
        void returnsNullWhenMissing() {
            when(valueOperations.get("shuttle:location:SHUTTLE_01")).thenReturn(null);

            ReceiveLocData result = shuttleLocationService.getLatestLocation("SHUTTLE_01");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getAllActiveLocations")
    class GetAllActiveLocations {

        @Test
        @DisplayName("키가 없으면 빈 목록")
        void emptyWhenNoKeys() {
            when(redisTemplate.keys("shuttle:location:*" )).thenReturn(Set.of());

            List<ReceiveLocData> result = shuttleLocationService.getAllActiveLocations();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("여러 키가 있으면 모두 반환")
        void returnsAll() {
            ReceiveLocData second = new ReceiveLocData();
            second.setShuttleId("SHUTTLE_02");
            second.setLatitude(37.3220);
            second.setLongitude(127.1281);
            second.setTimestamp(System.currentTimeMillis());

            when(redisTemplate.keys("shuttle:location:*"))
                    .thenReturn(Set.of("shuttle:location:SHUTTLE_01", "shuttle:location:SHUTTLE_02"));
            when(valueOperations.get("shuttle:location:SHUTTLE_01")).thenReturn(receiveLocData);
            when(valueOperations.get("shuttle:location:SHUTTLE_02")).thenReturn(second);

            List<ReceiveLocData> result = shuttleLocationService.getAllActiveLocations();

            assertThat(result).containsExactlyInAnyOrder(receiveLocData, second);
        }

        @Test
        @DisplayName("null 값은 건너뛰고 반환")
        void ignoresNullValues() {
            when(redisTemplate.keys("shuttle:location:*"))
                    .thenReturn(Set.of("shuttle:location:SHUTTLE_01", "shuttle:location:SHUTTLE_02"));
            when(valueOperations.get("shuttle:location:SHUTTLE_01")).thenReturn(receiveLocData);
            when(valueOperations.get("shuttle:location:SHUTTLE_02")).thenReturn(null);

            List<ReceiveLocData> result = shuttleLocationService.getAllActiveLocations();

            assertThat(result).containsExactly(receiveLocData);
        }
    }
}